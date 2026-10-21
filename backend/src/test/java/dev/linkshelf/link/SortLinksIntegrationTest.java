package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** The store is shared with other tests, so order and counts are asserted only on links carrying a unique tag. */
@SpringBootTest
@AutoConfigureMockMvc
class SortLinksIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private String tag;

	@BeforeEach
	void uniqueTag() {
		tag = "sort-" + UUID.randomUUID().toString().substring(0, 8);
	}

	private String saveLink(String name) throws Exception {
		String url = "https://example.com/" + tag + "/" + name;
		mockMvc
			.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"" + url + "\",\"tags\":[\"" + tag + "\"]}"))
			.andExpect(status().isCreated());
		return url;
	}

	private String body(MockHttpServletRequestBuilder request) throws Exception {
		return mockMvc.perform(request)
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("R-LINK-19: omitting order gives exactly the same response as order=newest")
	void R_LINK_19_omitted_order_response_equals_newest_response() throws Exception {
		saveLink("a");
		saveLink("b");
		saveLink("c");

		assertThat(body(get("/api/links").param("tag", tag)))
			.isEqualTo(body(get("/api/links").param("tag", tag).param("order", "newest")));
		assertThat(body(get("/api/links").param("size", "100")))
			.isEqualTo(body(get("/api/links").param("size", "100").param("order", "newest")));
	}

	@Test
	@DisplayName("R-LINK-19, R-LINK-20: order=oldest lists saved links in saving order and pages them")
	void R_LINK_19_oldest_lists_saved_order_end_to_end() throws Exception {
		String a = saveLink("a");
		String b = saveLink("b");
		String c = saveLink("c");

		String oldest = body(get("/api/links").param("tag", tag).param("order", "oldest"));
		String newest = body(get("/api/links").param("tag", tag));
		String secondPage = body(
				get("/api/links").param("tag", tag).param("order", "oldest").param("size", "2").param("page", "1"));

		assertThat(JsonPath.<List<String>>read(oldest, "$.items[*].url")).containsExactly(a, b, c);
		assertThat(JsonPath.<List<String>>read(newest, "$.items[*].url")).containsExactly(c, b, a);
		assertThat((Integer) JsonPath.read(oldest, "$.totalElements")).isEqualTo(3);
		assertThat(JsonPath.<List<String>>read(secondPage, "$.items[*].url")).containsExactly(c);
		assertThat((Integer) JsonPath.read(secondPage, "$.totalPages")).isEqualTo(2);
	}

	@Test
	@DisplayName("R-LINK-10, R-LINK-20: a huge page with order=oldest is an empty page, not a server error")
	void R_LINK_20_oldest_with_huge_page_is_empty_not_server_error() throws Exception {
		saveLink("only");

		mockMvc
			.perform(get("/api/links").param("tag", tag)
				.param("order", "oldest")
				.param("page", "2147483647")
				.param("size", "100"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items").isEmpty())
			.andExpect(jsonPath("$.page").value(2147483647))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.hasNext").value(false));
		mockMvc.perform(get("/api/links").param("order", "oldest").param("page", "2147483647").param("size", "1"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.items").isEmpty());
	}

}
