package com.jazzify.backend.domain.lick.dto.response;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record LickVideoResponse(
	String videoId,
	@Nullable Double startSec,
	@Nullable Double endSec,
	String url
) {
}

