package com.jazzify.backend.domain.user.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.domain.user.entity.UserRole;

@NullMarked
public record UserProfileResponse(
	UUID publicId,
	String name,
	String username,
	UserRole role,
	@Nullable LocalDateTime createdAt,
	@Nullable LocalDateTime updatedAt
) {
}

