package dev.linkshelf.link.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * Required absolute http/https URL with a host, at most 2,048 code points after stripping (R-LINK-02). Hosts that
 * {@link java.net.URI} cannot parse, such as non-punycode IDN, are rejected (spec A1).
 */
@Documented
@Constraint(validatedBy = LinkUrlValidator.class)
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface LinkUrl {

	String message() default "호스트가 있는 http 또는 https URL이어야 하며 2048자 이하여야 합니다";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
