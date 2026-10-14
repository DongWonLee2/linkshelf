package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@SpringBootTest
@AutoConfigureMockMvc
class SaveLinkIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoSpyBean
	private LinkRepository linkRepository;

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
