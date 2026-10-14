package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ListLinksIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LinkRepository linkRepository;

	@BeforeEach
	void clearLinks() {
		linkRepository.deleteAll();
	}

	private void saveLink(String json) throws Exception {
		mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated());
	}

	private String getLinks(String query) throws Exception {
		return mockMvc.perform(get("/api/links" + query))
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("R-LINK-09: saved links are listed newest first with Hangul memo and tags intact")
	void R_LINK_09_lists_saved_links_newest_first_with_korean_intact() throws Exception {
		saveLink("{\"url\":\"https://example.com/a\",\"memo\":\"첫 번째 😀\",\"tags\":[\"자바\"]}");
		saveLink("{\"url\":\"https://example.com/b\"}");
		saveLink("{\"url\":\"https://example.com/c\",\"tags\":[\"Java\",\"스프링\"]}");

		mockMvc.perform(get("/api/links"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(3))
			.andExpect(jsonPath("$.items[0].url").value("https://example.com/c"))
			.andExpect(jsonPath("$.items[0].tags[1]").value("스프링"))
			.andExpect(jsonPath("$.items[1].url").value("https://example.com/b"))
			.andExpect(jsonPath("$.items[2].url").value("https://example.com/a"))
			.andExpect(jsonPath("$.items[2].memo").value("첫 번째 😀"))
			.andExpect(jsonPath("$.items[2].tags[0]").value("자바"))
			.andExpect(jsonPath("$.totalElements").value(3));
	}

	@Test
	@DisplayName("R-LINK-10: walking every default-size page returns all saved links once (SC-001)")
	void R_LINK_10_all_pages_together_return_every_link() throws Exception {
		for (int i = 0; i < 25; i++) {
			saveLink("{\"url\":\"https://example.com/n" + i + "\"}");
		}

		String first = getLinks("");
		String second = getLinks("?page=1");
		List<String> urls = new ArrayList<>(JsonPath.<List<String>>read(first, "$.items[*].url"));
		urls.addAll(JsonPath.<List<String>>read(second, "$.items[*].url"));

		assertThat((Integer) JsonPath.read(first, "$.size")).isEqualTo(20);
		assertThat((Integer) JsonPath.read(first, "$.totalPages")).isEqualTo(2);
		assertThat((Boolean) JsonPath.read(first, "$.hasNext")).isTrue();
		assertThat((Boolean) JsonPath.read(second, "$.hasNext")).isFalse();
		assertThat(urls).hasSize(25);
		assertThat(new HashSet<>(urls)).hasSize(25);
		assertThat(urls.get(0)).isEqualTo("https://example.com/n24");
	}

	@Test
	@DisplayName("R-LINK-09: an empty store returns an empty page")
	void R_LINK_09_empty_store_returns_empty_items() throws Exception {
		mockMvc.perform(get("/api/links"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items").isEmpty())
			.andExpect(jsonPath("$.totalElements").value(0))
			.andExpect(jsonPath("$.totalPages").value(0))
			.andExpect(jsonPath("$.hasNext").value(false));
	}

	@Test
	@DisplayName("R-LINK-10: a page past the last one is empty")
	void R_LINK_10_page_after_last_is_empty() throws Exception {
		saveLink("{\"url\":\"https://example.com/only\"}");

		mockMvc.perform(get("/api/links?page=5"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items").isEmpty())
			.andExpect(jsonPath("$.totalElements").value(1));
	}

}
