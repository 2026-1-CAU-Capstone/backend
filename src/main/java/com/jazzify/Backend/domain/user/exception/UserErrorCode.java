package com.jazzify.backend.domain.user.exception;

import com.jazzify.backend.shared.exception.BaseErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

@Getter
@NullMarked
@AllArgsConstructor
public enum UserErrorCode implements BaseErrorCode {

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_USERNAME(HttpStatus.CONFLICT, "USER_002", "이미 사용 중인 사용자명입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "USER_003", "비밀번호가 일치하지 않습니다."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "USER_004", "유효하지 않은 리프레시 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "USER_005", "리프레시 토큰이 존재하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
