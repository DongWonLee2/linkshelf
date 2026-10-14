package dev.linkshelf.link;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
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
class ListLinksControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LinkService linkService;

	@BeforeEach
	void stubService() {
		LinkResponse link = new LinkResponse(3L, "https://example.com/c", "메모", List.of("Java", "스프링"),
				Instant.parse("2026-10-14T03:00:02Z"), false);
		given(linkService.list(anyInt(), anyInt(), isNull())).willReturn(new LinkPageResponse(List.of(link), 0, 20, 1, 1, false));
	}

	private ResultActions getLinks(String query) throws Exception {
		return mockMvc.perform(get("/api/links" + query));
	}

	private void expectRejected(ResultActions result, String... fields) throws Exception {
		result.andExpect(status().isBadRequest())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.title").value("Bad Request"))
			.andExpect(jsonPath("$.detail").value("Invalid request parameters."))
			.andExpect(jsonPath("$.instance").value("/api/links"))
			.andExpect(jsonPath("$.errors.length()").value(fields.length))
			.andExpect(jsonPath("$.errors[*].field", containsInAnyOrder((Object[]) fields)));
		verify(linkService, never()).list(anyInt(), anyInt(), any());
	}

	@Test
	@DisplayName("R-LINK-10: without parameters the first page of 20 is requested (and no tag is required)")
	void R_LINK_10_defaults_page_0_size_20() throws Exception {
		getLinks("").andExpect(status().isOk());

		verify(linkService).list(0, 20, null);
	}

	@Test
	@DisplayName("R-LINK-10: the response is a page object with items and paging fields")
	void R_LINK_10_returns_page_object_shape() throws Exception {
		getLinks("?page=0&size=20").andExpect(status().isOk())
			.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
			.andExpect(jsonPath("$.items.length()").value(1))
			.andExpect(jsonPath("$.items[0].id").value(3))
			.andExpect(jsonPath("$.items[0].url").value("https://example.com/c"))
			.andExpect(jsonPath("$.items[0].memo").value("메모"))
			.andExpect(jsonPath("$.items[0].tags[1]").value("스프링"))
			.andExpect(jsonPath("$.items[0].createdAt").value("2026-10-14T03:00:02Z"))
			.andExpect(jsonPath("$.page").value(0))
			.andExpect(jsonPath("$.size").value(20))
			.andExpect(jsonPath("$.totalElements").value(1))
			.andExpect(jsonPath("$.totalPages").value(1))
			.andExpect(jsonPath("$.hasNext").value(false));
	}

	@ParameterizedTest
	@ValueSource(ints = { 1, 100 })
	@DisplayName("R-LINK-11: page sizes 1 and 100 are accepted")
	void R_LINK_11_size_bounds_accepted(int size) throws Exception {
		getLinks("?size=" + size).andExpect(status().isOk());

		verify(linkService).list(0, size, null);
	}

	@ParameterizedTest
	@ValueSource(ints = { 0, 101, -1 })
	@DisplayName("R-LINK-11: page sizes outside 1..100 are rejected")
	void R_LINK_11_size_out_of_bounds_400(int size) throws Exception {
		expectRejected(getLinks("?size=" + size), "size");
	}

	@Test
	@DisplayName("R-LINK-11: a negative page is rejected")
	void R_LINK_11_negative_page_400() throws Exception {
		expectRejected(getLinks("?page=-1"), "page");
	}

	@Test
	@DisplayName("R-LINK-11: a bad page and a bad size are reported together")
	void R_LINK_11_reports_page_and_size_together() throws Exception {
		expectRejected(getLinks("?page=-1&size=0"), "page", "size");
	}

	@Test
	@DisplayName("R-LINK-11: error messages match the contract")
	void R_LINK_11_error_messages_match_contract() throws Exception {
		getLinks("?page=-1&size=101").andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.errors[?(@.field == 'page')].message").value("0 이상이어야 합니다"))
			.andExpect(jsonPath("$.errors[?(@.field == 'size')].message").value("1 이상 100 이하여야 합니다"));
		getLinks("?size=ten").andExpect(jsonPath("$.errors[0].message").value("숫자여야 합니다"));
	}

	@Test
	@DisplayName("R-LINK-11 (approved exception B5): a non-numeric value is reported alone")
	void R_LINK_11_non_numeric_page_reports_only_that_field() throws Exception {
		expectRejected(getLinks("?page=abc&size=0"), "page");
	}

	@Test
	@DisplayName("R-LINK-11: a non-numeric size is rejected")
	void R_LINK_11_non_numeric_size_400() throws Exception {
		expectRejected(getLinks("?size=ten"), "size");
	}

}
