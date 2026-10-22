package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

@SpringBootTest
@AutoConfigureMockMvc
class UnreadFilterIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	private String tag;

	@BeforeEach
	void uniqueTag() {
		tag = "unread-" + UUID.randomUUID().toString().substring(0, 8);
	}

	private long saveLink(String name) throws Exception {
		String response = mockMvc
			.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"https://example.com/" + tag + "/" + name + "\",\"tags\":[\"" + tag + "\"]}"))
			.andExpect(status().isCreated())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);
		return ((Number) JsonPath.read(response, "$.id")).longValue();
	}

	private void markRead(long id) throws Exception {
		mockMvc
			.perform(put("/api/links/" + id + "/read").contentType(MediaType.APPLICATION_JSON)
				.content("{\"read\":true}"))
			.andExpect(status().isOk());
	}

	private String body(MockHttpServletRequestBuilder request) throws Exception {
		return mockMvc.perform(request)
			.andExpect(status().isOk())
			.andReturn()
			.getResponse()
			.getContentAsString(StandardCharsets.UTF_8);
	}

	@Test
	@DisplayName("R-LINK-22: omitting unread gives exactly the same response as unread=false")
	void R_LINK_22_omitted_unread_response_equals_false_response() throws Exception {
		saveLink("a");
		markRead(saveLink("b"));
		saveLink("c");

		assertThat(body(get("/api/links").param("tag", tag)))
			.isEqualTo(body(get("/api/links").param("tag", tag).param("unread", "false")));
		assertThat(body(get("/api/links").param("size", "100")))
			.isEqualTo(body(get("/api/links").param("size", "100").param("unread", "false")));
	}

	@Test
	@DisplayName("R-LINK-22, R-LINK-23: after marking one read, unread=true with the tag lists only the rest")
	void R_LINK_23_save_mark_read_then_unread_only_lists_rest_end_to_end() throws Exception {
		long a = saveLink("a");
		long b = saveLink("b");
		long c = saveLink("c");
		markRead(b);

		String unread = body(get("/api/links").param("tag", tag).param("unread", "true").param("order", "oldest"));
		String all = body(get("/api/links").param("tag", tag));

		assertThat(JsonPath.<List<Integer>>read(unread, "$.items[*].id")).containsExactly((int) a, (int) c);
		assertThat(JsonPath.<List<Boolean>>read(unread, "$.items[*].read")).containsOnly(false);
		assertThat((Integer) JsonPath.read(unread, "$.totalElements")).isEqualTo(2);
		assertThat((Integer) JsonPath.read(all, "$.totalElements")).isEqualTo(3);
	}

}
