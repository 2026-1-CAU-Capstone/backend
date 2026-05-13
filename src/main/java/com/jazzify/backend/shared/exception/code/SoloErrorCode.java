package com.jazzify.backend.shared.exception.code;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@NullMarked
@AllArgsConstructor
public enum SoloErrorCode implements BaseErrorCode {

	SOLO_NOT_FOUND(HttpStatus.NOT_FOUND, "SOLO_001", "솔로를 찾을 수 없습니다."),
	SOLO_DUPLICATE(HttpStatus.CONFLICT, "SOLO_002", "동일한 제목과 연주자의 솔로가 이미 존재합니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}


