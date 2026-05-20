package com.jazzify.backend.domain.chordproject.dto.response;

import java.util.List;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record ChordProjectOmrCreateResponse(
	ChordProjectResponse project,
	List<ChordInfoResponse> chords
) {
}

