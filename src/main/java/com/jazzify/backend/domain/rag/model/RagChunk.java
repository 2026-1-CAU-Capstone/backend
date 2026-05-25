package com.jazzify.backend.domain.rag.model;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record RagChunk(
	UUID publicId,
	UUID documentPublicId,
	String chunkId,
	String sectionId,
	int level,
	String title,
	@Nullable String instruction,
	String response,
	String embedText,
	RagSourceType sourceType,
	String song,
	String key,
	String source,
	String analyzedSongs,
	List<String> topicTags,
	List<Double> embedding
) {
}

