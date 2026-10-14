package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

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

	@Test
	@DisplayName("R-LINK-04: the DB unique constraint uk_link_normalized_url rejects the same normalized URL")
	void R_LINK_04_unique_constraint_rejects_same_normalized_url() {
		linkRepository.saveAndFlush(new Link("https://example.com/a", null, List.of(), Instant.now()));

		assertThatThrownBy(() -> linkRepository
			.saveAndFlush(new Link("https://example.com/a", null, List.of(), Instant.now())))
			.isInstanceOf(DataIntegrityViolationException.class)
			.satisfies(ex -> {
				SQLException sqlException = findCause(ex, SQLException.class);
				assertThat((Throwable) sqlException).isNotNull();
				assertThat(sqlException.getSQLState()).isEqualTo("23505");
				assertThat(sqlException.getMessage()).containsIgnoringCase("uk_link_normalized_url");
			});
	}

	private static <T extends Throwable> T findCause(Throwable ex, Class<T> type) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (type.isInstance(cause)) {
				return type.cast(cause);
			}
		}
		return null;
	}

}
