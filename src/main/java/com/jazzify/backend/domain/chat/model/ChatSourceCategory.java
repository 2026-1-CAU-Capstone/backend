package com.jazzify.backend.domain.chat.model;

import org.jspecify.annotations.NullMarked;

import com.fasterxml.jackson.annotation.JsonValue;

@NullMarked
public enum ChatSourceCategory {

	DIRECT("direct"),
	CHORD("chord"),
	SHEET("sheet");

	private final String id;

	ChatSourceCategory(String id) {
		this.id = id;
	}

	@JsonValue
	public String id() {
		return id;
	}
}
