package dev.linkshelf.link.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Optional tag list (R-LINK-05): no null or blank element, each at most {@link #maxLength()} code points after
 * stripping, and at most {@link #maxCount()} tags after merging case-insensitive duplicates.
 */
@Documented
@Constraint(validatedBy = LinkTagsValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LinkTags {

	int maxCount();

	int maxLength();

	String message() default "태그는 비어 있지 않은 {maxLength}자 이하여야 하며, 중복을 합친 뒤 {maxCount}개 이하여야 합니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
