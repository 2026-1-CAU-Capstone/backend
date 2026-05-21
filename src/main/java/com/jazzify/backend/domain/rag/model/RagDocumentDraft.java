package com.jazzify.backend.domain.rag.model;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record RagDocumentDraft(
	String slug,
	RagSourceType sourceType,
	String title,
	String content,
	Map<String, String> metadata,
	List<String> topicTags
) {
}

