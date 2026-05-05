package com.jazzify.backend.domain.lick.dto.response;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record MeasureResponse(
	@Nullable String chord,
	List<NoteInfoResponse> notes
) {
}

