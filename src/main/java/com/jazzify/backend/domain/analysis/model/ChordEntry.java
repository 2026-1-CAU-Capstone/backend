package com.jazzify.backend.domain.analysis.model;

/**
 * Single chord entry from input.
 */
public record ChordEntry(
        int bar,
        double beat,
        String symbol,
        double durationBeats
) {}

