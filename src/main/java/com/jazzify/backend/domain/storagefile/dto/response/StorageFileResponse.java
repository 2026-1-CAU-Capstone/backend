package com.jazzify.backend.domain.storagefile.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record StorageFileResponse(
	UUID publicId,
	String originalFileName,
	long fileSize,
	String contentType,
	LocalDateTime createdAt
) {
}

