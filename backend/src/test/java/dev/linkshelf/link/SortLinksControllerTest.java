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
class SortLinksControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LinkService linkService;

	@BeforeEach
	void stubService() {
		given(linkService.list(anyInt(), anyInt(), any(), any(), anyBoolean()))
			.willReturn(new LinkPageResponse(List.of(), 0, 20, 0, 0, false));
	}

	private ResultActions getLinks(String... orders) throws Exception {
		return mockMvc.perform(get("/api/links").param("order", orders));
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
	@DisplayName("R-LINK-19: an omitted order means newest; newest and oldest are passed to the service")
	void R_LINK_19_passes_order_to_service() throws Exception {
		mockMvc.perform(get("/api/links")).andExpect(status().isOk());
		verify(linkService).list(0, 20, null, LinkOrder.NEWEST, false);

		mockMvc.perform(get("/api/links").param("order", "newest").param("page", "1")).andExpect(status().isOk());
		verify(linkService).list(1, 20, null, LinkOrder.NEWEST, false);

		mockMvc.perform(get("/api/links").param("order", "oldest").param("page", "2").param("tag", "java"))
			.andExpect(status().isOk());
		verify(linkService).list(2, 20, "java", LinkOrder.OLDEST, false);
	}

	@Test
	@DisplayName("R-LINK-21: an order other than exactly newest or oldest is rejected, not corrected (sent twice too)")
	void R_LINK_21_rejects_unknown_order_400() throws Exception {
		expectRejected(getLinks("bad"), "order");
		expectRejected(getLinks("NEWEST"), "order");
		expectRejected(getLinks(" newest"), "order");
		expectRejected(getLinks("newest", "oldest"), "order");
	}

	@Test
	@DisplayName("R-LINK-21: an empty order (order=) is rejected rather than treated as omitted (research R1)")
	void R_LINK_21_rejects_empty_order_400() throws Exception {
		expectRejected(mockMvc.perform(get("/api/links?order=")), "order");
	}

	@Test
	@DisplayName("R-LINK-11, R-LINK-21: bad order, page and size are reported together")
	void R_LINK_21_reports_order_with_page_and_size_together() throws Exception {
		expectRejected(mockMvc.perform(get("/api/links").param("order", "bad").param("page", "-1").param("size", "0")),
				"order", "page", "size");
	}

	@Test
	@DisplayName("R-LINK-21: the order error message matches the contract")
	void R_LINK_21_error_message_matches_contract() throws Exception {
		getLinks("bad").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Invalid request parameters."))
			.andExpect(jsonPath("$.errors.length()").value(1))
			.andExpect(jsonPath("$.errors[0].field").value("order"))
			.andExpect(jsonPath("$.errors[0].message").value("newest 또는 oldest여야 합니다"));
	}

}
