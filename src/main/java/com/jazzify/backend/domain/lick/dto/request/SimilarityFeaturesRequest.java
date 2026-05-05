package com.jazzify.backend.domain.lick.dto.request;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 섹션 5 유사도 피처 요청. 모든 필드가 선택값이며,
 * null인 필드는 sheetData로부터 자동으로 계산된다.
 */
@NullMarked
public record SimilarityFeaturesRequest(
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

