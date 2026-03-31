package com.jazzify.backend.domain.chordproject.dto.response;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record ChordInfoResponse(
	UUID publicId,
	@Nullable String chord,
	int bar,
	double beat,
	double durationBeats,
	@Nullable ChordAnalysisResponse analysis
) {

	public record ChordAnalysisResponse(
		@Nullable String degree,
		@Nullable Boolean isDiatonic,
		@Nullable String normalizedQuality,
		@Nullable List<Object> functions,
		@Nullable Object secondaryDominant,
		@Nullable String diminishedFunction,
		@Nullable Object chromaticApproach,
		@Nullable Object deceptiveResolution,
		@Nullable Object pedalInfo,
		@Nullable Object modalInterchange,
		@Nullable String modeSegment,
		@Nullable Object tonicization,
		double ambiguityScore,
		@Nullable List<Object> ambiguityFlags
	) {
	}
}


