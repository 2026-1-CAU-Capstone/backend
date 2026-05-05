package com.jazzify.backend.domain.lick.entity;

import org.jspecify.annotations.NullMarked;

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
	public static RhythmFeel from(String value) {
		for (RhythmFeel r : values()) {
			if (r.value.equalsIgnoreCase(value)) {
				return r;
			}
		}
		throw new IllegalArgumentException("Unknown RhythmFeel: " + value);
	}
}

