package com.jazzify.backend.domain.lick.entity;

import org.jspecify.annotations.NullMarked;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

@NullMarked
public enum LickSource {

	USER("user"),
	WEIMAR("weimar"),
	CURATED("curated");

	private final String value;

	LickSource(String value) {
		this.value = value;
	}

	@JsonValue
	public String getValue() {
		return value;
	}

	@JsonCreator
	public static LickSource from(String value) {
		for (LickSource s : values()) {
			if (s.value.equalsIgnoreCase(value)) {
				return s;
			}
		}
		throw new IllegalArgumentException("Unknown LickSource: " + value);
	}
}

