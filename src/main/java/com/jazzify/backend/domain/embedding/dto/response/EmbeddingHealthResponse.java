package com.jazzify.backend.domain.embedding.dto.response;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 임베딩 서버 헬스체크 응답.
 *
 * @param configured 서버 URL 설정 여부
 * @param serverUrl  설정된 서버 베이스 URL (미설정 시 null)
 * @param reachable  실제 HTTP 연결 가능 여부 (미설정이면 항상 false)
 */
@NullMarked
public record EmbeddingHealthResponse(
	boolean configured,
	@Nullable String serverUrl,
	boolean reachable
) {
}

