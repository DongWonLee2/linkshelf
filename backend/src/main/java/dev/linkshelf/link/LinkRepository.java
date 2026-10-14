package dev.linkshelf.link;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkRepository extends JpaRepository<Link, Long> {

	boolean existsByNormalizedUrl(String normalizedUrl);

}
