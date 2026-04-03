package com.jazzify.backend.domain.lick.dto.request;

import org.jspecify.annotations.NullMarked;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@NullMarked
public record LickUpdateRequest(
	@NotBlank(message = "제목은 필수입니다.")
	@Size(max = 255, message = "제목은 255자 이하여야 합니다.")
	String title,

	@NotBlank(message = "내용은 필수입니다.")
	String contents
) {
}

