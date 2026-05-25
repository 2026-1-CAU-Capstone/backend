package com.jazzify.backend.domain.rag.service.implementation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jazzify.backend.domain.rag.config.RagProperties;
import com.jazzify.backend.shared.exception.code.RagErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@NullMarked
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagEmbeddingClient {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final RagProperties ragProperties;

	public List<Double> embed(String text) {
		return embed(List.of(text)).getFirst();
	}

	public List<List<Double>> embed(List<String> texts) {
		String serverUrl = resolveServerUrl();
		if (serverUrl == null || serverUrl.isBlank()) {
			throw RagErrorCode.RAG_EMBEDDING_NOT_CONFIGURED.toException();
		}

		WebClient.Builder webClientBuilder = WebClient.builder()
			.baseUrl(serverUrl)
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

		String apiKey = ragProperties.embedding().apiKey();
		if (apiKey != null && !apiKey.isBlank()) {
			webClientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
		}

		WebClient webClient = webClientBuilder.build();

		Map<String, Object> body = new LinkedHashMap<>();
		// TODO: 실제 임베딩 서버 요청 포맷이 확정되면 이 body 구조를 그 계약에 맞게 교체한다.
		body.put("input", texts);

		try {
			String response = webClient.post()
				.uri(ragProperties.embedding().endpoint())
				.bodyValue(body)
				.retrieve()
				.bodyToMono(String.class)
				.block();

			if (response == null || response.isBlank()) {
				throw RagErrorCode.RAG_EMBEDDING_REQUEST_FAILED.toException("빈 응답");
			}

			JsonNode root = OBJECT_MAPPER.readTree(response);
			JsonNode data = root.get("data");
			if (data == null || !data.isArray() || data.isEmpty()) {
				throw RagErrorCode.RAG_EMBEDDING_REQUEST_FAILED.toException("embedding data가 비어 있습니다.");
			}

			List<List<Double>> embeddings = new ArrayList<>();
			for (JsonNode item : data) {
				JsonNode embeddingNode = item.get("embedding");
				List<Double> embedding = new ArrayList<>();
				for (JsonNode value : embeddingNode) {
					embedding.add(value.asDouble());
				}
				embeddings.add(embedding);
			}
			return embeddings;
		} catch (Exception e) {
			throw RagErrorCode.RAG_EMBEDDING_REQUEST_FAILED.toException(e.getMessage());
		}
	}

	public boolean isConfigured() {
		String serverUrl = resolveServerUrl();
		return serverUrl != null && !serverUrl.isBlank();
	}

	private @Nullable String resolveServerUrl() {
		return ragProperties.embedding().serverUrl();
	}
}



