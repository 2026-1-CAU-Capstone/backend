package com.jazzify.backend.shared.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.jazzify.backend.shared.exception.CustomException;
import com.jazzify.backend.shared.exception.code.BaseErrorCode;

import lombok.Builder;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Getter
@Builder
@NullMarked
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private final String code;
    private final String message;
    private final @Nullable String detail;

    public static ErrorResponse of(BaseErrorCode errorCode) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .build();
    }

    public static ErrorResponse of(BaseErrorCode errorCode, @Nullable String detail) {
        return ErrorResponse.builder()
                .code(errorCode.getCode())
                .message(errorCode.getMessage())
                .detail(detail)
                .build();
    }

    public static ErrorResponse of(CustomException e) {
        return ErrorResponse.builder()
                .code(e.getCode())
                .message(e.getErrorCode().getMessage())
                .detail(e.getDetail())
                .build();
    }
}
