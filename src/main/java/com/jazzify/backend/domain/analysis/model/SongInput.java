package com.jazzify.backend.domain.analysis.model;

import java.util.List;

/**
 * Full song input.
 */
public record SongInput(
        String title,
        String key,
        String timeSignature,
        List<ChordEntry> chords,
        Integer tempo
) {}

