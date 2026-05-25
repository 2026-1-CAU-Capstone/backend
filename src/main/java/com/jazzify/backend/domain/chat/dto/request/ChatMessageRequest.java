package com.jazzify.backend.domain.chat.dto.request;

import org.jspecify.annotations.NullMarked;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@NullMarked
public record ChatMessageRequest(
	@NotBlank @Pattern(regexp = "user|assistant") String role,
	@NotBlank String content
) {
}

