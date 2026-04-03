package com.jazzify.backend.shared.exception.code;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@NullMarked
@AllArgsConstructor
public enum ChordProjectErrorCode implements BaseErrorCode {

	CHORD_PROJECT_NOT_FOUND(HttpStatus.NOT_FOUND, "CHORD_PROJECT_001", "코드 프로젝트를 찾을 수 없습니다."),
	CHORD_PROJECT_ACCESS_DENIED(HttpStatus.FORBIDDEN, "CHORD_PROJECT_002", "해당 코드 프로젝트에 대한 접근 권한이 없습니다."),
	CHORD_PROJECT_NO_CHORDS(HttpStatus.BAD_REQUEST, "CHORD_PROJECT_003", "분석할 코드 정보가 없습니다. 먼저 코드를 등록해 주세요."),
	INVALID_TIME_SIGNATURE(HttpStatus.BAD_REQUEST, "CHORD_PROJECT_004", "유효하지 않은 박자표입니다."),
	CHORD_PROJECT_NOT_ANALYZED(HttpStatus.BAD_REQUEST, "CHORD_PROJECT_005", "분석 결과가 없습니다. 먼저 분석을 실행해 주세요.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}

