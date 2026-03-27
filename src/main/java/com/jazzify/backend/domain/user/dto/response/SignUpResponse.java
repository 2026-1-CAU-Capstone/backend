package com.jazzify.backend.domain.user.dto.response;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record SignUpResponse(
	UUID publicId,
	String name,
	String username
) {
}
