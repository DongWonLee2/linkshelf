package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class UrlNormalizerTest {

	@Test
	@DisplayName("R-LINK-04: scheme and host are lower-cased, surrounding whitespace removed")
	void R_LINK_04_lowercases_scheme_and_host() {
		assertThat(UrlNormalizer.normalize("HTTPS://Example.COM/a")).isEqualTo("https://example.com/a");
		assertThat(UrlNormalizer.normalize("  http://WWW.Example.com/a 　")).isEqualTo("http://www.example.com/a");
	}

	@Test
	@DisplayName("R-LINK-04: only the scheme's default port is removed")
	void R_LINK_04_removes_only_default_port() {
		assertThat(UrlNormalizer.normalize("http://example.com:80/a")).isEqualTo("http://example.com/a");
		assertThat(UrlNormalizer.normalize("HTTPS://Example.COM:443/a")).isEqualTo("https://example.com/a");
		assertThat(UrlNormalizer.normalize("http://example.com:443/a")).isEqualTo("http://example.com:443/a");
		assertThat(UrlNormalizer.normalize("https://example.com:8443/a")).isEqualTo("https://example.com:8443/a");
	}

	@Test
	@DisplayName("R-LINK-04: path, query, fragment, trailing slash and percent-encoding are kept as-is")
	void R_LINK_04_keeps_path_query_fragment_and_trailing_slash() {
		assertThat(UrlNormalizer.normalize("https://example.com/A/")).isEqualTo("https://example.com/A/");
		assertThat(UrlNormalizer.normalize("https://example.com/a?b=1&C=2#x")).isEqualTo("https://example.com/a?b=1&C=2#x");
		assertThat(UrlNormalizer.normalize("https://example.com/a%20b")).isEqualTo("https://example.com/a%20b");
		assertThat(UrlNormalizer.normalize("https://example.com/경로/😀")).isEqualTo("https://example.com/경로/😀");
		assertThat(UrlNormalizer.normalize("http://User@Example.com/")).isEqualTo("http://User@example.com/");
	}

	@Test
	@DisplayName("R-LINK-04: an empty path becomes \"/\" (standard-equivalent for http/https)")
	void R_LINK_04_treats_empty_path_as_slash() {
		assertThat(UrlNormalizer.normalize("https://example.com")).isEqualTo("https://example.com/");
		assertThat(UrlNormalizer.normalize("HTTP://Example.COM:80")).isEqualTo("http://example.com/");
		assertThat(UrlNormalizer.normalize("https://example.com?x=1")).isEqualTo("https://example.com/?x=1");
		assertThat(UrlNormalizer.normalize("https://example.com#top")).isEqualTo("https://example.com/#top");
	}

}
