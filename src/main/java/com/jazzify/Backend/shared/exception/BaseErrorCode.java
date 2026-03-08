package com.jazzify.backend.shared.exception;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

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
