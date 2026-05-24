package com.jazzify.backend.domain.solo.event;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record SoloOmrRequestedEvent(
	UUID soloPublicId,
	String originalFilename,
	String contentType,
	byte[] fileData
) {
}

