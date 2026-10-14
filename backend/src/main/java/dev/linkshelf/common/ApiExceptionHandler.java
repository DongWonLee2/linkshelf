package dev.linkshelf.common;

import java.util.List;

import dev.linkshelf.link.DuplicateLinkException;
import dev.linkshelf.link.LinkNotFoundException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * RFC 9457 problem responses (ADR-0001). Validation failures add an {@code errors} list with every invalid field;
 * a duplicate link adds the conflicting normalized {@code url}. Invalid query parameters use the same {@code errors}
 * list (contracts/browse-links.md).
 */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		ProblemDetail body = ex.getBody();
		List<FieldErrorResponse> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
			.toList();
		body.setProperty("errors", errors);
		return handleExceptionInternal(ex, body, headers, status, request);
	}

	@Override
	protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<FieldErrorResponse> errors = ex.getParameterValidationResults()
			.stream()
			.flatMap(result -> result.getResolvableErrors()
				.stream()
				.map(error -> new FieldErrorResponse(result.getMethodParameter().getParameterName(),
						error.getDefaultMessage())))
			.toList();
		return handleExceptionInternal(ex, invalidParameters(errors), headers, HttpStatus.BAD_REQUEST, request);
	}

	/**
	 * A value that cannot be converted is reported alone: conversion fails before validation runs, so other parameters
	 * are not checked (approved exception B5 to R-LINK-11).
	 */
	@Override
	protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		String field = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName()
				: ex.getPropertyName();
		return handleExceptionInternal(ex, invalidParameters(List.of(new FieldErrorResponse(field, "숫자여야 합니다"))),
				headers, HttpStatus.BAD_REQUEST, request);
	}

	private static ProblemDetail invalidParameters(List<FieldErrorResponse> errors) {
		ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request parameters.");
		body.setProperty("errors", errors);
		return body;
	}

	@ExceptionHandler(DuplicateLinkException.class)
	ProblemDetail handleDuplicateLink(DuplicateLinkException ex) {
		ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "이미 저장된 URL입니다");
		body.setProperty("url", ex.getNormalizedUrl());
		return body;
	}

	@ExceptionHandler(LinkNotFoundException.class)
	ProblemDetail handleLinkNotFound(LinkNotFoundException ex) {
		ProblemDetail body = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, "링크를 찾을 수 없습니다");
		body.setProperty("id", ex.getId());
		return body;
	}

	record FieldErrorResponse(String field, String message) {
	}

}
