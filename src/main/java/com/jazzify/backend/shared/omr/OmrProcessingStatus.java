package com.jazzify.backend.shared.omr;

import org.jspecify.annotations.NullMarked;

@NullMarked
public enum OmrProcessingStatus {
	PENDING,
	PROCESSING,
	COMPLETED,
	FAILED
}

