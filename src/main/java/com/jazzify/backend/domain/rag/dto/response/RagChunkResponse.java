package com.jazzify.backend.domain.rag.dto.response;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record RagChunkResponse(
	String id,
	double score,
	String sourceType,
	String title,
	String song,
	String key,
	int level,
	String sectionId,
	@Nullable String instruction,
	String response,
	List<String> topicTags,
	String source,
	String analyzedSongs,
	@Nullable Double rrfScore,
	List<String> matchedQueries
) {
}

