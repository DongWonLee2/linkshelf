package dev.linkshelf.link.validation;

import java.util.List;

import dev.linkshelf.link.TagNormalizer;
import dev.linkshelf.link.Texts;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class LinkTagsValidator implements ConstraintValidator<LinkTags, List<String>> {

	private int maxCount;

	private int maxLength;

	@Override
	public void initialize(LinkTags constraint) {
		this.maxCount = constraint.maxCount();
		this.maxLength = constraint.maxLength();
	}

	@Override
	public boolean isValid(List<String> tags, ConstraintValidatorContext context) {
		if (tags == null) {
			return true;
		}
		for (String tag : tags) {
			String stripped = Texts.stripToNull(tag);
			if (stripped == null || Texts.codePoints(stripped) > maxLength) {
				return false;
			}
		}
		return TagNormalizer.normalize(tags).size() <= maxCount;
	}

}
