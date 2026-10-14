package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class MarkReadIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LinkRepository linkRepository;

	@BeforeEach
	void clearLinks() {
		linkRepository.deleteAll();
	}

	private String body(ResultActions result) throws Exception {
		return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	private long saveLink() throws Exception {
		String json = body(mockMvc
			.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"https://example.com/a\",\"memo\":\"한글 메모\",\"tags\":[\"Java\",\"스프링\"]}"))
			.andExpect(status().isCreated()));
		return ((Number) JsonPath.read(json, "$.id")).longValue();
	}

	private ResultActions putRead(long id, boolean read) throws Exception {
		return mockMvc.perform(put("/api/links/" + id + "/read").contentType(MediaType.APPLICATION_JSON)
			.content("{\"read\":" + read + "}"));
	}

	@Test
	@DisplayName("R-LINK-16: the save response says the new link is unread")
	void R_LINK_16_save_response_has_read_false() throws Exception {
		mockMvc
			.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON)
				.content("{\"url\":\"https://example.com/new\"}"))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.read").value(false));
	}

	@Test
	@DisplayName("R-LINK-15: marking twice gives the same result and keeps the other fields (SC-003)")
	void R_LINK_15_marking_twice_gives_same_result_and_keeps_other_fields() throws Exception {
		long id = saveLink();
		String before = body(mockMvc.perform(get("/api/links")));

		String first = body(putRead(id, true).andExpect(status().isOk()));
		String second = body(putRead(id, true).andExpect(status().isOk()));

		assertThat(second).isEqualTo(first);
		assertThat((Boolean) JsonPath.read(first, "$.read")).isTrue();
		for (String field : new String[] { "id", "url", "memo", "tags", "createdAt" }) {
			assertThat((Object) JsonPath.read(first, "$." + field))
				.isEqualTo(JsonPath.read(before, "$.items[0]." + field));
		}
	}

	@Test
	@DisplayName("R-LINK-16: list items show the read state after marking")
	void R_LINK_16_list_items_have_read() throws Exception {
		long id = saveLink();
		mockMvc.perform(get("/api/links")).andExpect(jsonPath("$.items[0].read").value(false));

		putRead(id, true).andExpect(status().isOk());

		mockMvc.perform(get("/api/links")).andExpect(jsonPath("$.items[0].read").value(true));
	}

	@Test
	@DisplayName("R-LINK-15: a read link can be marked unread again")
	void R_LINK_15_can_mark_unread_again() throws Exception {
		long id = saveLink();
		putRead(id, true).andExpect(status().isOk());

		putRead(id, false).andExpect(status().isOk()).andExpect(jsonPath("$.read").value(false));
		mockMvc.perform(get("/api/links")).andExpect(jsonPath("$.items[0].read").value(false));
	}

	@Test
	@DisplayName("R-LINK-17: an unknown id answers 404 and changes nothing")
	void R_LINK_17_unknown_id_changes_nothing() throws Exception {
		long id = saveLink();

		putRead(id + 1000, true).andExpect(status().isNotFound());

		mockMvc.perform(get("/api/links"))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.items[0].read").value(false));
	}

}
