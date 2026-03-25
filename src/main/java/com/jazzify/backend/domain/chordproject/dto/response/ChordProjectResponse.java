package com.jazzify.backend.domain.chordproject.dto.response;

import com.jazzify.backend.shared.domain.MusicKey;
import org.jspecify.annotations.NullMarked;

import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public record ChordProjectResponse(
        UUID publicId,
        String title,
        MusicKey keySignature,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
