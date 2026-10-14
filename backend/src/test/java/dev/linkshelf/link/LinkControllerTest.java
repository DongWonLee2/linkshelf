package dev.linkshelf.link;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(LinkController.class)
class LinkControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private LinkService linkService;

	private ResultActions postLink(String json) throws Exception {
		return mockMvc.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content(json));
	}

	@Test
	@DisplayName("R-LINK-01: valid request returns 201 with the saved link")
	void R_LINK_01_returns_201_with_saved_link() throws Exception {
		given(linkService.save(any())).willReturn(new LinkResponse(1L, "https://example.com/a", "한글 메모 😀",
				List.of("Java", "스프링"), Instant.parse("2026-10-14T03:00:00.123456Z")));

		postLink("""
				{"url":"HTTPS://Example.COM:443/a","memo":"한글 메모 😀","tags":["Java"," java ","스프링"]}
				""")
			.andExpect(status().isCreated())
			.andExpect(header().doesNotExist("Location"))
			.andExpect(jsonPath("$.id").value(1))
			.andExpect(jsonPath("$.url").value("https://example.com/a"))
			.andExpect(jsonPath("$.memo").value("한글 메모 😀"))
			.andExpect(jsonPath("$.tags[0]").value("Java"))
			.andExpect(jsonPath("$.tags[1]").value("스프링"))
			.andExpect(jsonPath("$.createdAt").value("2026-10-14T03:00:00.123456Z"));
	}

}
