package com.jazzify.backend.domain.rag.model;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record RagDocument(
	UUID publicId,
	String slug,
	RagSourceType sourceType,
	String title,
	String content,
	Map<String, String> metadata,
	List<String> topicTags,
	int embeddingVersion,
	long chunkCount
) {
}


