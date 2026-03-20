package com.jazzify.backend.domain.storagefile.dto.response;

import org.jspecify.annotations.NullMarked;

import java.time.LocalDateTime;
import java.util.UUID;

@NullMarked
public record StorageFileResponse(
        UUID publicId,
        String originalFileName,
        long fileSize,
        String contentType,
        LocalDateTime createdAt
) {
}

