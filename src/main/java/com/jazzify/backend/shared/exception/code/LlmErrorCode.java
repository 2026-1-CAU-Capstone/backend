package com.jazzify.backend.shared.exception.code;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@NullMarked
@AllArgsConstructor
public enum LlmErrorCode implements BaseErrorCode {

	LLM_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "LLM_001", "LLM 설정이 누락되었습니다."),
	LLM_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "LLM_002", "외부 LLM 요청에 실패했습니다."),
	LLM_STREAM_FAILED(HttpStatus.BAD_GATEWAY, "LLM_003", "LLM 스트리밍 응답 처리에 실패했습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}

