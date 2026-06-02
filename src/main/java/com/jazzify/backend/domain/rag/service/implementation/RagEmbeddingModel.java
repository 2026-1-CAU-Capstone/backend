package com.jazzify.backend.domain.rag.service.implementation;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.jazzify.backend.domain.rag.config.RagProperties;
import com.jazzify.backend.shared.embedding.EmbeddingClient;
import com.jazzify.backend.shared.exception.code.RagErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * Spring AI {@link EmbeddingModel} 구현체.
 * <p>
 * 공유 {@link EmbeddingClient}를 통해 Jazzify Embedding Worker에 임베딩을 요청하고,
 * Spring AI 내부 형식인 {@link EmbeddingResponse}로 변환하여 반환한다.
 * 벡터 차원 수는 {@code rag.vector-store.dimensions} 설정을 따른다.
 */
@Component
@NullMarked
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagEmbeddingModel implements EmbeddingModel {

	private final EmbeddingClient embeddingClient;
	private final RagProperties ragProperties;

	@Override
	public EmbeddingResponse call(EmbeddingRequest request) {
		if (request.getInstructions().isEmpty()) {
			return new EmbeddingResponse(List.of());
		}

		List<List<Double>> vectors = embeddingClient.embedBatch(request.getInstructions());

		List<Embedding> embeddings = new ArrayList<>();
		for (int i = 0; i < vectors.size(); i++) {
			List<Double> vector = vectors.get(i);
			float[] values = new float[vector.size()];
			for (int j = 0; j < vector.size(); j++) {
				values[j] = vector.get(j).floatValue();
			}
			embeddings.add(new Embedding(values, i));
		}
		return new EmbeddingResponse(embeddings);
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
		return embeddingClient.isConfigured();
	}
}
