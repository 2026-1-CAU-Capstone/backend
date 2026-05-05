package com.jazzify.backend.domain.lick.dto.request;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

@NullMarked
public record NoteInfoRequest(
	@NotEmpty
	List<String> keys,

	@NotBlank
	String duration,

	@Nullable Map<String, String> accidentals,
	@Nullable Integer tuplet,
	@Nullable Boolean dotted,
	@Nullable Boolean tie,
	@Nullable Boolean gliss,
	@Nullable Boolean beamBreak
) {
}

