package com.jazzify.backend.shared.embedding;

import java.time.Duration;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jazzify.backend.shared.exception.CustomException;
import com.jazzify.backend.shared.exception.code.EmbeddingErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Jazzify Embedding Worker와 통신하는 HTTP 클라이언트.
 * <p>
 * {@code POST /embed} 엔드포인트를 호출하여 텍스트를 부동소수점 벡터로 변환한다.
 * 단일·배치 모두 {@code {"texts": [...]}} 요청 형식을 사용하고 응답의 {@code embeddings} 필드를 파싱한다.
 * <p>
 * {@code embedding.server-url}이 설정되지 않으면 {@link EmbeddingErrorCode#EMBEDDING_SERVER_NOT_CONFIGURED} 예외를 던진다.
 */
@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
public class EmbeddingClient {

	private static final String EMBED_ENDPOINT = "/v1/embed";
	private static final String HEALTH_ENDPOINT = "/health";
	private static final Duration HEALTH_TIMEOUT = Duration.ofSeconds(3);

	private final EmbeddingProperties embeddingProperties;

	/**
	 * 단일 텍스트를 임베딩 벡터로 변환한다.
	 *
	 * @param text 임베딩할 텍스트
	 * @return Double 리스트로 표현된 임베딩 벡터
	 */
	public List<Double> embed(String text) {
		return embedBatch(List.of(text)).getFirst();
	}

	/**
	 * 여러 텍스트를 배치로 임베딩 벡터 목록으로 변환한다.
	 *
	 * @param texts 임베딩할 텍스트 목록
	 * @return 각 텍스트에 대응하는 임베딩 벡터 목록 (입력 순서 보장)
	 */
	public List<List<Double>> embedBatch(List<String> texts) {
		String serverUrl = requireServerUrl();
		WebClient webClient = createWebClient(serverUrl);

		EmbedRequest body = new EmbedRequest(texts);

		try {
			EmbedResponse response = webClient.post()
				.uri(EMBED_ENDPOINT)
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(body)
				.retrieve()
				.bodyToMono(EmbedResponse.class)
				.block();

			if (response == null || response.embeddings() == null || response.embeddings().isEmpty()) {
				throw EmbeddingErrorCode.EMBEDDING_REQUEST_FAILED.toException(
					"임베딩 서버 응답에 embeddings 필드가 없거나 비어 있습니다."
				);
			}

			log.debug("[EMBED] 임베딩 완료: count={}, dimension={}", response.count(), response.dimension());
			return response.embeddings();

		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			throw EmbeddingErrorCode.EMBEDDING_REQUEST_FAILED.toException(e.getMessage());
		}
	}

	/**
	 * 임베딩 서버가 설정되어 있는지 확인한다.
	 *
	 * @return {@code embedding.server-url}이 설정된 경우 {@code true}
	 */
	public boolean isConfigured() {
		String serverUrl = embeddingProperties.serverUrl();
		return serverUrl != null && !serverUrl.isBlank();
	}

	/**
	 * 임베딩 서버의 {@code GET /health} 를 호출하여 연결 상태를 확인한다.
	 *
	 * @return 설정 여부·URL·연결 가능 여부를 담은 {@link EmbeddingHealthStatus}
	 */
	public EmbeddingHealthStatus checkHealth() {
		String serverUrl = embeddingProperties.serverUrl();
		boolean configured = serverUrl != null && !serverUrl.isBlank();
		if (!configured) {
			return new EmbeddingHealthStatus(false, null, false);
		}
		try {
			createWebClient(serverUrl)
				.get()
				.uri(HEALTH_ENDPOINT)
				.retrieve()
				.toBodilessEntity()
				.block(HEALTH_TIMEOUT);
			return new EmbeddingHealthStatus(true, serverUrl, true);
		} catch (Exception e) {
			log.warn("[EMBED] 헬스체크 실패: {}", e.getMessage());
			return new EmbeddingHealthStatus(true, serverUrl, false);
		}
	}

	// ─── Private Helpers ─────────────────────────────────────────────────

	private String requireServerUrl() {
		String serverUrl = embeddingProperties.serverUrl();
		if (serverUrl == null || serverUrl.isBlank()) {
			throw EmbeddingErrorCode.EMBEDDING_SERVER_NOT_CONFIGURED.toException();
		}
		return serverUrl;
	}

	private static final int MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024; // 10MB

	private WebClient createWebClient(String serverUrl) {
		ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
			.codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
			.build();

		WebClient.Builder builder = WebClient.builder()
			.baseUrl(serverUrl)
			.exchangeStrategies(exchangeStrategies)
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
			.filter(logRequest())
			.filter(logResponse());

		String apiKey = embeddingProperties.apiKey();
		if (apiKey != null && !apiKey.isBlank()) {
			builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
		}

		return builder.build();
	}

	private ExchangeFilterFunction logRequest() {
		return ExchangeFilterFunction.ofRequestProcessor(request -> {
			log.debug("[EMBED] ▶ {} {}", request.method(), request.url());
			return Mono.just(request);
		});
	}

	private ExchangeFilterFunction logResponse() {
		return ExchangeFilterFunction.ofResponseProcessor(response -> {
			log.debug("[EMBED] ◀ Status | {}", response.statusCode());
			return Mono.just(response);
		});
	}

	// ─── Inner DTO Records ───────────────────────────────────────────────

	/**
	 * 임베딩 서버 헬스체크 결과.
	 *
	 * @param configured 서버 URL 설정 여부
	 * @param serverUrl  설정된 서버 URL (미설정 시 null)
	 * @param reachable  실제 HTTP 연결 가능 여부
	 */
	public record EmbeddingHealthStatus(
		boolean configured,
		@Nullable String serverUrl,
		boolean reachable
	) {
	}

	/**
	 * 임베딩 서버 요청 본문.
	 * 배치 형식({@code texts})을 항상 사용한다.
	 */
	private record EmbedRequest(
		@JsonProperty("texts") List<String> texts
	) {
	}

	/**
	 * 임베딩 서버 응답 본문.
	 * <p>
	 * {@code embedding}은 첫 번째 벡터(단일 호출 편의용),
	 * {@code embeddings}는 전체 벡터 배열이다.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	private record EmbedResponse(
		@Nullable String model,
		@Nullable Integer dimension,
		@Nullable Integer count,
		@Nullable Boolean normalized,
		@JsonProperty("embeddings") @Nullable List<List<Double>> embeddings,
		@JsonProperty("embedding") @Nullable List<Double> embedding
	) {
	}
}

