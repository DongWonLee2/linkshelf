package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
class SaveLinkIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoSpyBean
	private LinkRepository linkRepository;

	@Autowired
	private PlatformTransactionManager transactionManager;

	@BeforeEach
	void clearLinks() {
		linkRepository.deleteAll();
	}

	private ResultActions postLink(String json) throws Exception {
		return mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	@Test
	@DisplayName("R-LINK-01: a link is normalized, saved and returned end to end")
	void R_LINK_01_saves_link_end_to_end() throws Exception {
		postLink("""
				{"url":" HTTPS://Example.COM:443/a ","memo":" 한글 메모 😀 ","tags":["Java"," java ","JAVA","스프링"]}
				""")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.id").isNumber())
			.andExpect(jsonPath("$.url").value("https://example.com/a"))
			.andExpect(jsonPath("$.memo").value("한글 메모 😀"))
			.andExpect(jsonPath("$.tags.length()").value(2))
			.andExpect(jsonPath("$.tags[0]").value("Java"))
			.andExpect(jsonPath("$.tags[1]").value("스프링"))
			.andExpect(jsonPath("$.createdAt").isString());

		assertThat(linkRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("R-LINK-01: a URL-only request (or a whitespace-only memo) stores null memo and empty tags")
	void R_LINK_01_url_only_returns_null_memo_and_empty_tags() throws Exception {
		postLink("""
				{"url":"https://example.com/only"}
				""")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.memo").value(nullValue()))
			.andExpect(jsonPath("$.tags").isArray())
			.andExpect(jsonPath("$.tags").isEmpty());

		postLink("{\"url\":\"https://example.com/blank-memo\",\"memo\":\"  \u3000 \",\"tags\":[]}")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.memo").value(nullValue()))
			.andExpect(jsonPath("$.tags").isEmpty());
	}

	@Test
	@DisplayName("R-LINK-03: a 500 code-point memo and ten 30 code-point tags are saved together")
	void R_LINK_03_saves_max_memo_and_max_tags_together() throws Exception {
		String memo = "가".repeat(249) + "😀".repeat(251);
		List<String> tags = IntStream.range(0, 10).mapToObj(i -> i + "😀".repeat(29)).toList();
		String json = "{\"url\":\"https://example.com/max\",\"memo\":\"" + memo + "\",\"tags\":"
				+ tags.stream().map(tag -> "\"" + tag + "\"").collect(Collectors.joining(",", "[", "]")) + "}";

		postLink(json)
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.memo").value(memo))
			.andExpect(jsonPath("$.tags.length()").value(10))
			.andExpect(jsonPath("$.tags[0]").value(tags.get(0)))
			.andExpect(jsonPath("$.tags[9]").value(tags.get(9)));
		assertThat(linkRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("R-LINK-03: max memo and ten max tags saved through the service read back equal from the DB, tag order kept")
	void R_LINK_03_round_trips_max_memo_and_max_tags_through_db() throws Exception {
		String memo = "가".repeat(248) + "😀".repeat(250) + "👍🏽";
		List<String> tags = IntStream.iterate(9, i -> i >= 0, i -> i - 1)
			.mapToObj(i -> i + "태그" + "😀".repeat(27))
			.toList();
		assertThat(Texts.codePoints(memo)).isEqualTo(500);
		assertThat(tags).allSatisfy(tag -> assertThat(Texts.codePoints(tag)).isEqualTo(30));
		String json = "{\"url\":\"https://example.com/round-trip\",\"memo\":\"" + memo + "\",\"tags\":"
				+ tags.stream().map(tag -> "\"" + tag + "\"").collect(Collectors.joining(",", "[", "]")) + "}";

		String body = postLink(json).andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);
		long id = ((Number) JsonPath.read(body, "$.id")).longValue();

		// The service transaction has committed; a new transaction starts with an empty persistence context.
		Link reloaded = new TransactionTemplate(transactionManager).execute(status -> {
			Link link = linkRepository.findById(id).orElseThrow();
			return new Link(link.getNormalizedUrl(), link.getMemo(), link.getTags(), link.getCreatedAt());
		});

		assertThat(reloaded.getNormalizedUrl()).isEqualTo("https://example.com/round-trip");
		assertThat(reloaded.getMemo()).isEqualTo(memo);
		assertThat(reloaded.getTags()).containsExactlyElementsOf(tags);
	}

	@ParameterizedTest
	@ValueSource(strings = { """
			{"url":"ftp://example.com/a"}
			""", """
			{"url":"not a url"}
			""", """
			{"memo":"URL 없음"}
			""", """
			{"url":"https://example.com/a","tags":["   "]}
			""" })
	@DisplayName("R-LINK-06: rejected requests store nothing")
	void R_LINK_06_rejected_requests_store_nothing(String json) throws Exception {
		postLink(json).andExpect(status().isBadRequest());

		assertThat(linkRepository.count()).isZero();
	}

	@Test
	@DisplayName("R-LINK-04: a standard-equivalent URL is rejected with 409 and nothing new is stored")
	void R_LINK_04_rejects_equivalent_url_with_409_and_keeps_count() throws Exception {
		postLink("""
				{"url":"https://example.com/a"}
				""").andExpect(status().isCreated());

		postLink("""
				{"url":"HTTPS://Example.COM:443/a","memo":"다시 저장"}
				""")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.url").value("https://example.com/a"));
		assertThat(linkRepository.count()).isEqualTo(1);

		postLink("""
				{"url":"https://example.com/a/"}
				""").andExpect(status().isCreated());
		assertThat(linkRepository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("R-LINK-04: a bare host and the same host with \"/\" are one URL (409, count stays 1)")
	void R_LINK_04_rejects_slash_variant_of_bare_host_with_409() throws Exception {
		postLink("""
				{"url":"https://example.com"}
				""")
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.url").value("https://example.com/"));

		postLink("""
				{"url":"https://example.com/"}
				""").andExpect(status().isConflict());
		assertThat(linkRepository.count()).isEqualTo(1);
	}

	@Test
	@DisplayName("R-LINK-04: when the pre-check misses (concurrent save), the unique constraint still yields 409")
	void R_LINK_04_translates_unique_violation_to_409_when_precheck_misses() throws Exception {
		doReturn(false).when(linkRepository).existsByNormalizedUrl(anyString());

		postLink("""
				{"url":"https://example.com/race"}
				""").andExpect(status().isCreated());
		postLink("""
				{"url":"https://EXAMPLE.com/race"}
				""")
			.andExpect(status().isConflict())
			.andExpect(jsonPath("$.url").value("https://example.com/race"));

		verify(linkRepository, times(2)).saveAndFlush(any());
		assertThat(linkRepository.count()).isEqualTo(1);
	}

}
