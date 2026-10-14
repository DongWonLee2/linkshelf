package dev.linkshelf.link;

import java.util.List;

import org.springframework.data.domain.Page;

/** One page of links (contracts/browse-links.md, approved 2026-10-14). */
public record LinkPageResponse(List<LinkResponse> items, int page, int size, long totalElements, int totalPages,
		boolean hasNext) {

	/** {@code hasNext} is computed in long: {@code Page.hasNext()} overflows when the page number is Integer.MAX_VALUE. */
	static LinkPageResponse from(Page<LinkResponse> page) {
		return new LinkPageResponse(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements(),
				page.getTotalPages(), (long) page.getNumber() + 1 < page.getTotalPages());
	}

}
