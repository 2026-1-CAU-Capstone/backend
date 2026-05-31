package com.jazzify.backend.core.security;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.domain.user.entity.UserRole;

@NullMarked
public record CustomPrincipal(
	UUID publicId,
	String username,
	UserRole role
) {
}
