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
class SortLinksRepositoryTest {

	private static final Instant T0 = Instant.parse("2026-10-21T03:00:00Z");

	@Autowired
	private LinkRepository linkRepository;

	private Link save(String path, List<String> tags, Instant createdAt) {
		return linkRepository.saveAndFlush(new Link("https://example.com/" + path, null, tags, createdAt));
	}

	private Link save(String path, Instant createdAt) {
		return save(path, List.of(), createdAt);
	}

	private static List<String> urls(Page<Link> page) {
		return page.getContent().stream().map(Link::getNormalizedUrl).toList();
	}

	@Test
	@DisplayName("R-LINK-19: oldest first orders by created time ascending, then by id ascending")
	void R_LINK_19_oldest_orders_by_created_at_asc_then_id_asc() {
		save("new", T0.plusSeconds(20));
		save("same-1", T0.plusSeconds(10));
		save("same-2", T0.plusSeconds(10));
		save("old", T0);

		Page<Link> page = linkRepository.findPage(LinkRepository.OLDEST_FIRST, 0, 10);

		assertThat(urls(page)).containsExactly("https://example.com/old", "https://example.com/same-1",
				"https://example.com/same-2", "https://example.com/new");
	}

	@Test
	@DisplayName("R-LINK-19: newest first keeps the existing R-LINK-09 order")
	void R_LINK_19_newest_matches_existing_order() {
		save("old", T0);
		save("same-1", T0.plusSeconds(10));
		save("same-2", T0.plusSeconds(10));
		save("new", T0.plusSeconds(20));

		Page<Link> page = linkRepository.findPage(LinkRepository.NEWEST_FIRST, 0, 10);

		assertThat(urls(page)).containsExactly("https://example.com/new", "https://example.com/same-2",
				"https://example.com/same-1", "https://example.com/old");
	}

	@Test
	@DisplayName("R-LINK-20: walking every oldest-first page returns each link exactly once")
	void R_LINK_20_oldest_pages_without_overlap_or_gap() {
		for (int i = 0; i < 5; i++) {
			save("p" + i, T0.plusSeconds(i % 2));
		}

		List<String> seen = new ArrayList<>();
		for (int p = 0; p < 3; p++) {
			seen.addAll(urls(linkRepository.findPage(LinkRepository.OLDEST_FIRST, p, 2)));
		}

		assertThat(seen).hasSize(5);
		assertThat(new HashSet<>(seen)).hasSize(5);
		assertThat(seen).containsExactly("https://example.com/p0", "https://example.com/p2", "https://example.com/p4",
				"https://example.com/p1", "https://example.com/p3");
		assertThat(linkRepository.findPage(LinkRepository.OLDEST_FIRST, 3, 2).getContent()).isEmpty();
	}

	@Test
	@DisplayName("R-LINK-20: with a tag, filtered links are sorted oldest first and only they are counted")
	void R_LINK_20_oldest_with_tag_filters_then_sorts_and_counts_filtered_only() {
		save("java-new", List.of("Java"), T0.plusSeconds(30));
		save("other", List.of("spring"), T0);
		save("java-old", List.of("java"), T0.plusSeconds(10));
		save("java-mid", List.of("JAVA", "spring"), T0.plusSeconds(20));

		Page<Link> first = linkRepository.findPageByTag("java", LinkRepository.OLDEST_FIRST, 0, 2);
		Page<Link> second = linkRepository.findPageByTag("java", LinkRepository.OLDEST_FIRST, 1, 2);
		Page<Link> newest = linkRepository.findPageByTag("java", LinkRepository.NEWEST_FIRST, 0, 2);

		assertThat(urls(first)).containsExactly("https://example.com/java-old", "https://example.com/java-mid");
		assertThat(urls(second)).containsExactly("https://example.com/java-new");
		assertThat(first.getTotalElements()).isEqualTo(3);
		assertThat(first.getTotalPages()).isEqualTo(2);
		assertThat(first.hasNext()).isTrue();
		assertThat(newest.getTotalElements()).isEqualTo(first.getTotalElements());
		assertThat(newest.getTotalPages()).isEqualTo(first.getTotalPages());
	}

}
