package com.jazzify.backend.domain.solo.entity;

import org.jspecify.annotations.NullMarked;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import org.jspecify.annotations.Nullable;

@NullMarked
public enum SoloSource {

	USER("user"),
	WEIMAR("weimar"),
	CURATED("curated"),
	UNKNOWN("unknown");

	private final String value;

	SoloSource(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static SoloSource from(@Nullable String value) {
		if (value == null || value.isBlank()) return UNKNOWN;
		for (SoloSource s : values()) {
			if (s.value.equalsIgnoreCase(value)) {
				return s;
			}
		}
		return UNKNOWN;
	}
}
