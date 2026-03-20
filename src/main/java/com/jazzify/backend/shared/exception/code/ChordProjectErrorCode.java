package com.jazzify.backend.shared.exception.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

@Getter
@NullMarked
@AllArgsConstructor
public enum ChordProjectErrorCode implements BaseErrorCode {

    CHORD_PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "CHORD_PROJECT_001", "코드 프로젝트를 찾을 수 없습니다."),
    CHORD_PROJECT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CHORD_PROJECT_002", "해당 코드 프로젝트에 대한 접근 권한이 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

