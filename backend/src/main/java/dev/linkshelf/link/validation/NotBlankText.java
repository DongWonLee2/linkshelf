package dev.linkshelf.link.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Optional text that, when present, is not blank after stripping (R-LINK-14). Uses the same whitespace rule as the
 * save API ({@code String.strip()}), unlike {@code @NotBlank}, which also rejects null.
 */
@Documented
@Constraint(validatedBy = NotBlankTextValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface NotBlankText {

	String message() default "공백만 있을 수 없습니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
