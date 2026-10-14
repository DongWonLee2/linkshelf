package dev.linkshelf.link.validation;

import dev.linkshelf.link.Texts;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NotBlankTextValidator implements ConstraintValidator<NotBlankText, String> {

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		return value == null || Texts.stripToNull(value) != null;
	}

}
