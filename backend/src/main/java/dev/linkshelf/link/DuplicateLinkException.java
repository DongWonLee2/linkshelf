package dev.linkshelf.link;

/** The normalized URL is already saved (R-LINK-04). Mapped to 409 by the API exception handler. */
public class DuplicateLinkException extends RuntimeException {

	private final String normalizedUrl;

	public DuplicateLinkException(String normalizedUrl) {
		super("Link already saved: " + normalizedUrl);
		this.normalizedUrl = normalizedUrl;
	}

	public DuplicateLinkException(String normalizedUrl, Throwable cause) {
		super("Link already saved: " + normalizedUrl, cause);
		this.normalizedUrl = normalizedUrl;
	}

	public String getNormalizedUrl() {
		return normalizedUrl;
	}

}
