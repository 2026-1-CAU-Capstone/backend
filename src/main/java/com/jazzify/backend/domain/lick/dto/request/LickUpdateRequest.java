package com.jazzify.backend.domain.lick.dto.request;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@NullMarked
public record LickUpdateRequest(
	@NotBlank(message = "제목은 필수입니다.")
	@Size(max = 255, message = "제목은 255자 이하여야 합니다.")
	String title,

	@Nullable
	@Size(max = 255, message = "작곡자는 255자 이하여야 합니다.")
	String composer,

	@NotBlank(message = "내용은 필수입니다.")
	String contents
) {
}

