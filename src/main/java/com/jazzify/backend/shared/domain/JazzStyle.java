package com.jazzify.backend.shared.domain;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@NullMarked
public enum JazzStyle {

	SWING("SWING"),
	BEBOP("BEBOP"),
	HARDBOP("HARDBOP"),
	COOL("COOL"),
	MODAL("MODAL"),
	FUSION("FUSION");

	private final String value;

	JazzStyle(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static @Nullable JazzStyle from(@Nullable String value) {
		if (value == null || value.isBlank()) return null;
		for (JazzStyle s : values()) {
			if (s.value.equalsIgnoreCase(value)) {
				return s;
			}
		}
		throw new IllegalArgumentException("Unknown JazzStyle: " + value);
	}
}

