package com.jazzify.backend.domain.sheetproject.dto.response;

import java.util.List;

import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.domain.chordproject.dto.response.ChordInfoResponse;

@NullMarked
public record SheetProjectOmrCreateResponse(
	SheetProjectResponse project,
	List<ChordInfoResponse> chords
) {
}

