package com.jazzify.backend.shared.exception.code;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@NullMarked
@AllArgsConstructor
public enum RagErrorCode implements BaseErrorCode {

	RAG_NOT_ENABLED(HttpStatus.SERVICE_UNAVAILABLE, "RAG_001", "RAG 기능이 활성화되어 있지 않습니다."),
	RAG_EMBEDDING_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "RAG_002", "RAG 임베딩 서버 설정이 누락되었습니다."),
	RAG_BOOTSTRAP_PATH_INVALID(HttpStatus.BAD_REQUEST, "RAG_004", "RAG 부트스트랩 데이터 경로가 올바르지 않습니다."),
	RAG_BOOTSTRAP_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "RAG_005", "RAG 문서 초기화에 실패했습니다."),
	RAG_SEARCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "RAG_006", "RAG 검색에 실패했습니다."),
	RAG_EMBEDDING_REQUEST_FAILED(HttpStatus.BAD_GATEWAY, "RAG_008", "외부 임베딩 요청에 실패했습니다."),
	RAG_SCHEMA_INITIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "RAG_010", "RAG PostgreSQL 스키마 초기화에 실패했습니다."),
	RAG_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "RAG_011", "RAG 문서를 찾을 수 없습니다."),
	RAG_DUPLICATE_DOCUMENT_SLUG(HttpStatus.CONFLICT, "RAG_012", "이미 사용 중인 RAG 문서 slug입니다."),
	RAG_INVALID_SOURCE_TYPE(HttpStatus.BAD_REQUEST, "RAG_013", "지원하지 않는 RAG 문서 sourceType입니다."),
	RAG_INVALID_DOCUMENT_CONTENT(HttpStatus.BAD_REQUEST, "RAG_014", "RAG 문서 content에서 검색 가능한 섹션을 추출할 수 없습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}



