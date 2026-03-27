package com.jazzify.backend.domain.analysis.analyzer;

import com.jazzify.backend.domain.analysis.model.ChromaticApproachInfo;
import com.jazzify.backend.domain.analysis.model.FunctionEntry;
import com.jazzify.backend.domain.analysis.model.ParsedChord;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.jazzify.backend.domain.analysis.parser.NoteUtils.mod12;

/**
 * Layer 2‑8: Chromatic Approach Detector.
 */
@Component
public class ChromaticApproachDetector {

    public List<ParsedChord> detect(List<ParsedChord> chords) {
        int n = chords.size();
        for (int i = 0; i < n - 1; i++) {
            ParsedChord chord = chords.get(i);
            ParsedChord next = chords.get(i + 1);

            if (Boolean.TRUE.equals(chord.getIsDiatonic())) continue;
            if (chord.getSecondaryDominant() != null) continue;
            if (chord.getFunctions().stream().anyMatch(f ->
                    f.getNote() != null && f.getNote().contains("tritone"))) continue;

            int interval = mod12(next.getRoot() - chord.getRoot());
            String direction = null;
            if (interval == 1) direction = "below";
            else if (interval == 11) direction = "above";
            if (direction == null) continue;

            String nq = nq(chord);
            String nextNq = nq(next);
            boolean qualityMatch = nq.equals(nextNq);

            chord.setChromaticApproach(ChromaticApproachInfo.builder()
                    .target(next.getOriginalSymbol())
                    .targetBar(next.getBar())
                    .targetBeat(next.getBeat())
                    .direction(direction)
                    .qualityMatch(qualityMatch)
                    .build());

            if (chord.getFunctions().isEmpty()) {
                chord.setFunctions(List.of(new FunctionEntry("chromatic_approach",
                        qualityMatch ? 0.7 : 0.5,
                        "Chromatic approach from " + direction + " to " + next.getOriginalSymbol())));
            }
        }
        return chords;
    }

    private static String nq(ParsedChord c) {
        return c.getNormalizedQuality() != null ? c.getNormalizedQuality() : c.getQuality();
    }
}

