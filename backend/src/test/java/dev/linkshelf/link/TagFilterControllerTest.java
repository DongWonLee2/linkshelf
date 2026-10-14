package dev.linkshelf.link;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(LinkController.class)
class TagFilterControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LinkService linkService;

	@BeforeEach
	void stubService() {
		given(linkService.list(anyInt(), anyInt(), any())).willReturn(new LinkPageResponse(List.of(), 0, 20, 0, 0, false));
	}

	private void expectTagRejected(ResultActions result, String message) throws Exception {
		result.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.detail").value("Invalid request parameters."))
			.andExpect(jsonPath("$.errors.length()").value(1))
			.andExpect(jsonPath("$.errors[0].field").value("tag"))
			.andExpect(jsonPath("$.errors[0].message").value(message));
		verify(linkService, never()).list(anyInt(), anyInt(), any());
	}

	@ParameterizedTest
	@ValueSource(strings = { "", "  ", "\u3000" })
	@DisplayName("R-LINK-14: an empty or whitespace-only tag (including an ideographic space) is rejected")
	void R_LINK_14_blank_tag_400(String tag) throws Exception {
		expectTagRejected(mockMvc.perform(get("/api/links").param("tag", tag)), "공백만 있을 수 없습니다");
	}

	@Test
	@DisplayName("R-LINK-14: a tag over 30 code points is rejected")
	void R_LINK_14_tag_over_30_code_points_400() throws Exception {
		expectTagRejected(mockMvc.perform(get("/api/links").param("tag", "a" + "😀".repeat(30))), "30자 이하여야 합니다");
	}

	@Test
	@DisplayName("R-LINK-14: a tag of exactly 30 code points (emoji count as one) is accepted")
	void R_LINK_14_tag_of_30_code_points_ok() throws Exception {
		String tag = "😀".repeat(30);
		mockMvc.perform(get("/api/links").param("tag", tag)).andExpect(status().isOk());

		verify(linkService).list(0, 20, tag);
	}

	@Test
	@DisplayName("R-LINK-12: the tag is passed to the service as sent")
	void R_LINK_12_passes_tag_to_service() throws Exception {
		mockMvc.perform(get("/api/links").param("tag", " JAVA ").param("page", "1").param("size", "5"))
			.andExpect(status().isOk());

		verify(linkService).list(1, 5, " JAVA ");
	}

}
