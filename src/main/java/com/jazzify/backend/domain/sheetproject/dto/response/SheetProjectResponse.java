package com.jazzify.backend.domain.sheetproject.dto.response;

import com.jazzify.backend.shared.domain.MusicKey;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public record SheetProjectResponse(
        UUID publicId,
        String title,
        @Nullable MusicKey key,
        UUID filePublicId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
