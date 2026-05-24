package com.jazzify.backend.domain.lick.event;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record LickOmrRequestedEvent(
	UUID lickPublicId,
	String originalFilename,
	String contentType,
	byte[] fileData
) {
}

