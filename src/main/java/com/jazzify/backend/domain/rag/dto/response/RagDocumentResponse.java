package com.jazzify.backend.domain.rag.dto.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record RagDocumentResponse(
	UUID publicId,
	String slug,
	String sourceType,
	String title,
	String content,
	Map<String, String> metadata,
	List<String> topicTags,
	int embeddingVersion,
	long chunkCount
) {
}

