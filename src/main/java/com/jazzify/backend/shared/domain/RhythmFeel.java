package com.jazzify.backend.shared.domain;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@NullMarked
public enum RhythmFeel {

	SWING("SWING"),
	STRAIGHT("STRAIGHT"),
	BOSSA("BOSSA"),
	LATIN("LATIN");

	private final String value;

	RhythmFeel(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static @Nullable RhythmFeel from(@Nullable String value) {
		if (value == null || value.isBlank()) return null;
		for (RhythmFeel r : values()) {
			if (r.value.equalsIgnoreCase(value)) {
				return r;
			}
		}
		throw new IllegalArgumentException("Unknown RhythmFeel: " + value);
	}
}

