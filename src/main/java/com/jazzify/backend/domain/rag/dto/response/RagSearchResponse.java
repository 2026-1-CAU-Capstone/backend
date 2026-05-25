package com.jazzify.backend.domain.rag.dto.response;

import java.util.List;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record RagSearchResponse(
	String query,
	List<RagChunkResponse> results
) {
}

