package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import com.jazzify.backend.domain.analysis.util.NoteUtils.KeyInfo;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.jazzify.backend.domain.analysis.util.NoteUtils.mod12;

/**
 * Layer 1‑1: Diatonic Classifier.
 * Determines diatonic status and assigns scale‑degree labels.
 */
@Component
public class DiatonicClassifier {

    public static final int[] MAJOR_SCALE = {0, 2, 4, 5, 7, 9, 11};
    public static final int[] NATURAL_MINOR_SCALE = {0, 2, 3, 5, 7, 8, 10};
    public static final int[] HARMONIC_MINOR_SCALE = {0, 2, 3, 5, 7, 8, 11};
    public static final int[] MELODIC_MINOR_SCALE = {0, 2, 3, 5, 7, 9, 11};

    public static final Map<String, int[]> QUALITY_INTERVALS = Map.ofEntries(
            Map.entry("maj7", new int[]{0, 4, 7, 11}),
            Map.entry("maj", new int[]{0, 4, 7}),
            Map.entry("min7", new int[]{0, 3, 7, 10}),
            Map.entry("min", new int[]{0, 3, 7}),
            Map.entry("dom7", new int[]{0, 4, 7, 10}),
            Map.entry("min7b5", new int[]{0, 3, 6, 10}),
            Map.entry("dim7", new int[]{0, 3, 6, 9}),
            Map.entry("dim", new int[]{0, 3, 6}),
            Map.entry("aug", new int[]{0, 4, 8}),
            Map.entry("aug7", new int[]{0, 4, 8, 10}),
            Map.entry("augmaj7", new int[]{0, 4, 8, 11}),
            Map.entry("sus4", new int[]{0, 5, 7}),
            Map.entry("sus2", new int[]{0, 2, 7}),
            Map.entry("dom7sus4", new int[]{0, 5, 7, 10}),
            Map.entry("min6", new int[]{0, 3, 7, 9}),
            Map.entry("maj6", new int[]{0, 4, 7, 9}),
            Map.entry("minmaj7", new int[]{0, 3, 7, 11}),
            Map.entry("power", new int[]{0, 7})
    );

    private static final Map<Integer, String> MAJOR_DEGREE_LABELS = Map.ofEntries(
            Map.entry(0, "I"), Map.entry(1, "bII"), Map.entry(2, "ii"), Map.entry(3, "bIII"),
            Map.entry(4, "iii"), Map.entry(5, "IV"), Map.entry(6, "#IV"),
            Map.entry(7, "V"), Map.entry(8, "bVI"), Map.entry(9, "vi"),
            Map.entry(10, "bVII"), Map.entry(11, "vii")
    );

    private static final Map<Integer, String> MINOR_DEGREE_LABELS = Map.ofEntries(
            Map.entry(0, "i"), Map.entry(1, "bII"), Map.entry(2, "ii"), Map.entry(3, "bIII"),
            Map.entry(4, "III"), Map.entry(5, "iv"), Map.entry(6, "#iv"),
            Map.entry(7, "v"), Map.entry(8, "bVI"), Map.entry(9, "vi"),
            Map.entry(10, "bVII"), Map.entry(11, "vii")
    );

    private static final Map<Integer, List<String>> MAJOR_DIATONIC = Map.of(
            0, List.of("maj7", "maj", "maj6"),
            2, List.of("min7", "min"),
            4, List.of("min7", "min"),
            5, List.of("maj7", "maj", "maj6"),
            7, List.of("dom7", "maj"),
            9, List.of("min7", "min", "min6"),
            11, List.of("min7b5", "dim")
    );

    private static final Map<Integer, List<String>> MINOR_DIATONIC = Map.of(
            0, List.of("min7", "min", "min6", "minmaj7"),
            2, List.of("min7b5", "dim"),
            3, List.of("maj7", "maj"),
            5, List.of("min7", "min"),
            7, List.of("min7", "min", "dom7"),
            8, List.of("maj7", "maj"),
            10, List.of("dom7", "maj7", "maj")
    );

    private static final Set<String> MINOR_QUALITIES = Set.of(
            "min7", "min", "min6", "min7b5", "minmaj7", "dim", "dim7"
    );
    private static final Set<String> MAJOR_QUALITIES = Set.of(
            "maj7", "maj", "maj6", "dom7", "aug", "aug7", "augmaj7", "sus4", "sus2", "dom7sus4"
    );
    private static final Set<String> DIM_QUALITIES = Set.of("dim", "dim7", "min7b5");

    // ── public API ──

    public List<ParsedChord> classify(List<ParsedChord> chords, String key) {
        KeyInfo ki = NoteUtils.parseKey(key);
        int keyRoot = ki.root();
        String mode = ki.mode();

        int[] primaryScale = ki.isMajor() ? MAJOR_SCALE : NATURAL_MINOR_SCALE;

        for (ParsedChord chord : chords) {
            int interval = mod12(chord.getRoot() - keyRoot);
            String q = chord.getNormalizedQuality() != null ? chord.getNormalizedQuality() : chord.getQuality();

            chord.setDegree(getDegreeLabel(interval, q, mode));

            boolean isDia = checkDiatonic(chord.getRoot(), q, keyRoot, primaryScale);
            if (!isDia && ki.isMinor()) {
                isDia = checkDiatonic(chord.getRoot(), q, keyRoot, HARMONIC_MINOR_SCALE)
                        || checkDiatonic(chord.getRoot(), q, keyRoot, MELODIC_MINOR_SCALE);
            }
            chord.setIsDiatonic(isDia);
        }
        return chords;
    }

    // ── private helpers ──

    private String getDegreeLabel(int interval, String quality, String mode) {
        Map<Integer, String> labels = "minor".equals(mode) ? MINOR_DEGREE_LABELS : MAJOR_DEGREE_LABELS;
        String label = labels.getOrDefault(interval, "?" + interval);

        if (MINOR_QUALITIES.contains(quality)) {
            label = toLowerDegree(label);
        } else if (MAJOR_QUALITIES.contains(quality)) {
            label = toUpperDegree(label);
        }
        if (DIM_QUALITIES.contains(quality) && !label.endsWith("°") && !label.endsWith("o")) {
            label = label + "°";
        }
        return label;
    }

    private String toLowerDegree(String label) {
        if (label.startsWith("b") || label.startsWith("#")) {
            return label.charAt(0) + label.substring(1).toLowerCase();
        }
        if (label.equals(label.toUpperCase())) return label.toLowerCase();
        return label;
    }

    private String toUpperDegree(String label) {
        if (label.startsWith("b") || label.startsWith("#")) {
            return label.charAt(0) + label.substring(1).toUpperCase();
        }
        return label.toUpperCase();
    }

    private boolean checkDiatonic(int chordRoot, String quality, int keyRoot, int[] scale) {
        int[] intervals = QUALITY_INTERVALS.getOrDefault(quality, new int[]{0, 4, 7});
        Set<Integer> scalePcs = new HashSet<>();
        for (int s : scale) scalePcs.add(mod12(keyRoot + s));

        for (int iv : intervals) {
            if (!scalePcs.contains(mod12(chordRoot + iv))) return false;
        }
        return true;
    }
}

