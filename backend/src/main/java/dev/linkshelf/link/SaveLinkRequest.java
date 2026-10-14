package dev.linkshelf.link;

import java.util.List;

import dev.linkshelf.link.validation.LinkTags;
import dev.linkshelf.link.validation.LinkUrl;
import dev.linkshelf.link.validation.MaxCodePoints;

public record SaveLinkRequest(@LinkUrl String url, @MaxCodePoints(500) String memo,
		@LinkTags(maxCount = 10, maxLength = 30) List<String> tags) {

}
