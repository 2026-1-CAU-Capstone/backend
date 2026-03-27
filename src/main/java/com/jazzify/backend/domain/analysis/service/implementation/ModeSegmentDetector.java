package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import com.jazzify.backend.domain.analysis.util.NoteUtils.KeyInfo;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.jazzify.backend.domain.analysis.util.NoteUtils.mod12;

/**
 * Layer 3‑12: Mode Segment Detector.
 */
@Component
public class ModeSegmentDetector {

    private static final Map<String, int[]> MODE_SCALES = Map.of(
            "ionian", new int[]{0, 2, 4, 5, 7, 9, 11},
            "dorian", new int[]{0, 2, 3, 5, 7, 9, 10},
            "phrygian", new int[]{0, 1, 3, 5, 7, 8, 10},
            "lydian", new int[]{0, 2, 4, 6, 7, 9, 11},
            "mixolydian", new int[]{0, 2, 4, 5, 7, 9, 10},
            "aeolian", new int[]{0, 2, 3, 5, 7, 8, 10},
            "locrian", new int[]{0, 1, 3, 5, 6, 8, 10}
    );

    private static final int WINDOW_BARS = 4;
    private static final double THRESHOLD = 0.55;

    public List<ParsedChord> detect(List<ParsedChord> chords, String key) {
        if (chords.isEmpty()) return chords;
        KeyInfo ki = NoteUtils.parseKey(key);
        int keyRoot = ki.root();
        String defaultMode = ki.isMinor() ? "aeolian" : "ionian";

        int maxBar = chords.stream().mapToInt(ParsedChord::getBar).max().orElse(1);

        // Group chord indices by bar
        Map<Integer, List<Integer>> barChords = new HashMap<>();
        for (int idx = 0; idx < chords.size(); idx++) {
            barChords.computeIfAbsent(chords.get(idx).getBar(), k -> new ArrayList<>()).add(idx);
        }

        for (int startBar = 1; startBar <= maxBar; startBar++) {
            int endBar = Math.min(startBar + WINDOW_BARS - 1, maxBar);

            Set<Integer> windowPcs = new HashSet<>();
            List<Integer> windowIndices = new ArrayList<>();
            List<ParsedChord> windowChords = new ArrayList<>();

            for (int bar = startBar; bar <= endBar; bar++) {
                for (int idx : barChords.getOrDefault(bar, List.of())) {
                    windowPcs.addAll(getPitchClasses(chords.get(idx)));
                    windowIndices.add(idx);
                    windowChords.add(chords.get(idx));
                }
            }
            if (windowPcs.isEmpty()) continue;

            int localRoot = determineLocalKeyRoot(windowChords, keyRoot);

            String bestMode = null;
            double bestScore = -999.0;
            for (var entry : MODE_SCALES.entrySet()) {
                double score = scoreMode(windowPcs, localRoot, entry.getValue());
                if (score > bestScore) { bestScore = score; bestMode = entry.getKey(); }
            }

            if (localRoot != keyRoot) {
                for (var entry : MODE_SCALES.entrySet()) {
                    double score = scoreMode(windowPcs, keyRoot, entry.getValue());
                    if (score > bestScore) { bestScore = score; bestMode = entry.getKey(); }
                }
            }

            if (bestMode != null && bestScore >= THRESHOLD) {
                for (int idx : windowIndices) {
                    if (chords.get(idx).getModeSegment() == null) {
                        chords.get(idx).setModeSegment(bestMode);
                    }
                }
            }
        }

        for (ParsedChord c : chords) {
            if (c.getModeSegment() == null) c.setModeSegment(defaultMode);
        }
        return chords;
    }

    // ── helpers ──

    private Set<Integer> getPitchClasses(ParsedChord chord) {
        String q = chord.getNormalizedQuality() != null ? chord.getNormalizedQuality() : chord.getQuality();
        int[] intervals = DiatonicClassifier.QUALITY_INTERVALS.getOrDefault(q, new int[]{0, 4, 7});
        Set<Integer> pcs = new HashSet<>();
        for (int iv : intervals) pcs.add(mod12(chord.getRoot() + iv));
        return pcs;
    }

    private double scoreMode(Set<Integer> pitchClasses, int root, int[] scale) {
        if (pitchClasses.isEmpty()) return 0.0;
        Set<Integer> scalePcs = new HashSet<>();
        for (int s : scale) scalePcs.add(mod12(root + s));

        int overlap = 0, outside = 0;
        for (int pc : pitchClasses) {
            if (scalePcs.contains(pc)) overlap++;
            else outside++;
        }
        return (overlap - outside * 2.0) / Math.max(pitchClasses.size(), 1);
    }

    private int determineLocalKeyRoot(List<ParsedChord> window, int songKeyRoot) {
        for (int i = window.size() - 1; i >= 0; i--) {
            ParsedChord c = window.get(i);
            if (c.getTonicization() != null) {
                String tk = c.getTonicization().getTemporaryKey();
                if (tk != null) {
                    int pc = NoteUtils.parseNoteName(tk);
                    if (pc >= 0) return pc;
                }
            }
            for (var gm : c.getGroupMemberships()) {
                if ("ii-V-I".equals(gm.getGroupType())
                        && ("I".equals(gm.getRole()) || "I (iii substitute)".equals(gm.getRole()))) {
                    return c.getRoot();
                }
            }
        }
        return songKeyRoot;
    }
}

