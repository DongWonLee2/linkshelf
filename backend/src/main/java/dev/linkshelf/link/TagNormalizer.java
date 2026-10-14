package dev.linkshelf.link;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Normalizes tags (R-LINK-05, spec Q5): strip each tag and merge tags that differ only in case, keeping the first
 * spelling and first-seen order.
 */
public final class TagNormalizer {

	private TagNormalizer() {
	}

	/** Assumes tags already passed {@code @LinkTags} validation (no null or blank elements). */
	public static List<String> normalize(List<String> tags) {
		if (tags == null) {
			return List.of();
		}
		Map<String, String> byKey = new LinkedHashMap<>();
		for (String tag : tags) {
			String stripped = tag.strip();
			byKey.putIfAbsent(stripped.toLowerCase(Locale.ROOT), stripped);
		}
		return new ArrayList<>(byKey.values());
	}

}
