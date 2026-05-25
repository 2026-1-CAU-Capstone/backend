package com.jazzify.backend.domain.chat.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record ChatMessageResponse(
	UUID publicId,
	String role,
	String content,
	int sortOrder,
	LocalDateTime createdAt
) {
}

