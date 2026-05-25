package com.jazzify.backend.domain.chat.dto.request;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.jazzify.backend.domain.chat.model.ChatAnalysisCategory;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@NullMarked
public record ChatStreamRequest(
	@NotBlank String message,
	@Valid List<ChatMessageRequest> history,
	@JsonAlias({"chordContext", "chord_context"}) @Nullable String chordContext,
	@Nullable ChatAnalysisCategory category,
	@JsonAlias({"songTitle", "song_title"}) @Nullable String songTitle,
	@Valid List<ChatImageRequest> images,
	@JsonAlias({"chatPublicId", "chat_public_id"}) @Nullable UUID chatPublicId
) {

	public ChatStreamRequest {
		history = history != null ? List.copyOf(history) : List.of();
		images = images != null ? List.copyOf(images) : List.of();
	}
}

