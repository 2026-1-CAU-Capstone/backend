package com.jazzify.backend.domain.rag.service.implementation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.rag.model.RagChunkSearchResult;
import com.jazzify.backend.domain.rag.model.RagDocument;
import com.jazzify.backend.domain.rag.model.RagSourceType;
import com.jazzify.backend.domain.rag.repository.RagChunkRepository;
import com.jazzify.backend.domain.rag.repository.RagDocumentRepository;
import com.jazzify.backend.shared.exception.code.RagErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@NullMarked
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
@Transactional(transactionManager = "ragTransactionManager", readOnly = true)
public class RagReader {

	private final RagChunkRepository ragChunkRepository;
	private final RagDocumentRepository ragDocumentRepository;
	private final RagEmbeddingClient ragEmbeddingClient;

	public RagDocument getDocumentByPublicId(UUID publicId) {
		return ragDocumentRepository.findByPublicId(publicId)
			.orElseThrow(RagErrorCode.RAG_DOCUMENT_NOT_FOUND::toException);
	}

	public Optional<RagDocument> findDocumentBySlug(String slug) {
		return ragDocumentRepository.findBySlug(slug);
	}

	public Page<RagDocument> getDocuments(@Nullable RagSourceType sourceType, @Nullable String query, Pageable pageable) {
		return ragDocumentRepository.findAll(sourceType, query, pageable);
	}

	public List<RagChunkSearchResult> search(
		String query,
		int limit,
		@Nullable Integer levelFilter,
		@Nullable String songFilter,
		@Nullable String tagFilter,
		@Nullable RagSourceType sourceType
	) {
		List<Double> embedding = ragEmbeddingClient.embed(query);
		return ragChunkRepository.searchByEmbedding(embedding, limit, levelFilter, songFilter, tagFilter, sourceType);
	}

	public List<RagChunkSearchResult> searchByEmbedding(
		List<Double> embedding,
		int limit,
		@Nullable Integer levelFilter,
		@Nullable String songFilter,
		@Nullable String tagFilter,
		@Nullable RagSourceType sourceType
	) {
		return ragChunkRepository.searchByEmbedding(embedding, limit, levelFilter, songFilter, tagFilter, sourceType);
	}

	public long countChunks() {
		return ragChunkRepository.count();
	}

	public long countDocuments() {
		return ragDocumentRepository.count();
	}
}


