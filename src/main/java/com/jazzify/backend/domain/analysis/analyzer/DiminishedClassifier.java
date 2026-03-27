package com.jazzify.backend.domain.analysis.analyzer;

import com.jazzify.backend.domain.analysis.model.AmbiguityFlag;
import com.jazzify.backend.domain.analysis.model.FunctionEntry;
import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.model.SecondaryDominantInfo;
import com.jazzify.backend.domain.analysis.parser.NoteUtils;
import com.jazzify.backend.domain.analysis.parser.NoteUtils.KeyInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.jazzify.backend.domain.analysis.parser.NoteUtils.mod12;
import static com.jazzify.backend.domain.analysis.parser.NoteUtils.pcToNoteName;

/**
 * Layer 2‑7: Diminished Chord Classifier.
 */
@Component
public class DiminishedClassifier {

    private static final Set<String> DIM = Set.of("dim7", "dim");
    private static final Map<Integer, String> TARGET_DEGREES = Map.of(
            0, "I", 2, "ii", 4, "iii", 5, "IV", 7, "V", 9, "vi", 11, "vii"
    );

    public List<ParsedChord> detect(List<ParsedChord> chords, String key) {
        KeyInfo ki = NoteUtils.parseKey(key);
        int keyRoot = ki.root();
        int n = chords.size();

        for (int i = 0; i < n; i++) {
            ParsedChord chord = chords.get(i);
            String nq = nq(chord);
            if (!DIM.contains(nq)) continue;

            ParsedChord prev = i > 0 ? chords.get(i - 1) : null;
            ParsedChord next = i + 1 < n ? chords.get(i + 1) : null;
            boolean classified = false;

            // Auxiliary
            if (prev != null && next != null && prev.getRoot() == next.getRoot()) {
                int diff = mod12(chord.getRoot() - prev.getRoot());
                if (diff == 0 || diff == 1) {
                    chord.setDiminishedFunction("auxiliary");
                    classified = true;
                }
            }

            // Passing
            if (!classified && prev != null && next != null) {
                boolean asc = mod12(chord.getRoot() - prev.getRoot()) == 1
                        && mod12(next.getRoot() - chord.getRoot()) == 1;
                boolean desc = mod12(prev.getRoot() - chord.getRoot()) == 1
                        && mod12(chord.getRoot() - next.getRoot()) == 1;
                if (asc || desc) {
                    chord.setDiminishedFunction("passing");
                    classified = true;
                }
            }

            // Dominant function: root + 1 == next root → rootless V7b9
            if (!classified && next != null) {
                if (mod12(chord.getRoot() + 1) == next.getRoot()) {
                    int impliedDomRoot = mod12(chord.getRoot() - 4);
                    String impliedDomName = pcToNoteName(impliedDomRoot);
                    chord.setDiminishedFunction("dominant_function");
                    classified = true;

                    int targetInterval = mod12(next.getRoot() - keyRoot);
                    String targetDeg = TARGET_DEGREES.getOrDefault(targetInterval,
                            "(" + pcToNoteName(next.getRoot()) + ")");

                    chord.setSecondaryDominant(SecondaryDominantInfo.builder()
                            .type("V/" + targetDeg + " (as dim7)")
                            .impliedDominant(impliedDomName + "7b9")
                            .targetDegree(targetDeg)
                            .targetChord(next.getOriginalSymbol())
                            .resolved(true)
                            .originPosition(Map.of("bar", chord.getBar(), "beat", chord.getBeat()))
                            .build());

                    chord.setFunctions(List.of(new FunctionEntry("D", 0.8,
                            "Diminished chord functioning as " + impliedDomName + "7b9 (V/" + targetDeg + ")")));
                }
            }

            if (!classified) {
                chord.setDiminishedFunction("unknown");
                chord.getAmbiguityFlags().add(AmbiguityFlag.builder()
                        .aspect("diminished_function")
                        .interpretations(List.of("passing", "auxiliary", "dominant_function"))
                        .contextNeeded(true).build());
            }
        }
        return chords;
    }

    private static String nq(ParsedChord c) {
        return c.getNormalizedQuality() != null ? c.getNormalizedQuality() : c.getQuality();
    }
}

