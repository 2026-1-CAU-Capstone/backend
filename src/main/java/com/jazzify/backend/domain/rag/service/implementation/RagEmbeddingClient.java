package com.jazzify.backend.domain.rag.service.implementation;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.jazzify.backend.shared.embedding.EmbeddingClient;

import lombok.RequiredArgsConstructor;

/**
 * RAG 도메인 전용 임베딩 클라이언트 위임자.
 * <p>
 * 공유 {@link EmbeddingClient}에 임베딩 요청을 위임한다.
 * RAG 도메인 내부에서 임베딩이 필요한 컴포넌트가 이 클래스를 통해 접근하도록 하여
 * 공유 계층과의 직접 의존을 캡슐화한다.
 */
@Component
@NullMarked
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagEmbeddingClient {

	private final EmbeddingClient embeddingClient;

	public List<Double> embed(String text) {
		return embeddingClient.embed(text);
	}

	public List<List<Double>> embed(List<String> texts) {
		return embeddingClient.embedBatch(texts);
	}

	public boolean isConfigured() {
		return embeddingClient.isConfigured();
	}
}
