package com.jazzify.backend.domain.chordproject.model;

import java.util.Locale;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.shared.exception.code.ChordProjectErrorCode;

@NullMarked
public enum ChordProjectOmrSourceType {
	SHEET_MUSIC,
	CHORD_CHART;

	public static ChordProjectOmrSourceType from(@Nullable String value) {
		if (value == null || value.isBlank()) {
			return CHORD_CHART;
		}

		String normalized = value.trim()
			.toUpperCase(Locale.ROOT)
			.replace("-", "_");

		return switch (normalized) {
			case "SHEET", "SHEET_MUSIC" -> SHEET_MUSIC;
			case "CHART", "CHORD_CHART" -> CHORD_CHART;
			default -> throw ChordProjectErrorCode.INVALID_OMR_SOURCE_TYPE.toException(value);
		};
	}
}
