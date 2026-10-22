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
		assertThat(urls(linkRepository.findPageFiltered("java", false, LinkRepository.NEWEST_FIRST, 0, 10)))
			.containsExactly("https://example.com/lower", "https://example.com/upper");
	}

	@Test
	@DisplayName("R-LINK-13: only a whole-tag match counts")
	void R_LINK_13_requires_whole_tag() {
		assertThat(linkRepository.findPageFiltered("jav", false, LinkRepository.NEWEST_FIRST, 0, 10).getContent()).isEmpty();
		assertThat(urls(linkRepository.findPageFiltered("javascript", false, LinkRepository.NEWEST_FIRST, 0, 10)))
			.containsExactly("https://example.com/script");
	}

	@Test
	@DisplayName("R-LINK-13: a Hangul tag matches")
	void R_LINK_13_korean_tag() {
		assertThat(urls(linkRepository.findPageFiltered("스프링", false, LinkRepository.NEWEST_FIRST, 0, 10))).containsExactly("https://example.com/lower");
	}

	@Test
	@DisplayName("R-LINK-12: a link with several tags appears once and the total counts filtered links only")
	void R_LINK_12_link_with_many_tags_appears_once_and_total_counts_filtered_only() {
		save("both", List.of("Java", "JAVA-2", "spring"), 4);

		Page<Link> page = linkRepository.findPageFiltered("spring", false, LinkRepository.NEWEST_FIRST, 0, 1);

		assertThat(urls(page)).containsExactly("https://example.com/both");
		assertThat(page.getTotalElements()).isEqualTo(2);
		assertThat(page.getTotalPages()).isEqualTo(2);
		assertThat(urls(linkRepository.findPageFiltered("spring", false, LinkRepository.NEWEST_FIRST, 1, 1)))
			.containsExactly("https://example.com/lower");
	}

	@Test
	@DisplayName("R-LINK-12: a link whose tags match twice (stored outside the save API) still appears once")
	void R_LINK_12_link_matching_twice_appears_once() {
		save("twice", List.of("Java", "java"), 5);

		Page<Link> page = linkRepository.findPageFiltered("java", false, LinkRepository.NEWEST_FIRST, 0, 10);

		assertThat(urls(page)).containsExactly("https://example.com/twice", "https://example.com/lower",
				"https://example.com/upper");
		assertThat(page.getTotalElements()).isEqualTo(3);
	}

	@Test
	@DisplayName("R-LINK-13: non-ASCII letters match regardless of case")
	void R_LINK_13_non_ascii_case_insensitive() {
		save("eclair", List.of("Éclair"), 6);

		assertThat(urls(linkRepository.findPageFiltered("éclair", false, LinkRepository.NEWEST_FIRST, 0, 10))).containsExactly("https://example.com/eclair");
	}

	@Test
	@DisplayName("R-LINK-12: walking small filtered pages returns each link once even when one matches twice")
	void R_LINK_12_small_filtered_pages_without_overlap_or_gap() {
		save("twice", List.of("Java", "java"), 5);

		List<String> seen = new java.util.ArrayList<>();
		for (int p = 0; p < 2; p++) {
			seen.addAll(urls(linkRepository.findPageFiltered("java", false, LinkRepository.NEWEST_FIRST, p, 2)));
		}

		assertThat(seen).containsExactly("https://example.com/twice", "https://example.com/lower",
				"https://example.com/upper");
	}

}
