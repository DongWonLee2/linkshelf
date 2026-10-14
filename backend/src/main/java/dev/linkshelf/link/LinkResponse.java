package dev.linkshelf.link;

import java.time.Instant;
import java.util.List;

public record LinkResponse(Long id, String url, String memo, List<String> tags, Instant createdAt, boolean read) {

	static LinkResponse from(Link link) {
		return new LinkResponse(link.getId(), link.getNormalizedUrl(), link.getMemo(), link.getTags(),
				link.getCreatedAt(), link.isRead());
	}

}
