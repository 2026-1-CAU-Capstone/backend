package com.jazzify.backend.domain.chat.util;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.domain.chat.dto.request.ChatMessageRequest;
import com.jazzify.backend.domain.chat.dto.response.ChatDetailResponse;
import com.jazzify.backend.domain.chat.dto.response.ChatMessageResponse;
import com.jazzify.backend.domain.chat.dto.response.ChatSummaryResponse;
import com.jazzify.backend.domain.chat.entity.Chat;
import com.jazzify.backend.domain.chat.entity.ChatMessage;
import com.jazzify.backend.domain.chat.model.ChatHistoryMessage;
import com.jazzify.backend.domain.chat.model.ChatSourceCategory;
import com.jazzify.backend.domain.chat.model.ChatType;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatMapper {

	public static ChatSummaryResponse toSummaryResponse(Chat chat) {
		return new ChatSummaryResponse(
			requirePublicId(chat),
			chat.getType(),
			chat.getTitle(),
			sourceCategory(chat),
			chat.getSongTitle(),
			chat.getProjectPublicId(),
			requireCreatedAt(chat.getCreatedAt()),
			requireUpdatedAt(chat.getUpdatedAt())
		);
	}

	public static ChatDetailResponse toDetailResponse(Chat chat, List<ChatMessage> messages) {
		return new ChatDetailResponse(
			requirePublicId(chat),
			chat.getType(),
			chat.getTitle(),
			sourceCategory(chat),
			chat.getSongTitle(),
			chat.getProjectPublicId(),
			requireCreatedAt(chat.getCreatedAt()),
			requireUpdatedAt(chat.getUpdatedAt()),
			messages.stream().map(ChatMapper::toMessageResponse).toList()
		);
	}

	public static ChatMessageResponse toMessageResponse(ChatMessage message) {
		return new ChatMessageResponse(
			Objects.requireNonNull(message.getPublicId(), "chatMessage.publicId must not be null"),
			message.getRole(),
			message.getContent(),
			message.getSortOrder(),
			requireCreatedAt(message.getCreatedAt())
		);
	}

	public static ChatHistoryMessage toHistoryMessage(ChatMessageRequest request) {
		return new ChatHistoryMessage(request.role(), request.content());
	}

	public static ChatHistoryMessage toHistoryMessage(ChatMessage message) {
		return new ChatHistoryMessage(message.getRole(), message.getContent());
	}

	private static UUID requirePublicId(Chat chat) {
		return Objects.requireNonNull(chat.getPublicId(), "chat.publicId must not be null");
	}

	private static ChatSourceCategory sourceCategory(Chat chat) {
		if (chat.getSourceCategory() != null) {
			return chat.getSourceCategory();
		}
		ChatType type = chat.getType();
		if (type == ChatType.CHORD_PROJECT) {
			return ChatSourceCategory.CHORD;
		}
		if (type == ChatType.SHEET_PROJECT) {
			return ChatSourceCategory.SHEET;
		}
		return ChatSourceCategory.DIRECT;
	}

	private static LocalDateTime requireCreatedAt(LocalDateTime createdAt) {
		return Objects.requireNonNull(createdAt, "createdAt must not be null");
	}

	private static LocalDateTime requireUpdatedAt(LocalDateTime updatedAt) {
		return Objects.requireNonNull(updatedAt, "updatedAt must not be null");
	}
}

