package com.jazzify.backend.domain.chat.model;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record ChatMessageDraft(
	String role,
	String content
) {
}

