package com.jazzify.backend.domain.rag.dto.response;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record RagDocumentSummaryResponse(
	UUID publicId,
	String slug,
	String sourceType,
	String title,
	List<String> topicTags,
	int embeddingVersion,
	long chunkCount
) {
}

