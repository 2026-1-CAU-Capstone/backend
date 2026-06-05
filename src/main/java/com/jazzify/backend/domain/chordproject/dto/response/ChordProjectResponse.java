package com.jazzify.backend.domain.chordproject.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.omr.OmrProcessingStatus;

@NullMarked
public record ChordProjectResponse(
	UUID publicId,
	String title,
	MusicKey keySignature,
	String timeSignature,
	OmrProcessingStatus omrStatus,
	int omrProgress,
	@Nullable String omrFailureReason,
	List<ChordInfoResponse> chords,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
