package com.jazzify.backend.domain.sheetproject.event;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.shared.domain.MusicKey;

@NullMarked
public record SheetProjectOmrRequestedEvent(
	UUID projectPublicId,
	String originalFilename,
	String contentType,
	byte[] fileData,
	@Nullable String requestedTitle,
	@Nullable MusicKey requestedKey
) {
}

