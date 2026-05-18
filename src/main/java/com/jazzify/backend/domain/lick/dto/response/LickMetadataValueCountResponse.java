package com.jazzify.backend.domain.lick.dto.response;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record LickMetadataValueCountResponse(
	String name,
	long count
) {
}

