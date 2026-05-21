package com.jazzify.backend.domain.rag.service.implementation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
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
public class RagEmbeddingModel implements EmbeddingModel {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final RagProperties ragProperties;

	@Override
	public EmbeddingResponse call(EmbeddingRequest request) {
		String serverUrl = resolveServerUrl();
		if (serverUrl == null || serverUrl.isBlank()) {
			throw RagErrorCode.RAG_EMBEDDING_NOT_CONFIGURED.toException();
		}
		if (request.getInstructions().isEmpty()) {
			return new EmbeddingResponse(List.of());
		}

		WebClient.Builder webClientBuilder = WebClient.builder()
			.baseUrl(serverUrl)
			.defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);

		String apiKey = ragProperties.embedding().apiKey();
		if (apiKey != null && !apiKey.isBlank()) {
			webClientBuilder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
		}

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("input", request.getInstructions());

		try {
			String response = webClientBuilder.build().post()
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

			List<Embedding> embeddings = new ArrayList<>();
			for (int i = 0; i < data.size(); i++) {
				JsonNode embeddingNode = data.get(i).get("embedding");
				if (embeddingNode == null || !embeddingNode.isArray() || embeddingNode.isEmpty()) {
					throw RagErrorCode.RAG_EMBEDDING_REQUEST_FAILED.toException("embedding 배열 형식이 올바르지 않습니다.");
				}
				float[] values = new float[embeddingNode.size()];
				for (int j = 0; j < embeddingNode.size(); j++) {
					values[j] = embeddingNode.get(j).floatValue();
				}
				embeddings.add(new Embedding(values, i));
			}
			return new EmbeddingResponse(embeddings);
		} catch (Exception e) {
			throw RagErrorCode.RAG_EMBEDDING_REQUEST_FAILED.toException(e.getMessage());
		}
	}

	@Override
	public float[] embed(Document document) {
		String content = getEmbeddingContent(document);
		if (content == null || content.isBlank()) {
			throw RagErrorCode.RAG_EMBEDDING_REQUEST_FAILED.toException("임베딩할 문서 내용이 비어 있습니다.");
		}
		return EmbeddingModel.super.embed(content);
	}

	@Override
	public int dimensions() {
		return Math.max(1, ragProperties.vectorStore().dimensions());
	}

	public boolean isConfigured() {
		String serverUrl = resolveServerUrl();
		return serverUrl != null && !serverUrl.isBlank();
	}

	private @Nullable String resolveServerUrl() {
		return ragProperties.embedding().serverUrl();
	}
}


