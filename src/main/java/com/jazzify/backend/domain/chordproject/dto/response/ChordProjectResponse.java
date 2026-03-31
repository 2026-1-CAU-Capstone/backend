package com.jazzify.backend.domain.chordproject.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.shared.domain.MusicKey;

@NullMarked
public record ChordProjectResponse(
	UUID publicId,
	String title,
	MusicKey keySignature,
	String timeSignature,
	LocalDateTime createdAt,
	LocalDateTime updatedAt
) {
}
