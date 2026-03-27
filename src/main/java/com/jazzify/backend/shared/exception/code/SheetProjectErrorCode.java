package com.jazzify.backend.shared.exception.code;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@NullMarked
@AllArgsConstructor
public enum SheetProjectErrorCode implements BaseErrorCode {

	SHEET_PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "SHEET_PROJECT_001", "악보 프로젝트를 찾을 수 없습니다."),
	SHEET_PROJECT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "SHEET_PROJECT_002", "해당 악보 프로젝트에 대한 접근 권한이 없습니다."),
	SHEET_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "SHEET_PROJECT_003", "악보 파일을 찾을 수 없습니다."),
	UNSUPPORTED_FILE_TYPE(HttpStatus.BAD_REQUEST, "SHEET_PROJECT_004", "지원하지 않는 파일 형식입니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}

