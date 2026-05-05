package com.jazzify.backend.domain.lick.dto.response;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record SheetDataResponse(
	@Nullable String title,
	@Nullable String composer,
	@Nullable String key,
	@Nullable String timeSignature,
	@Nullable Integer tempo,
	List<MeasureResponse> measures
) {
}

