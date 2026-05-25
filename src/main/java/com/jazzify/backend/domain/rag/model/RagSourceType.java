package com.jazzify.backend.domain.rag.model;

import java.util.Locale;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum RagSourceType {
	STANDARD,
	LESSON;

	public String dbValue() {
		return name().toLowerCase(Locale.ROOT);
	}

	public static RagSourceType from(String value) {
		return RagSourceType.valueOf(value.trim().toUpperCase(Locale.ROOT));
	}
}

