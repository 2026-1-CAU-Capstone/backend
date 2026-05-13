package com.jazzify.backend.domain.solo.dto.response;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record SoloVideoResponse(
	String videoId,
	@Nullable Double startSec,
	@Nullable Double endSec,
	String url
) {
}

