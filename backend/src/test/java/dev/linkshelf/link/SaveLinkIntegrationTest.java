package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class SaveLinkIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
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

}
