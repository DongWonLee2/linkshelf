package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;

@DataJpaTest
class TagFilterRepositoryTest {

	private static final Instant T0 = Instant.parse("2026-10-14T03:00:00Z");

	@Autowired
	private LinkRepository linkRepository;

	@BeforeEach
	void saveLinks() {
		save("upper", List.of("Java"), 0);
		save("lower", List.of("java", "스프링", "Spring"), 1);
		save("script", List.of("JavaScript"), 2);
		save("none", List.of(), 3);
	}

	private void save(String path, List<String> tags, int seconds) {
		linkRepository.saveAndFlush(new Link("https://example.com/" + path, null, tags, T0.plusSeconds(seconds)));
	}

	private static List<String> urls(Page<Link> page) {
		return page.getContent().stream().map(Link::getNormalizedUrl).toList();
	}

	@Test
	@DisplayName("R-LINK-13: a lower-cased tag matches stored tags regardless of case, newest first")
	void R_LINK_13_matches_case_insensitively() {
		assertThat(urls(linkRepository.findNewestFirstByTag("java", 0, 10)))
			.containsExactly("https://example.com/lower", "https://example.com/upper");
	}

	@Test
	@DisplayName("R-LINK-13: only a whole-tag match counts")
	void R_LINK_13_requires_whole_tag() {
		assertThat(linkRepository.findNewestFirstByTag("jav", 0, 10).getContent()).isEmpty();
		assertThat(urls(linkRepository.findNewestFirstByTag("javascript", 0, 10)))
			.containsExactly("https://example.com/script");
	}

	@Test
	@DisplayName("R-LINK-13: a Hangul tag matches")
	void R_LINK_13_korean_tag() {
		assertThat(urls(linkRepository.findNewestFirstByTag("스프링", 0, 10))).containsExactly("https://example.com/lower");
	}

	@Test
	@DisplayName("R-LINK-12: a link with several tags appears once and the total counts filtered links only")
	void R_LINK_12_link_with_many_tags_appears_once_and_total_counts_filtered_only() {
		save("both", List.of("Java", "JAVA-2", "spring"), 4);

		Page<Link> page = linkRepository.findNewestFirstByTag("spring", 0, 1);

		assertThat(urls(page)).containsExactly("https://example.com/both");
		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getTotalPages()).isEqualTo(2);
		assertThat(urls(linkRepository.findNewestFirstByTag("spring", 1, 1)))
			.containsExactly("https://example.com/lower");
	}

}
