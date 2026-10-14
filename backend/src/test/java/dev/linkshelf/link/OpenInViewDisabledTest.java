package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Issue #5: open-in-view is off, so the persistence context is open only inside the service transaction.
 */
@SpringBootTest
@AutoConfigureMockMvc
class OpenInViewDisabledTest {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private LinkRepository linkRepository;

	@BeforeEach
	void clearLinks() {
		linkRepository.deleteAll();
	}

	@Test
	@DisplayName("CHORE-OSIV-01: no open-entity-manager-in-view interceptor is registered")
	void osiv_interceptor_is_not_registered() {
		assertThat(context.getBeansOfType(OpenEntityManagerInViewInterceptor.class)).isEmpty();
	}

	@Test
	@DisplayName("CHORE-OSIV-01: the save API returns 201 with tags without open-in-view")
	void save_returns_tags_without_open_in_view() throws Exception {
		mockMvc
			.perform(post("/api/links").contentType(MediaType.APPLICATION_JSON).content("""
					{"url":"https://example.com/osiv","tags":["Java","스프링"]}
					"""))
			.andExpect(status().isCreated())
			.andExpect(jsonPath("$.tags.length()").value(2))
			.andExpect(jsonPath("$.tags[0]").value("Java"))
			.andExpect(jsonPath("$.tags[1]").value("스프링"));
	}

}
