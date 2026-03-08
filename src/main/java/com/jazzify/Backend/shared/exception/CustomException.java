package com.jazzify.backend.shared.exception;

import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;

import com.jazzify.backend.shared.exception.code.BaseErrorCode;

@Getter
@NullMarked
public class CustomException extends RuntimeException {

    private final BaseErrorCode errorCode;
    private final @Nullable String detail;

    public CustomException(BaseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.detail = null;
    }

    public CustomException(BaseErrorCode errorCode, String detail) {
        super(errorCode.getMessage() + " - " + detail);
        this.errorCode = errorCode;
        this.detail = detail;
    }

    public HttpStatus getHttpStatus() {
        return errorCode.getHttpStatus();
    }

    public String getCode() {
        return errorCode.getCode();
    }
}
