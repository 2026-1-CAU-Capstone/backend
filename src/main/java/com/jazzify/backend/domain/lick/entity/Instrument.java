package com.jazzify.backend.domain.lick.entity;

import org.jspecify.annotations.NullMarked;

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
	CL("cl");

	private final String value;

	Instrument(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static Instrument from(String value) {
		for (Instrument i : values()) {
			if (i.value.equalsIgnoreCase(value)) {
				return i;
			}
		}
		throw new IllegalArgumentException("Unknown Instrument: " + value);
	}
}

