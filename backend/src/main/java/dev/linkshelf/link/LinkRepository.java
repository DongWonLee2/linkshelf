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

	Sort OLDEST_FIRST = Sort.by(Sort.Order.asc("createdAt"), Sort.Order.asc("id"));

	boolean existsByNormalizedUrl(String normalizedUrl);

	/**
	 * A page whose offset does not fit in an int lies past the last link, so it is answered as an empty page instead
	 * of letting the query fail (PR #16 review).
	 */
	default Page<Link> findPage(Sort sort, int page, int size) {
		return sortedPage(sort, page, size, this::findAll, this::count);
	}

	/**
	 * R-LINK-13, R-LINK-23: a null {@code tag} does not filter by tag; otherwise it must already be stripped and
	 * lower-cased with {@code Locale.ROOT} (spec 002 research R2). {@code unreadOnly} keeps only unread links
	 * (R-LINK-22).
	 */
	default Page<Link> findPageFiltered(String tag, boolean unreadOnly, Sort sort, int page, int size) {
		return sortedPage(sort, page, size, request -> findFiltered(tag, unreadOnly, request),
				() -> countFiltered(tag, unreadOnly));
	}

	@Query("select l from Link l where (:tag is null or exists (select 1 from l.tags t where lower(t) = :tag))"
			+ " and (:unreadOnly = false or l.read = false)")
	Page<Link> findFiltered(@Param("tag") String tag, @Param("unreadOnly") boolean unreadOnly, Pageable pageable);

	@Query("select count(l) from Link l where (:tag is null or exists (select 1 from l.tags t where lower(t) = :tag))"
			+ " and (:unreadOnly = false or l.read = false)")
	long countFiltered(@Param("tag") String tag, @Param("unreadOnly") boolean unreadOnly);

	/** The total is counted separately only on the huge-page path; otherwise the paged query counts it. */
	private static Page<Link> sortedPage(Sort sort, int page, int size, Function<PageRequest, Page<Link>> query,
			LongSupplier total) {
		PageRequest request = PageRequest.of(page, size, sort);
		if (request.getOffset() > Integer.MAX_VALUE) {
			return new PageImpl<>(List.of(), request, total.getAsLong());
		}
		return query.apply(request);
	}

}
