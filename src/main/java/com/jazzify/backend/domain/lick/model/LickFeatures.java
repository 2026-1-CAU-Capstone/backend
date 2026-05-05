package com.jazzify.backend.domain.lick.model;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 섹션 5 유사도 피처 집합.
 * sheetData에서 자동 계산되거나, 요청에서 직접 제공된 값을 담는다.
 */
@NullMarked
public record LickFeatures(
	@Nullable Integer nEvents,
	@Nullable List<Integer> pitches,
	@Nullable List<Integer> intervals,
	@Nullable List<Integer> parsons,
	@Nullable List<Integer> fuzzyIntervals,
	@Nullable List<Integer> durationClasses,
	@Nullable Integer pitchMin,
	@Nullable Integer pitchMax,
	@Nullable Integer pitchRange,
	@Nullable Double pitchMean,
	@Nullable Integer startPitch,
	@Nullable Integer endPitch
) {
}

