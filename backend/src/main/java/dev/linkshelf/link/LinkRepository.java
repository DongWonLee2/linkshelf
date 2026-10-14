package dev.linkshelf.link;

import java.util.List;
import java.util.function.Function;
import java.util.function.LongSupplier;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LinkRepository extends JpaRepository<Link, Long> {

	/** R-LINK-09: newest first; links saved at the same instant keep a stable order by id. */
	Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

	boolean existsByNormalizedUrl(String normalizedUrl);

	/**
	 * A page whose offset does not fit in an int lies past the last link, so it is answered as an empty page instead
	 * of letting the query fail (PR #16 review).
	 */
	default Page<Link> findNewestFirst(int page, int size) {
		return newestFirst(page, size, this::findAll, this::count);
	}

	/** R-LINK-13: {@code tag} must already be stripped and lower-cased with {@code Locale.ROOT} (research R2). */
	default Page<Link> findNewestFirstByTag(String tag, int page, int size) {
		return newestFirst(page, size, request -> findByTag(tag, request), () -> countByTag(tag));
	}

	@Query("select l from Link l where exists (select 1 from l.tags t where lower(t) = :tag)")
	Page<Link> findByTag(@Param("tag") String tag, Pageable pageable);

	@Query("select count(l) from Link l where exists (select 1 from l.tags t where lower(t) = :tag)")
	long countByTag(@Param("tag") String tag);

	/** The total is counted separately only on the huge-page path; otherwise the paged query counts it. */
	private static Page<Link> newestFirst(int page, int size, Function<PageRequest, Page<Link>> query,
			LongSupplier total) {
		PageRequest request = PageRequest.of(page, size, NEWEST_FIRST);
		if (request.getOffset() > Integer.MAX_VALUE) {
			return new PageImpl<>(List.of(), request, total.getAsLong());
		}
		return query.apply(request);
	}

}
