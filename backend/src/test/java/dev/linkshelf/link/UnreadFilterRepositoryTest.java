package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;

@DataJpaTest
class UnreadFilterRepositoryTest {

	private static final Instant T0 = Instant.parse("2026-10-22T03:00:00Z");

	@Autowired
	private LinkRepository linkRepository;

	private Link save(String path, List<String> tags, Instant createdAt, boolean read) {
		Link link = new Link("https://example.com/" + path, null, tags, createdAt);
		link.markRead(read);
		return linkRepository.saveAndFlush(link);
	}

	private static List<String> urls(Page<Link> page) {
		return page.getContent().stream().map(Link::getNormalizedUrl).toList();
	}

	@Test
	@DisplayName("R-LINK-22: unread only leaves out links marked read")
	void R_LINK_22_unread_only_excludes_read_links() {
		save("a", List.of(), T0, false);
		save("b", List.of(), T0.plusSeconds(10), true);
		save("c", List.of(), T0.plusSeconds(20), false);

		Page<Link> page = linkRepository.findPageFiltered(null, true, LinkRepository.NEWEST_FIRST, 0, 10);

		assertThat(urls(page)).containsExactly("https://example.com/c", "https://example.com/a");
		assertThat(page.getTotalElements()).isEqualTo(2);
	}

	@Test
	@DisplayName("R-LINK-22: without unread only, the filtered query lists the same links as before")
	void R_LINK_22_without_unread_matches_existing_paths() {
		save("a", List.of("java"), T0, false);
		save("b", List.of("spring"), T0.plusSeconds(10), true);
		save("c", List.of("Java"), T0.plusSeconds(20), true);

		Page<Link> all = linkRepository.findPageFiltered(null, false, LinkRepository.NEWEST_FIRST, 0, 10);
		Page<Link> java = linkRepository.findPageFiltered("java", false, LinkRepository.NEWEST_FIRST, 0, 10);

		assertThat(urls(all)).isEqualTo(urls(linkRepository.findPage(LinkRepository.NEWEST_FIRST, 0, 10)));
		assertThat(urls(java)).containsExactly("https://example.com/c", "https://example.com/a");
	}

	@Test
	@DisplayName("R-LINK-23: with a tag, only unread links with that tag are listed and counted")
	void R_LINK_23_unread_with_tag_filters_both_and_counts_filtered_only() {
		save("java-unread-old", List.of("Java"), T0, false);
		save("java-read", List.of("java"), T0.plusSeconds(10), true);
		save("spring-unread", List.of("spring"), T0.plusSeconds(20), false);
		save("java-unread-new", List.of("JAVA", "spring"), T0.plusSeconds(30), false);

		Page<Link> first = linkRepository.findPageFiltered("java", true, LinkRepository.NEWEST_FIRST, 0, 1);
		Page<Link> second = linkRepository.findPageFiltered("java", true, LinkRepository.NEWEST_FIRST, 1, 1);

		assertThat(urls(first)).containsExactly("https://example.com/java-unread-new");
		assertThat(urls(second)).containsExactly("https://example.com/java-unread-old");
		assertThat(first.getTotalElements()).isEqualTo(2);
		assertThat(first.getTotalPages()).isEqualTo(2);
		assertThat(second.hasNext()).isFalse();
	}

	@Test
	@DisplayName("R-LINK-23: unread links are sorted oldest first when asked")
	void R_LINK_23_unread_with_oldest_order() {
		save("new", List.of(), T0.plusSeconds(20), false);
		save("read", List.of(), T0.plusSeconds(5), true);
		save("same-1", List.of(), T0.plusSeconds(10), false);
		save("same-2", List.of(), T0.plusSeconds(10), false);

		Page<Link> page = linkRepository.findPageFiltered(null, true, LinkRepository.OLDEST_FIRST, 0, 10);

		assertThat(urls(page)).containsExactly("https://example.com/same-1", "https://example.com/same-2",
				"https://example.com/new");
	}

	@Test
	@DisplayName("R-LINK-23: walking every unread page returns each unread link exactly once")
	void R_LINK_23_unread_pages_without_overlap_or_gap() {
		for (int i = 0; i < 7; i++) {
			save("p" + i, List.of(), T0.plusSeconds(i % 2), i % 3 == 0);
		}

		List<String> seen = new ArrayList<>();
		for (int p = 0; p < 3; p++) {
			seen.addAll(urls(linkRepository.findPageFiltered(null, true, LinkRepository.OLDEST_FIRST, p, 2)));
		}

		assertThat(seen).containsExactly("https://example.com/p2", "https://example.com/p4", "https://example.com/p1",
				"https://example.com/p5");
		assertThat(new HashSet<>(seen)).hasSize(4);
		assertThat(linkRepository.findPageFiltered(null, true, LinkRepository.OLDEST_FIRST, 2, 2).getContent())
			.isEmpty();
	}

	@Test
	@DisplayName("R-LINK-10, R-LINK-23: a huge page with unread only is an empty page that still counts unread links")
	void R_LINK_23_unread_with_huge_page_is_empty() {
		save("unread", List.of("java"), T0, false);
		save("read", List.of("java"), T0.plusSeconds(10), true);

		Page<Link> page = linkRepository.findPageFiltered("java", true, LinkRepository.NEWEST_FIRST,
				Integer.MAX_VALUE, 100);

		assertThat(page.getContent()).isEmpty();
		assertThat(page.getTotalElements()).isEqualTo(1);
		assertThat(page.hasNext()).isFalse();
	}

}
