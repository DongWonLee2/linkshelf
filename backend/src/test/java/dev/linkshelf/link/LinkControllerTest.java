package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(LinkController.class)
class LinkControllerTest {

	private static final String VALID_URL = "https://example.com/a";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LinkService linkService;

	@BeforeEach
	void stubService() {
		given(linkService.save(any())).willReturn(new LinkResponse(1L, VALID_URL, "한글 메모 😀",
				List.of("Java", "스프링"), Instant.parse("2026-10-14T03:00:00.123456Z")));
	}

	private ResultActions postLink(String json) throws Exception {
		return mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	private ResultActions postLink(String url, String memo, List<String> tags) throws Exception {
		return postLink("{\"url\":" + quote(url) + ",\"memo\":" + quote(memo) + ",\"tags\":"
				+ (tags == null ? "null" : tags.stream().map(LinkControllerTest::quote)
					.collect(Collectors.joining(",", "[", "]")))
				+ "}");
	}

	private static String quote(String value) {
		return value == null ? "null" : "\"" + value + "\"";
	}

	private static List<String> distinctTags(int count) {
		return IntStream.rangeClosed(1, count).mapToObj(i -> "tag" + i).collect(Collectors.toList());
	}

	private ResultActions expectRejected(ResultActions result, String... fields) throws Exception {
		result.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder(fields)))
			.andExpect(jsonPath("$.errors[*].message", everyItem(not(emptyString()))));
		return result;
	}

	@Test
	@DisplayName("R-LINK-01: valid request returns 201 with the saved link")
	void R_LINK_01_returns_201_with_saved_link() throws Exception {
		postLink("""
				{"url":"HTTPS://Example.COM:443/a","memo":"한글 메모 😀","tags":["Java"," java ","스프링"]}
				""")
			.andExpect(status().isCreated())
			.andExpect(header().doesNotExist("Location"))
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.url").value("https://example.com/a"))
			.andExpect(jsonPath("$.memo").value("한글 메모 😀"))
			.andExpect(jsonPath("$.tags[0]").value("Java"))
			.andExpect(jsonPath("$.tags[1]").value("스프링"))
			.andExpect(jsonPath("$.createdAt").value("2026-10-14T03:00:00.123456Z"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "   ", "not a url", "http://", "ftp://example.com", "https://한국.kr/a" })
	@DisplayName("R-LINK-02: invalid URL returns 400 problem with an error on url and never reaches the service")
	void R_LINK_02_rejects_invalid_url_with_400(String url) throws Exception {
		expectRejected(postLink(url, null, null), "url");
		expectRejected(postLink("{\"memo\":\"no url\"}"), "url");
		verify(linkService, never()).save(any());
	}

	@Test
	@DisplayName("R-LINK-03: a 500 code-point memo mixing Hangul and emoji is accepted (751 UTF-16 units)")
	void R_LINK_03_accepts_500_code_point_memo_mixing_hangul_and_emoji() throws Exception {
		String memo = "가".repeat(249) + "😀".repeat(251);
		assertThat(Texts.codePoints(memo)).isEqualTo(500);
		assertThat(memo.length()).isEqualTo(751);

		postLink(VALID_URL, memo, null).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("R-LINK-03: a 501 code-point memo mixing Hangul and emoji is rejected on memo")
	void R_LINK_03_rejects_501_code_point_memo_mixing_hangul_and_emoji() throws Exception {
		String memo = "가".repeat(249) + "😀".repeat(251) + "가";

		expectRejected(postLink(VALID_URL, memo, null), "memo");
		verify(linkService, never()).save(any());
	}

	@Test
	@DisplayName("R-LINK-08: a skin-tone emoji counts as two code points in the memo limit")
	void R_LINK_08_memo_counts_skin_tone_emoji_as_two() throws Exception {
		postLink(VALID_URL, "가".repeat(498) + "👍🏽", null).andExpect(status().isCreated());
		expectRejected(postLink(VALID_URL, "가".repeat(499) + "👍🏽", null), "memo");
	}

	@Test
	@DisplayName("R-LINK-05: tag count limit of 10 applies after merging case-insensitive duplicates")
	void R_LINK_05_tag_count_boundary_after_merge() throws Exception {
		postLink(VALID_URL, null, distinctTags(10)).andExpect(status().isCreated());
		expectRejected(postLink(VALID_URL, null, distinctTags(11)), "tags");

		List<String> elevenWithCaseDuplicate = new ArrayList<>(distinctTags(10));
		elevenWithCaseDuplicate.add("TAG1");
		postLink(VALID_URL, null, elevenWithCaseDuplicate).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("R-LINK-05: tag length limit is 30 code points")
	void R_LINK_05_tag_length_boundary() throws Exception {
		postLink(VALID_URL, null, List.of("😀".repeat(30))).andExpect(status().isCreated());
		expectRejected(postLink(VALID_URL, null, List.of("😀".repeat(31))), "tags");
	}

	@ParameterizedTest
	@ValueSource(strings = { "\"\"", "\"   \"", "\"　\"", "null" })
	@DisplayName("R-LINK-05: blank or null tag elements are rejected on tags")
	void R_LINK_05_rejects_blank_or_null_tag(String tagJson) throws Exception {
		expectRejected(postLink("{\"url\":\"" + VALID_URL + "\",\"tags\":[\"Java\"," + tagJson + "]}"), "tags");
		verify(linkService, never()).save(any());
	}

	@Test
	@DisplayName("R-LINK-06: all invalid fields are reported in one response")
	void R_LINK_06_reports_all_invalid_fields_at_once() throws Exception {
		expectRejected(postLink("ftp://example.com", "가".repeat(501), List.of("   ")), "url", "memo", "tags");
		verify(linkService, never()).save(any());
	}

	@Test
	@DisplayName("R-LINK-06: malformed JSON returns a 400 problem")
	void R_LINK_06_returns_400_problem_on_malformed_json() throws Exception {
		postLink("{\"url\":")
			.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400));
		verify(linkService, never()).save(any());
	}

}
