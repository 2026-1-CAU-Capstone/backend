package com.jazzify.backend.shared.exception.code;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Jazzify Embedding Worker 서버 연동 관련 에러 코드.
 */
@Getter
@NullMarked
@AllArgsConstructor
public enum EmbeddingErrorCode implements BaseErrorCode {

	EMBEDDING_SERVER_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "EMB_001", "임베딩 서버 주소가 설정되지 않았습니다."),
	EMBEDDING_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "EMB_002", "임베딩 서버 요청에 실패했습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}

