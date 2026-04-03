package com.jazzify.backend.domain.lick.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record LickResponse(
	UUID publicId,
	String title,
	String contents,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}

