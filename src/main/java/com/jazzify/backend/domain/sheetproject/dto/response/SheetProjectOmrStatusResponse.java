package com.jazzify.backend.domain.sheetproject.dto.response;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.shared.omr.OmrProcessingStatus;

@NullMarked
public record SheetProjectOmrStatusResponse(
	UUID publicId,
	OmrProcessingStatus status,
	int progress,
	@Nullable String failureReason
) {
}

