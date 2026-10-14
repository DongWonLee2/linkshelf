package dev.linkshelf.link;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LinkRepository extends JpaRepository<Link, Long> {

	/** R-LINK-09: newest first; links saved at the same instant keep a stable order by id. */
	Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

	boolean existsByNormalizedUrl(String normalizedUrl);

	/**
	 * A page whose offset does not fit in an int lies past the last link, so it is answered as an empty page instead
	 * of letting the query fail (PR #16 review).
	 */
	default Page<Link> findNewestFirst(int page, int size) {
		PageRequest request = PageRequest.of(page, size, NEWEST_FIRST);
		if (request.getOffset() > Integer.MAX_VALUE) {
			return new PageImpl<>(List.of(), request, count());
		}
		return findAll(request);
	}

}
