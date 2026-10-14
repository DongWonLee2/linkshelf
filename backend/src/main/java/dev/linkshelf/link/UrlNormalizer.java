package dev.linkshelf.link;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

/**
 * Normalizes a URL to its standard-equivalent form (R-LINK-04, spec Q2): lower-case scheme and host, drop the
 * scheme's default port. Path, query, fragment and percent-encoding are kept as-is, so the string is rebuilt from
 * raw components instead of the multi-argument {@link URI} constructors, which re-encode {@code %}.
 */
public final class UrlNormalizer {

	private UrlNormalizer() {
	}

	/** Assumes {@code url} already passed {@code @LinkUrl} validation. */
	public static String normalize(String url) {
		URI uri = parse(url.strip());
		String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
		StringBuilder normalized = new StringBuilder(scheme).append("://");
		if (uri.getRawUserInfo() != null) {
			normalized.append(uri.getRawUserInfo()).append('@');
		}
		normalized.append(uri.getHost().toLowerCase(Locale.ROOT));
		if (uri.getPort() != -1 && uri.getPort() != defaultPort(scheme)) {
			normalized.append(':').append(uri.getPort());
		}
		normalized.append(uri.getRawPath());
		if (uri.getRawQuery() != null) {
			normalized.append('?').append(uri.getRawQuery());
		}
		if (uri.getRawFragment() != null) {
			normalized.append('#').append(uri.getRawFragment());
		}
		return normalized.toString();
	}

	static URI parse(String url) {
		try {
			return new URI(url);
		}
		catch (URISyntaxException ex) {
			throw new IllegalArgumentException("Not a valid URI: " + url, ex);
		}
	}

	private static int defaultPort(String scheme) {
		return switch (scheme) {
			case "http" -> 80;
			case "https" -> 443;
			default -> -1;
		};
	}

}
