package com.jazzify.backend.domain.rag.dto.response;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record RagHealthResponse(
	String status,
	boolean enabled,
	boolean embeddingConfigured,
	boolean llmConfigured,
	long documentCount,
	long chunkCount
) {
}


