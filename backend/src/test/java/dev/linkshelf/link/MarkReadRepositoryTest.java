package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

@DataJpaTest
class MarkReadRepositoryTest {

	@Autowired
	private LinkRepository linkRepository;

	@Autowired
	private EntityManager entityManager;

	private Link reload(Long id) {
		entityManager.flush();
		entityManager.clear();
		return linkRepository.findById(id).orElseThrow();
	}

	@Test
	@DisplayName("R-LINK-16: a new link is unread")
	void R_LINK_16_new_link_is_unread() {
		Link saved = linkRepository.saveAndFlush(new Link("https://example.com/a", null, List.of(), Instant.now()));

		assertThat(reload(saved.getId()).isRead()).isFalse();
	}

	@Test
	@DisplayName("R-LINK-15: the read state persists and can be set back")
	void R_LINK_15_read_state_persists() {
		Long id = linkRepository.saveAndFlush(new Link("https://example.com/a", null, List.of(), Instant.now())).getId();

		reload(id).markRead(true);
		assertThat(reload(id).isRead()).isTrue();

		reload(id).markRead(false);
		assertThat(reload(id).isRead()).isFalse();
	}

}
