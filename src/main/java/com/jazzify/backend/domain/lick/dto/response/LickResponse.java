package com.jazzify.backend.domain.lick.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record LickResponse(
	UUID publicId,
	String title,
	@Nullable String composer,
	String content,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}

