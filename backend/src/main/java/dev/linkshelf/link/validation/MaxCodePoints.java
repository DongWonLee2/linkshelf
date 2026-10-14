package dev.linkshelf.link.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Optional text of at most {@link #value()} Unicode code points after stripping (R-LINK-03, R-LINK-08). Null or blank
 * is valid. Unlike {@code @Size}, an emoji outside the BMP counts as one.
 */
@Documented
@Constraint(validatedBy = MaxCodePointsValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface MaxCodePoints {

	int value();

	String message() default "{value}자 이하여야 합니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
