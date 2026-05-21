package com.jazzify.backend.domain.rag.model;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record RagChunkSearchResult(
	String id,
	double score,
	RagSourceType sourceType,
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

	public RagChunkSearchResult withFusion(@Nullable Double updatedRrfScore, List<String> updatedMatchedQueries) {
		return new RagChunkSearchResult(
			id,
			score,
			sourceType,
			title,
			song,
			key,
			level,
			sectionId,
			instruction,
			response,
			topicTags,
			source,
			analyzedSongs,
			updatedRrfScore,
			updatedMatchedQueries
		);
	}
}

