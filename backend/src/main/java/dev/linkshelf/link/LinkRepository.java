package dev.linkshelf.link;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkRepository extends JpaRepository<Link, Long> {

	/** R-LINK-09: newest first; links saved at the same instant keep a stable order by id. */
	Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

	boolean existsByNormalizedUrl(String normalizedUrl);

	default Page<Link> findNewestFirst(int page, int size) {
		return findAll(PageRequest.of(page, size, NEWEST_FIRST));
	}

}
