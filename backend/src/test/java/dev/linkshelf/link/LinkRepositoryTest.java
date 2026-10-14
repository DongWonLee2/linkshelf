package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class LinkRepositoryTest {

	@Autowired
	private LinkRepository linkRepository;

	@Autowired
	private EntityManager entityManager;

	private Link saveAndReload(Link link) {
		Long id = linkRepository.saveAndFlush(link).getId();
		entityManager.clear();
		return linkRepository.findById(id).orElseThrow();
	}

	@Test
	@DisplayName("R-LINK-07: Hangul memo and tags round-trip unchanged, tag order kept")
	void R_LINK_07_round_trips_hangul_memo_and_tags() {
		Link reloaded = saveAndReload(new Link("https://example.com/a", "주말에 읽을 글 😀",
				List.of("스프링", "자바", "Java"), Instant.parse("2026-10-14T03:00:00Z")));

		assertThat(reloaded.getNormalizedUrl()).isEqualTo("https://example.com/a");
		assertThat(reloaded.getMemo()).isEqualTo("주말에 읽을 글 😀");
		assertThat(reloaded.getTags()).containsExactly("스프링", "자바", "Java");
		assertThat(reloaded.getCreatedAt()).isEqualTo(Instant.parse("2026-10-14T03:00:00Z"));
	}

	@Test
	@DisplayName("R-LINK-08: columns hold the code-point maximum even when every char is outside the BMP")
	void R_LINK_08_columns_fit_max_code_points_of_astral_chars() {
		String memo = "😀".repeat(500);
		String tag = "😀".repeat(30);
		String url = "https://example.com/" + "😀".repeat(2048 - "https://example.com/".length());
		assertThat(Texts.codePoints(memo)).isEqualTo(500);
		assertThat(memo.length()).isEqualTo(1000);
		assertThat(Texts.codePoints(url)).isEqualTo(2048);

		Link reloaded = saveAndReload(new Link(url, memo, Collections.nCopies(1, tag), Instant.now()));

		assertThat(reloaded.getMemo()).isEqualTo(memo);
		assertThat(reloaded.getTags()).containsExactly(tag);
		assertThat(reloaded.getNormalizedUrl()).isEqualTo(url);
	}

}
