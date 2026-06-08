package com.jazzify.backend.domain.rag.dto.request;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonAlias;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@NullMarked
public record RagChatRequest(
	@NotBlank String message,
	@JsonAlias({"chordContext", "chord_context"}) @Nullable Map<String, Object> chordContext,
	@JsonAlias({"chordContextText", "chord_context_text"}) @Nullable String chordContextText,
	@Valid List<RagChatMessageRequest> history,
	@JsonAlias({"songTitle", "song_title"}) @Nullable String songTitle,
	@JsonAlias({"projectPublicId", "project_public_id"}) @Nullable String projectPublicId,
	@JsonAlias({"suppressInlineChart", "suppress_inline_chart"}) boolean suppressInlineChart,
	@JsonAlias({"chatPublicId", "chat_public_id"}) @Nullable UUID chatPublicId
) {

	public RagChatRequest {
		chordContext = chordContext != null ? Map.copyOf(new LinkedHashMap<>(chordContext)) : null;
		history = history != null ? List.copyOf(history) : List.of();
	}
}

