package com.jazzify.backend.domain.user.dto.result;

import org.jspecify.annotations.NullMarked;

import java.util.UUID;

@NullMarked
public record TokenResult(
        String accessToken,
        String refreshToken,
        UUID publicId,
        String username
) {
}

