package com.jazzify.backend.shared.exception.code;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

import com.jazzify.backend.shared.exception.CustomException;

@NullMarked
public interface BaseErrorCode {

	HttpStatus getHttpStatus();

	String getCode();

	String getMessage();

	default CustomException toException() {
		return new CustomException(this);
	}

	default CustomException toException(String detail) {
		return new CustomException(this, detail);
	}
}
