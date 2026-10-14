package dev.linkshelf.link;

import java.sql.SQLException;
import java.time.Instant;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LinkService {

	private static final String UNIQUE_VIOLATION_SQL_STATE = "23505";

	private final LinkRepository linkRepository;

	public LinkService(LinkRepository linkRepository) {
		this.linkRepository = linkRepository;
	}

	/**
	 * Saves a link. Duplicates are caught by a pre-check, and by the unique constraint when concurrent saves both pass
	 * the pre-check (research R3); {@code saveAndFlush} makes the violation surface here rather than at commit.
	 */
	@Transactional
	public LinkResponse save(SaveLinkRequest request) {
		String normalizedUrl = UrlNormalizer.normalize(request.url());
		if (linkRepository.existsByNormalizedUrl(normalizedUrl)) {
			throw new DuplicateLinkException(normalizedUrl);
		}
		Link link = new Link(normalizedUrl, Texts.stripToNull(request.memo()), TagNormalizer.normalize(request.tags()),
				Instant.now());
		try {
			return LinkResponse.from(linkRepository.saveAndFlush(link));
		}
		catch (DataIntegrityViolationException ex) {
			if (isNormalizedUrlViolation(ex)) {
				throw new DuplicateLinkException(normalizedUrl, ex);
			}
			throw ex;
		}
	}

	/** Builds responses inside the transaction because open-in-view is off (#5). */
	@Transactional(readOnly = true)
	public LinkPageResponse list(int page, int size, String tag) {
		Page<Link> links = tag == null ? linkRepository.findNewestFirst(page, size)
				: linkRepository.findNewestFirstByTag(tag.strip().toLowerCase(Locale.ROOT), page, size);
		return LinkPageResponse.from(links.map(LinkResponse::from));
	}

	private static boolean isNormalizedUrlViolation(Throwable ex) {
		for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
			if (cause instanceof SQLException sqlException
					&& UNIQUE_VIOLATION_SQL_STATE.equals(sqlException.getSQLState()) && sqlException.getMessage() != null
					&& sqlException.getMessage()
						.toLowerCase(Locale.ROOT)
						.contains(Link.NORMALIZED_URL_CONSTRAINT)) {
				return true;
			}
		}
		return false;
	}

}
