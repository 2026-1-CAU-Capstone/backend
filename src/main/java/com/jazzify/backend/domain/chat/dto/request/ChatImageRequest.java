package com.jazzify.backend.domain.chat.dto.request;

import org.jspecify.annotations.NullMarked;

import jakarta.validation.constraints.NotBlank;

@NullMarked
public record ChatImageRequest(
	@NotBlank String mediaType,
	@NotBlank String data
) {
}

