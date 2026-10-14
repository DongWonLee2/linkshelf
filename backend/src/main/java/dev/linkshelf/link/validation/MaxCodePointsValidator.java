package dev.linkshelf.link.validation;

import dev.linkshelf.link.Texts;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MaxCodePointsValidator implements ConstraintValidator<MaxCodePoints, String> {

	private int max;

	@Override
	public void initialize(MaxCodePoints constraint) {
		this.max = constraint.value();
	}

	@Override
	public boolean isValid(String value, ConstraintValidatorContext context) {
		String stripped = Texts.stripToNull(value);
		return stripped == null || Texts.codePoints(stripped) <= max;
	}

}
