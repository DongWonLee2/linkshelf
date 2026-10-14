package dev.linkshelf.link;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class TagFilterIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LinkRepository linkRepository;

	@BeforeEach
	void saveLinks() throws Exception {
		linkRepository.deleteAll();
		saveLink("{\"url\":\"https://example.com/a\",\"tags\":[\"Java\"]}");
		saveLink("{\"url\":\"https://example.com/b\",\"tags\":[\"java\",\"스프링\"]}");
		saveLink("{\"url\":\"https://example.com/c\"}");
	}

	private void saveLink(String json) throws Exception {
		mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(json))
			.andExpect(status().isCreated());
	}

	@Test
	@DisplayName("R-LINK-13: a padded upper-case tag filters end to end (SC-002)")
	void R_LINK_13_padded_uppercase_tag_filters_end_to_end() throws Exception {
		mockMvc.perform(get("/api/links").param("tag", " JAVA "))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items.length()").value(2))
			.andExpect(jsonPath("$.items[0].url").value("https://example.com/b"))
			.andExpect(jsonPath("$.items[1].url").value("https://example.com/a"))
			.andExpect(jsonPath("$.totalElements").value(2));
	}

	@Test
	@DisplayName("R-LINK-13: an unknown tag returns an empty page")
	void R_LINK_13_unknown_tag_returns_empty_items() throws Exception {
		mockMvc.perform(get("/api/links").param("tag", "없는태그"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items").isEmpty())
			.andExpect(jsonPath("$.totalElements").value(0));
	}

	@Test
	@DisplayName("R-LINK-12: filtered pages keep newest-first order and paging")
	void R_LINK_12_filtered_pages_keep_newest_first() throws Exception {
		mockMvc.perform(get("/api/links").param("tag", "java").param("size", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items[0].url").value("https://example.com/b"))
			.andExpect(jsonPath("$.hasNext").value(true));
		mockMvc.perform(get("/api/links").param("tag", "java").param("size", "1").param("page", "1"))
			.andExpect(jsonPath("$.items[0].url").value("https://example.com/a"))
			.andExpect(jsonPath("$.hasNext").value(false));
	}

	@Test
	@DisplayName("R-LINK-12: without a tag the list is not filtered")
	void R_LINK_12_no_tag_lists_everything() throws Exception {
		mockMvc.perform(get("/api/links")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(3));
	}

}
