package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;

@DataJpaTest
class ListLinksRepositoryTest {

	private static final Instant T0 = Instant.parse("2026-10-14T03:00:00Z");

	@Autowired
	private LinkRepository linkRepository;

	private Link save(String path, Instant createdAt) {
		return linkRepository.saveAndFlush(new Link("https://example.com/" + path, null, List.of(), createdAt));
	}

	private static List<String> urls(Page<Link> page) {
		return page.getContent().stream().map(Link::getNormalizedUrl).toList();
	}

	@Test
	@DisplayName("R-LINK-09: links are ordered by created time descending, then by id descending")
	void R_LINK_09_orders_by_created_at_desc_then_id_desc() {
		save("old", T0);
		save("same-1", T0.plusSeconds(10));
		save("same-2", T0.plusSeconds(10));
		save("new", T0.plusSeconds(20));

		Page<Link> page = linkRepository.findNewestFirst(0, 10);

		assertThat(urls(page)).containsExactly("https://example.com/new", "https://example.com/same-2",
				"https://example.com/same-1", "https://example.com/old");
	}

	@Test
	@DisplayName("R-LINK-10: walking every page returns each link exactly once")
	void R_LINK_10_pages_without_overlap_or_gap() {
		for (int i = 0; i < 5; i++) {
			save("p" + i, T0.plusSeconds(i % 2));
		}

		List<String> seen = new ArrayList<>();
		for (int p = 0; p < 3; p++) {
			seen.addAll(urls(linkRepository.findNewestFirst(p, 2)));
		}

		assertThat(seen).hasSize(5);
		Set<String> distinct = new HashSet<>(seen);
		assertThat(distinct).hasSize(5);
		assertThat(linkRepository.findNewestFirst(3, 2).getContent()).isEmpty();
	}

	@Test
	@DisplayName("R-LINK-10: a page reports the total count and whether a next page exists")
	void R_LINK_10_counts_total() {
		for (int i = 0; i < 5; i++) {
			save("c" + i, T0.plusSeconds(i));
		}

		Page<Link> first = linkRepository.findNewestFirst(0, 2);
		Page<Link> last = linkRepository.findNewestFirst(2, 2);

		assertThat(first.getTotalElements()).isEqualTo(5);
		assertThat(first.getTotalPages()).isEqualTo(3);
		assertThat(first.hasNext()).isTrue();
		assertThat(last.hasNext()).isFalse();
	}

}
