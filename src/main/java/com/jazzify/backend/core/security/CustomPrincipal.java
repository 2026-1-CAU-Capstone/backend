package com.jazzify.backend.core.security;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record CustomPrincipal(
	UUID publicId,
	String username
) {
}
