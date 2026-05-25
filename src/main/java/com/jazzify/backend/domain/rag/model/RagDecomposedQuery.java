package com.jazzify.backend.domain.rag.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record RagDecomposedQuery(
	String query,
	@Nullable Integer level,
	@Nullable String tag
) {
}

