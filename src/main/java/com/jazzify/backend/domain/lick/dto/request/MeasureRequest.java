package com.jazzify.backend.domain.lick.dto.request;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

@NullMarked
public record MeasureRequest(
	@Nullable String chord,

	@NotEmpty @Valid
	List<NoteInfoRequest> notes
) {
}

