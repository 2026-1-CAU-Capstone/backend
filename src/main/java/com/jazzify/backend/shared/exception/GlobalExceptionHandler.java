package com.jazzify.backend.shared.exception;

import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.jazzify.backend.shared.exception.code.GlobalErrorCode;
import com.jazzify.backend.shared.web.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NullMarked
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(CustomException.class)
	public ErrorResponse handleCustomException(CustomException e, HttpServletResponse response) {
		log.warn("CustomException: {} - {}", e.getCode(), e.getMessage());
		response.setStatus(e.getHttpStatus().value());
		return ErrorResponse.of(e);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public ErrorResponse handleValidationException(MethodArgumentNotValidException e) {
		String detail = e.getBindingResult().getFieldErrors().stream()
			.map(error -> error.getField() + ": " + error.getDefaultMessage())
			.collect(Collectors.joining(", "));
		log.warn("Validation failed: {}", detail);
		return ErrorResponse.of(GlobalErrorCode.INVALID_INPUT, detail);
	}

	@ExceptionHandler(NoSuchElementException.class)
	@ResponseStatus(HttpStatus.NOT_FOUND)
	public ErrorResponse handleNoSuchElementException(NoSuchElementException e) {
		log.warn("NoSuchElementException: {}", e.getMessage());
		return ErrorResponse.of(GlobalErrorCode.RESOURCE_NOT_FOUND, e.getMessage());
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	@ResponseStatus(HttpStatus.METHOD_NOT_ALLOWED)
	public ErrorResponse handleMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
		log.warn("Method not allowed: {}", e.getMessage());
		return ErrorResponse.of(GlobalErrorCode.METHOD_NOT_ALLOWED, e.getMessage());
	}

	@ExceptionHandler(AccessDeniedException.class)
	@ResponseStatus(HttpStatus.FORBIDDEN)
	public ErrorResponse handleAccessDeniedException(AccessDeniedException e) {
		log.warn("Access denied: {}", e.getMessage());
		return ErrorResponse.of(GlobalErrorCode.ACCESS_DENIED);
	}

	@ExceptionHandler(AuthenticationException.class)
	@ResponseStatus(HttpStatus.UNAUTHORIZED)
	public ErrorResponse handleAuthenticationException(AuthenticationException e) {
		log.warn("Authentication failed: {}", e.getMessage());
		return ErrorResponse.of(GlobalErrorCode.UNAUTHORIZED, e.getMessage());
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public ErrorResponse handleException(Exception e, HttpServletRequest request) {
		log.error("Unhandled exception at {}: ", request.getRequestURI(), e);
		return ErrorResponse.of(GlobalErrorCode.INTERNAL_SERVER_ERROR);
	}
}

