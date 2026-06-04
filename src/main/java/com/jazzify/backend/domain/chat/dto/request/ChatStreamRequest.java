package com.jazzify.backend.domain.chat.dto.request;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;
import com.jazzify.backend.domain.chat.model.ChatAnalysisCategory;
import com.jazzify.backend.domain.rag.dto.request.RagChatMessageRequest;
import com.jazzify.backend.domain.rag.dto.request.RagChatRequest;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@NullMarked
public record ChatStreamRequest(
	@NotBlank String message,
	@Valid List<ChatMessageRequest> history,
	@JsonAlias({"chordContext", "chord_context"}) @Nullable JsonNode chordContext,
	@Nullable ChatAnalysisCategory category,
	@JsonAlias({"songTitle", "song_title"}) @Nullable String songTitle,
	@Valid List<ChatImageRequest> images,
	@JsonAlias({"chatPublicId", "chat_public_id"}) @Nullable UUID chatPublicId,
	@JsonAlias({"useRag", "use_rag", "rag"}) boolean useRag,
	@JsonAlias({"chordContextText", "chord_context_text"}) @Nullable String chordContextText,
	@JsonAlias({"suppressInlineChart", "suppress_inline_chart"}) boolean suppressInlineChart
) {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public ChatStreamRequest {
		history = history != null ? List.copyOf(history) : List.of();
		images = images != null ? List.copyOf(images) : List.of();
	}

	public ChatStreamRequest(
		String message,
		List<ChatMessageRequest> history,
		@Nullable String chordContext,
		@Nullable ChatAnalysisCategory category,
		@Nullable String songTitle,
		List<ChatImageRequest> images,
		@Nullable UUID chatPublicId
	) {
		this(
			message,
			history,
			chordContext != null ? TextNode.valueOf(chordContext) : null,
			category,
			songTitle,
			images,
			chatPublicId,
			false,
			null,
			false
		);
	}

	public @Nullable String directChordContext() {
		if (chordContext == null || chordContext.isNull()) {
			return null;
		}
		if (chordContext.isTextual()) {
			return chordContext.asText();
		}
		return chordContext.toString();
	}

	public RagChatRequest toRagChatRequest() {
		return new RagChatRequest(
			message,
			ragChordContext(),
			chordContextText,
			history.stream()
				.map(message -> new RagChatMessageRequest(message.role(), message.content()))
				.toList(),
			songTitle,
			suppressInlineChart,
			chatPublicId
		);
	}

	@SuppressWarnings("unchecked")
	private @Nullable Map<String, Object> ragChordContext() {
		if (chordContext == null || chordContext.isNull() || !chordContext.isObject()) {
			return null;
		}
		return OBJECT_MAPPER.convertValue(chordContext, Map.class);
	}
}

