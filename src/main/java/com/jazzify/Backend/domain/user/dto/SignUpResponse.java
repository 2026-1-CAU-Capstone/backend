package com.jazzify.backend.domain.user.dto;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public record SignUpResponse(
        UUID publicId,
        String username
) {
}
