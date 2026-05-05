package com.jazzify.backend.domain.lick.entity;

import org.jspecify.annotations.NullMarked;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@NullMarked
public enum HarmonicContext {

	II_V_I("ii-V-I"),
	MINOR_II_V("minor-ii-V"),
	BLUES("blues"),
	MODAL("modal"),
	TURNAROUND("turnaround"),
	OTHER("other");

	private final String value;

	HarmonicContext(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static HarmonicContext from(String value) {
		for (HarmonicContext h : values()) {
			if (h.value.equalsIgnoreCase(value)) {
				return h;
			}
		}
		throw new IllegalArgumentException("Unknown HarmonicContext: " + value);
	}
}

