package com.jazzify.backend.domain.sheetproject.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.shared.domain.MusicKey;

@NullMarked
public record SheetProjectResponse(
	UUID publicId,
	String title,
	@Nullable MusicKey keySignature,
	UUID filePublicId,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
