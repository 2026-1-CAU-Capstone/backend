package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.FunctionEntry;
import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.model.SecondaryDominantInfo;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import com.jazzify.backend.domain.analysis.util.NoteUtils.KeyInfo;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.jazzify.backend.domain.analysis.util.NoteUtils.mod12;
import static com.jazzify.backend.domain.analysis.util.NoteUtils.pcToNoteName;

/**
 * Layer 2‑6: Secondary Dominant Detector.
 */
@Component
public class SecondaryDominantDetector {

    private static final Set<String> DOM7 = Set.of("dom7", "dom7sus4", "aug7");

    private static final Map<Integer, String> MAJOR_DEGREE_ROOTS = Map.of(
            0, "I", 2, "ii", 4, "iii", 5, "IV", 7, "V", 9, "vi", 11, "vii"
    );
    private static final Map<Integer, String> MINOR_DEGREE_ROOTS = Map.of(
            0, "i", 2, "ii", 3, "bIII", 5, "iv", 7, "V", 8, "bVI", 10, "bVII"
    );

    public List<ParsedChord> detect(List<ParsedChord> chords, String key) {
        KeyInfo ki = NoteUtils.parseKey(key);
        int keyRoot = ki.root();
        Map<Integer, String> degreeMap = ki.isMajor() ? MAJOR_DEGREE_ROOTS : MINOR_DEGREE_ROOTS;

        int n = chords.size();
        for (int i = 0; i < n; i++) {
            ParsedChord chord = chords.get(i);
            String nq = nq(chord);
            if (!DOM7.contains(nq)) continue;
            if (Boolean.TRUE.equals(chord.getIsDiatonic())
                    && ("V".equals(chord.getDegree()) || "v".equals(chord.getDegree()))) continue;

            int targetRoot = mod12(chord.getRoot() + 5);
            int targetInterval = mod12(targetRoot - keyRoot);
            String targetDegree = degreeMap.getOrDefault(targetInterval, "(" + pcToNoteName(targetRoot) + ")");

            boolean resolved = false;
            String targetChordSymbol = null;
            if (i + 1 < n) {
                ParsedChord next = chords.get(i + 1);
                if (next.getRoot() == targetRoot) {
                    resolved = true;
                    targetChordSymbol = next.getOriginalSymbol();
                }
            }

            String secDomType = "V/" + targetDegree;
            chord.setSecondaryDominant(SecondaryDominantInfo.builder()
                    .type(secDomType)
                    .targetDegree(targetDegree)
                    .targetChord(targetChordSymbol != null ? targetChordSymbol : pcToNoteName(targetRoot))
                    .resolved(resolved)
                    .originPosition(Map.of("bar", chord.getBar(), "beat", chord.getBeat()))
                    .build());

            chord.setFunctions(List.of(new FunctionEntry("D",
                    resolved ? 0.9 : 0.6,
                    "Secondary dominant " + secDomType + (resolved ? " (resolved)" : " (unresolved)"))));
        }
        return chords;
    }

    private static String nq(ParsedChord c) {
        return c.getNormalizedQuality() != null ? c.getNormalizedQuality() : c.getQuality();
    }
}

