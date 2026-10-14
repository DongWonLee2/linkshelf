package dev.linkshelf.link.validation;

import java.net.URI;
import java.net.URISyntaxException;

import dev.linkshelf.link.Texts;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LinkUrlValidator implements ConstraintValidator<LinkUrl, String> {

	private static final int MAX_CODE_POINTS = 2048;

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		String url = Texts.stripToNull(value);
		if (url == null || Texts.codePoints(url) > MAX_CODE_POINTS) {
			return false;
		}
		URI uri;
		try {
			uri = new URI(url);
		}
		catch (URISyntaxException ex) {
			return false;
		}
		String scheme = uri.getScheme();
		return ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) && uri.getHost() != null;
	}

}
