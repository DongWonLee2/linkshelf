package dev.linkshelf.link;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(LinkController.class)
class UnreadFilterControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LinkService linkService;

	@BeforeEach
	void stubService() {
		given(linkService.list(anyInt(), anyInt(), any(), any(), anyBoolean()))
			.willReturn(new LinkPageResponse(List.of(), 0, 20, 0, 0, false));
	}

	private ResultActions getLinks(String... unread) throws Exception {
		return mockMvc.perform(get("/api/links").param("unread", unread));
	}

	private void expectRejected(ResultActions result, String... fields) throws Exception {
		result.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.detail").value("Invalid request parameters."))
			.andExpect(jsonPath("$.instance").value("/api/links"))
			.andExpect(jsonPath("$.errors.length()").value(fields.length))
			.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder((Object[]) fields)));
		verify(linkService, never()).list(anyInt(), anyInt(), any(), any(), anyBoolean());
	}

	@Test
	@DisplayName("R-LINK-22: an omitted unread means false; true and false are passed to the service")
	void R_LINK_22_passes_unread_to_service() throws Exception {
		mockMvc.perform(get("/api/links")).andExpect(status().isOk());
		verify(linkService).list(0, 20, null, LinkOrder.NEWEST, false);

		mockMvc.perform(get("/api/links").param("unread", "false").param("page", "1")).andExpect(status().isOk());
		verify(linkService).list(1, 20, null, LinkOrder.NEWEST, false);

		mockMvc
			.perform(get("/api/links").param("unread", "true")
				.param("page", "2")
				.param("tag", "java")
				.param("order", "oldest"))
			.andExpect(status().isOk());
		verify(linkService).list(2, 20, "java", LinkOrder.OLDEST, true);
	}

	@Test
	@DisplayName("R-LINK-24: other true/false spellings that Spring's Boolean binding would accept are rejected")
	void R_LINK_24_rejects_other_boolean_spellings_400() throws Exception {
		expectRejected(getLinks("yes"), "unread");
		expectRejected(getLinks("on"), "unread");
		expectRejected(getLinks("1"), "unread");
		expectRejected(getLinks("TRUE"), "unread");
		expectRejected(getLinks(" true"), "unread");
	}

	@Test
	@DisplayName("R-LINK-24: an empty unread (unread=) is rejected rather than treated as omitted")
	void R_LINK_24_rejects_empty_unread_400() throws Exception {
		expectRejected(mockMvc.perform(get("/api/links?unread=")), "unread");
	}

	@Test
	@DisplayName("R-LINK-24: unread sent twice is rejected")
	void R_LINK_24_rejects_repeated_unread_400() throws Exception {
		expectRejected(getLinks("true", "false"), "unread");
	}

	@Test
	@DisplayName("R-LINK-11, R-LINK-24: bad unread, order and page are reported together")
	void R_LINK_24_reports_unread_with_other_errors_together() throws Exception {
		expectRejected(
				mockMvc.perform(get("/api/links").param("unread", "1").param("order", "bad").param("page", "-1")),
				"unread", "order", "page");
	}

	@Test
	@DisplayName("R-LINK-11 (exception B5), R-LINK-24: a non-numeric page is reported alone, even with a bad unread")
	void R_LINK_24_non_numeric_page_with_bad_unread_reports_only_page() throws Exception {
		expectRejected(mockMvc.perform(get("/api/links").param("unread", "yes").param("page", "abc")), "page");
	}

	@Test
	@DisplayName("R-LINK-24: the unread error message matches the contract")
	void R_LINK_24_error_message_matches_contract() throws Exception {
		getLinks("yes").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Invalid request parameters."))
			.andExpect(jsonPath("$.errors.length()").value(1))
			.andExpect(jsonPath("$.errors[0].field").value("unread"))
			.andExpect(jsonPath("$.errors[0].message").value("true 또는 false여야 합니다"));
	}

}
