package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;

import dev.linkshelf.link.validation.LinkUrlValidator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LinkUrlValidatorTest {

	private final LinkUrlValidator validator = new LinkUrlValidator();

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { "  ", "not a url", "http://", "ftp://example.com", "mailto:a@b.c", "https:///nohost",
			"https://한국.kr/a", "example.com/a" })
	@DisplayName("R-LINK-02: missing, malformed, non-http(s), host-less and IDN URLs are rejected")
	void R_LINK_02_rejects_invalid_urls(String url) {
		assertThat(validator.isValid(url, null)).isFalse();
	}

	@Test
	@DisplayName("R-LINK-02: URL length limit is 2,048 code points")
	void R_LINK_02_url_length_boundary() {
		String prefix = "https://example.com/";
		String atLimit = prefix + "😀".repeat(2048 - prefix.length());
		assertThat(Texts.codePoints(atLimit)).isEqualTo(2048);

		assertThat(validator.isValid(atLimit, null)).isTrue();
		assertThat(validator.isValid(atLimit + "a", null)).isFalse();
		assertThat(validator.isValid("  " + atLimit + "  ", null)).isTrue();
	}

	@Test
	@DisplayName("R-LINK-02: punycode host is accepted (spec A1)")
	void R_LINK_02_accepts_punycode_host() {
		assertThat(validator.isValid("https://xn--3e0b707e.kr/a", null)).isTrue();
		assertThat(validator.isValid("HTTP://Example.COM:8080/a?b=1#c", null)).isTrue();
	}

}
