package com.jazzify.backend.domain.chordproject.dto.request;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.shared.domain.MusicKey;

import jakarta.validation.constraints.Size;

@NullMarked
public record ChordProjectOmrCreateRequest(
	@Nullable
	@Size(max = 255, message = "제목은 255자 이하여야 합니다.")
	String title,

	@Nullable
	MusicKey key,

	@Nullable
	@Size(max = 10, message = "박자표는 10자 이하여야 합니다.")
	String timeSignature,

	@Nullable
	@Size(max = 30, message = "sourceType은 30자 이하여야 합니다.")
	String sourceType
) {
}

