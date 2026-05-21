package com.jazzify.backend.domain.chat.model;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record ChatHistoryMessage(
	String role,
	String content
) {
}

