package com.jazzify.backend.shared.exception.code;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@NullMarked
@AllArgsConstructor
public enum LickErrorCode implements BaseErrorCode {

	LICK_NOT_FOUND(HttpStatus.NOT_FOUND, "LICK_001", "릭을 찾을 수 없습니다."),
	LICK_DUPLICATE(HttpStatus.CONFLICT, "LICK_002", "동일한 제목과 연주자의 릭이 이미 존재합니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}

