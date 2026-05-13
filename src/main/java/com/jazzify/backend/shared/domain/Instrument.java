package com.jazzify.backend.shared.domain;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@NullMarked
public enum Instrument {

	AS("as"),
	TS("ts"),
	TP("tp"),
	P("p"),
	G("g"),
	B("b"),
	VOC("voc"),
	CL("cl"),
	UNKNOWN("unknown");

	private final String value;

	Instrument(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static Instrument from(@Nullable String value) {
		if (value == null || value.isBlank()) return UNKNOWN;
		for (Instrument i : values()) {
			if (i.value.equalsIgnoreCase(value)) {
				return i;
			}
		}
		return UNKNOWN;
	}
}

