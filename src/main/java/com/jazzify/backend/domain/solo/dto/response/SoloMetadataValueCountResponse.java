package com.jazzify.backend.domain.solo.dto.response;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record SoloMetadataValueCountResponse(
	String name,
	long count
) {
}

