package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.ParsedChord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Layer 1‑3: Chord Normalizer.
 * Strips tensions → extracts core quality for pattern matching.
 */
@Component
public class ChordNormalizer {

    private static final Map<String, String> CORE_QUALITY_MAP = Map.ofEntries(
            Map.entry("maj7", "maj7"), Map.entry("maj", "maj"),
            Map.entry("min7", "min7"), Map.entry("min", "min"),
            Map.entry("dom7", "dom7"), Map.entry("min7b5", "min7b5"),
            Map.entry("dim7", "dim7"), Map.entry("dim", "dim"),
            Map.entry("aug", "aug"), Map.entry("aug7", "aug7"),
            Map.entry("augmaj7", "augmaj7"), Map.entry("sus4", "sus4"),
            Map.entry("sus2", "sus2"), Map.entry("dom7sus4", "dom7sus4"),
            Map.entry("min6", "min6"), Map.entry("maj6", "maj6"),
            Map.entry("minmaj7", "minmaj7"), Map.entry("power", "power")
    );

    public List<ParsedChord> normalize(List<ParsedChord> chords) {
        for (ParsedChord c : chords) {
            c.setNormalizedQuality(CORE_QUALITY_MAP.getOrDefault(c.getQuality(), c.getQuality()));
        }
        return chords;
    }
}

