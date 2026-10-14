package dev.linkshelf.link;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

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
class MarkReadControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LinkService linkService;

	private ResultActions putRead(String id, String json) throws Exception {
		return mockMvc.perform(put("/api/links/" + id + "/read").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	@Test
	@DisplayName("R-LINK-15: marking a link returns the changed link with its read state")
	void R_LINK_15_returns_changed_link() throws Exception {
		given(linkService.markRead(7L, true)).willReturn(new LinkResponse(7L, "https://example.com/a", null,
				List.of("Java"), Instant.parse("2026-10-14T03:00:00Z"), true));

		putRead("7", "{\"read\":true}").andExpect(status().isOk())
			.andExpect(jsonPath("$.id").value(7))
			.andExpect(jsonPath("$.read").value(true))
			.andExpect(jsonPath("$.tags[0]").value("Java"));
		verify(linkService).markRead(7L, true);
	}

	@ParameterizedTest
	@ValueSource(strings = { "{}", "{\"read\":null}" })
	@DisplayName("R-LINK-17: a missing or null read value is rejected with the read field reported")
	void R_LINK_17_missing_or_null_read_400(String json) throws Exception {
		putRead("7", json).andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.errors[0].field").value("read"))
			.andExpect(jsonPath("$.errors[0].message").value("true 또는 false여야 합니다"));
		verify(linkService, never()).markRead(anyLong(), anyBoolean());
	}

	@ParameterizedTest
	@ValueSource(strings = { "{\"read\":\"true\"}", "{\"read\":1}", "{\"read\":\"yes\"}", "{\"read\":[true]}" })
	@DisplayName("R-LINK-17: a read value that is not a JSON boolean is rejected")
	void R_LINK_17_non_boolean_read_400(String json) throws Exception {
		putRead("7", json).andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("read"));
		verify(linkService, never()).markRead(anyLong(), anyBoolean());
	}

	@Test
	@DisplayName("R-LINK-17: a non-numeric id is rejected with the id field reported")
	void R_LINK_17_non_numeric_id_400() throws Exception {
		putRead("abc", "{\"read\":true}").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[0].field").value("id"));
	}

	@Test
	@DisplayName("R-LINK-17: an unknown id answers 404 with the id")
	void R_LINK_17_unknown_id_404_shape() throws Exception {
		given(linkService.markRead(999L, true)).willThrow(new LinkNotFoundException(999L));

		putRead("999", "{\"read\":true}").andExpect(status().isNotFound())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(404))
			.andExpect(jsonPath("$.detail").value("링크를 찾을 수 없습니다"))
			.andExpect(jsonPath("$.instance").value("/api/links/999/read"))
			.andExpect(jsonPath("$.id").value(999));
	}

	@Test
	@DisplayName("R-LINK-17: a bad body is rejected before the link is looked up")
	void R_LINK_17_bad_body_before_unknown_id() throws Exception {
		putRead("999", "{}").andExpect(status().isBadRequest());
		verify(linkService, never()).markRead(anyLong(), anyBoolean());
	}

}
