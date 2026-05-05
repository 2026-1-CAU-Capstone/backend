package com.jazzify.backend.domain.lick.dto.response;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record NoteInfoResponse(
	List<String> keys,
	String duration,
	@Nullable Map<String, String> accidentals,
	@Nullable Integer tuplet,
	@Nullable Boolean dotted,
	@Nullable Boolean tie,
	@Nullable Boolean gliss,
	@Nullable Boolean beamBreak
) {
}

