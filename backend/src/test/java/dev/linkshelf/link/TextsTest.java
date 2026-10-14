package dev.linkshelf.link;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TextsTest {

	@Test
	@DisplayName("R-LINK-08: length is counted in Unicode code points")
	void R_LINK_08_counts_code_points() {
		assertThat(Texts.codePoints("가")).isEqualTo(1);
		assertThat(Texts.codePoints("😀")).isEqualTo(1);
		assertThat(Texts.codePoints("👍🏽")).isEqualTo(2);
		assertThat(Texts.codePoints("가😀👍🏽")).isEqualTo(4);
	}

	@Test
	@DisplayName("R-LINK-08: stripToNull removes Unicode whitespace and maps blank to null")
	void R_LINK_08_strip_to_null_handles_unicode_whitespace() {
		assertThat(Texts.stripToNull(null)).isNull();
		assertThat(Texts.stripToNull("   ")).isNull();
		assertThat(Texts.stripToNull("　")).isNull();
		assertThat(Texts.stripToNull(" 　메모　 ")).isEqualTo("메모");
	}

}
