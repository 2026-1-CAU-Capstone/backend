package com.jazzify.backend.domain.analysis.analyzer;

import com.jazzify.backend.domain.analysis.model.DeceptiveResolutionInfo;
import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.parser.NoteUtils;
import com.jazzify.backend.domain.analysis.parser.NoteUtils.KeyInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.jazzify.backend.domain.analysis.parser.NoteUtils.mod12;
import static com.jazzify.backend.domain.analysis.parser.NoteUtils.pcToNoteName;

/**
 * Layer 2‑9: Deceptive Resolution Detector.
 */
@Component
public class DeceptiveResolutionDetector {

    private static final Set<String> DOM7 = Set.of("dom7", "dom7sus4", "aug7");

    // interval from expected I → (degree label, isCommon)
    private record DeceptiveTarget(String degree, boolean common) {}
    private static final Map<Integer, DeceptiveTarget> COMMON_DECEPTIVE = Map.of(
            9, new DeceptiveTarget("vi", true),
            8, new DeceptiveTarget("bVI", true),
            5, new DeceptiveTarget("IV", true),
            4, new DeceptiveTarget("iii", true),
            10, new DeceptiveTarget("bVII", true),
            3, new DeceptiveTarget("bIII", false)
    );

    public List<ParsedChord> detect(List<ParsedChord> chords, String key) {
        NoteUtils.parseKey(key); // validate
        int n = chords.size();

        for (int i = 0; i < n - 1; i++) {
            ParsedChord chord = chords.get(i);
            String nq = nq(chord);
            if (!DOM7.contains(nq)) continue;

            ParsedChord next = chords.get(i + 1);
            int expectedRoot = mod12(chord.getRoot() + 5);
            if (next.getRoot() == expectedRoot) continue;

            // Skip backdoor tagged
            boolean isBackdoor = chord.getGroupMemberships().stream()
                    .anyMatch(gm -> gm.getVariant() != null && gm.getVariant().contains("backdoor"));
            if (isBackdoor) continue;

            int intervalFromExpected = mod12(next.getRoot() - expectedRoot);
            String expectedName = pcToNoteName(expectedRoot);

            DeceptiveTarget dt = COMMON_DECEPTIVE.get(intervalFromExpected);
            String actualDegree;
            boolean common;
            if (dt != null) {
                actualDegree = dt.degree;
                common = dt.common;
            } else {
                actualDegree = "(" + pcToNoteName(next.getRoot()) + ")";
                common = false;
            }

            chord.setDeceptiveResolution(DeceptiveResolutionInfo.builder()
                    .dominantChord(chord.getOriginalSymbol())
                    .expectedResolution(expectedName + "maj7")
                    .actualResolution(next.getOriginalSymbol())
                    .actualDegree(actualDegree)
                    .commonPattern(common)
                    .build());
        }
        return chords;
    }

    private static String nq(ParsedChord c) {
        return c.getNormalizedQuality() != null ? c.getNormalizedQuality() : c.getQuality();
    }
}

