package com.jazzify.backend.domain.lick.dto.app;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record LickMetadataValueCountResult(
	String name,
	long count
) {
}

