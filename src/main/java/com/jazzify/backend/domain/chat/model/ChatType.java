package com.jazzify.backend.domain.chat.model;

import org.jspecify.annotations.NullMarked;

import com.fasterxml.jackson.annotation.JsonValue;

@NullMarked
public enum ChatType {

	DIRECT("direct"),
	RAG("rag");

	private final String id;

	ChatType(String id) {
		this.id = id;
	}

	@JsonValue
	public String id() {
		return id;
	}
}

