package com.jazzify.backend.domain.user.dto.response;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public record SignUpResponse(
        UUID publicId,
        String username
) {
}
