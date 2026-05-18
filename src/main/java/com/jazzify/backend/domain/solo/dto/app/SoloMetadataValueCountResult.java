package com.jazzify.backend.domain.solo.dto.app;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record SoloMetadataValueCountResult(
	String name,
	long count
) {
}

