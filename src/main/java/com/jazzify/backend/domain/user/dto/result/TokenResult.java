package com.jazzify.backend.domain.user.dto.result;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record TokenResult(
	String accessToken,
	String refreshToken,
	UUID publicId,
	String username
) {
}

