package com.jazzify.backend.domain.chordproject.model;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * ChordProject OMR 결과에서 추출한 코드의 저장 위치 정보.
 */
@NullMarked
public record ChordProjectOmrChord(
	int bar,
	@Nullable String chord,
	double beat,
	double durationBeats
) {
}
