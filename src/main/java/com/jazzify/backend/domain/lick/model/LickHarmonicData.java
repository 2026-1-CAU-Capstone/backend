package com.jazzify.backend.domain.lick.model;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.domain.lick.entity.HarmonicContext;

/**
 * 섹션 3 화성 맥락 집합.
 * sheetData에서 자동 추출되거나, 요청에서 직접 제공된 값을 담는다.
 */
@NullMarked
public record LickHarmonicData(
	List<String> chords,
	List<String> chordsPerNote,
	@Nullable HarmonicContext harmonicContext,
	@Nullable String targetChord
) {
}

