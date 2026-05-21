package com.jazzify.backend.shared.exception.code;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@NullMarked
@AllArgsConstructor
public enum ChatErrorCode implements BaseErrorCode {

	CHAT_NOT_FOUND(HttpStatus.NOT_FOUND, "CHAT_001", "채팅을 찾을 수 없습니다."),
	CHAT_TYPE_MISMATCH(HttpStatus.BAD_REQUEST, "CHAT_002", "요청한 채팅 유형이 기존 대화와 일치하지 않습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}

