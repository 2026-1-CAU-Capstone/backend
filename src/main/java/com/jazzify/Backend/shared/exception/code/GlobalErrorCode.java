package com.jazzify.backend.shared.exception.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

@Getter
@NullMarked
@AllArgsConstructor
public enum GlobalErrorCode implements BaseErrorCode {

    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "GLOBAL_001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "GLOBAL_002", "잘못된 입력값입니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "GLOBAL_003", "요청한 리소스를 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "GLOBAL_004", "허용되지 않은 HTTP 메서드입니다."),
    ACCESS_DENIED(HttpStatus.FORBIDDEN, "GLOBAL_005", "접근 권한이 없습니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "GLOBAL_006", "인증이 필요합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
