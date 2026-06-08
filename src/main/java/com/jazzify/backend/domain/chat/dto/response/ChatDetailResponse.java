package com.jazzify.backend.domain.chat.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.domain.chat.model.ChatSourceCategory;
import com.jazzify.backend.domain.chat.model.ChatType;

@NullMarked
public record ChatDetailResponse(
	UUID publicId,
	ChatType type,
	String title,
	ChatSourceCategory category,
	@Nullable String songTitle,
	@Nullable String projectPublicId,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,
	List<ChatMessageResponse> messages
) {
}

