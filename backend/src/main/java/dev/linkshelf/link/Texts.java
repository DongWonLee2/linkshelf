package dev.linkshelf.link;

/**
 * Text helpers shared by validation and normalization. Lengths are Unicode code points (R-LINK-08).
 */
public final class Texts {

	private Texts() {
	}

	public static int codePoints(String value) {
		return value.codePointCount(0, value.length());
	}

	/** Strips Unicode whitespace (including U+3000) and returns null when nothing is left. */
	public static String stripToNull(String value) {
		if (value == null) {
			return null;
		}
		String stripped = value.strip();
		return stripped.isEmpty() ? null : stripped;
	}

}
