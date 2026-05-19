package com.jazzify.backend.domain.sheetproject.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.omr.OmrProcessingStatus;

@NullMarked
public record SheetProjectResponse(
	UUID publicId,
	String title,
	@Nullable MusicKey keySignature,
	UUID filePublicId,
	OmrProcessingStatus omrStatus,
	int omrProgress,
	@Nullable String omrFailureReason,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
