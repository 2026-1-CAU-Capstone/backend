package com.jazzify.backend.shared.embedding;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Jazzify Embedding Worker 서버 연동 설정.
 * <p>
 * {@code application.yml}에서 {@code embedding.*} 키로 주입된다.
 *
 * <pre>
 * embedding:
 *   server-url: ${EMBEDDING_SERVER_URL:}   # 임베딩 서버 베이스 URL (예: http://127.0.0.1:8001)
 *   api-key: ${EMBEDDING_API_KEY:}          # 내부망 호출이면 비워도 됨
 * </pre>
 *
 * @param serverUrl 임베딩 서버 베이스 URL (미설정이면 null 또는 빈 문자열)
 * @param apiKey    서버 인증 키 – 설정 시 {@code Authorization: Bearer <apiKey>} 헤더로 전달 (미설정 시 생략)
 */
@NullMarked
@ConfigurationProperties(prefix = "embedding")
public record EmbeddingProperties(
	@Nullable String serverUrl,
	@Nullable String apiKey
) {
}

