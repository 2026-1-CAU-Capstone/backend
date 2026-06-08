package com.jazzify.backend.domain.sheetproject.dto.request;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.Size;

@NullMarked
public record SheetProjectOmrCreateRequest(
	@Nullable
	@Size(max = 255, message = "제목은 255자 이하여야 합니다.")
	String title,

	@Nullable
	@Size(max = 30, message = "조성은 30자 이하여야 합니다.")
	String key
) {
}

