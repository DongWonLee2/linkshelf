package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TagNormalizerTest {

	@Test
	@DisplayName("R-LINK-05: tags are stripped and merged ignoring case, keeping first spelling and order")
	void R_LINK_05_strips_and_merges_ignoring_case_keeping_first() {
		assertThat(TagNormalizer.normalize(List.of("Java", " java ", "JAVA", "스프링")))
			.containsExactly("Java", "스프링");
		assertThat(TagNormalizer.normalize(List.of("b", "A", "a", "B"))).containsExactly("b", "A");
		assertThat(TagNormalizer.normalize(null)).isEmpty();
	}

	@Test
	@DisplayName("R-LINK-05: full-width space (U+3000) is stripped")
	void R_LINK_05_strips_fullwidth_space() {
		assertThat(TagNormalizer.normalize(List.of("　태그　"))).containsExactly("태그");
	}

}
