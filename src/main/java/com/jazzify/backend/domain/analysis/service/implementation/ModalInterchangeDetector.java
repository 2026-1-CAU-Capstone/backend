package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.config.AnalysisConfigData;
import com.jazzify.backend.domain.analysis.config.AnalysisConfigData.*;
import com.jazzify.backend.domain.analysis.model.FunctionEntry;
import com.jazzify.backend.domain.analysis.model.ModalInterchangeInfo;
import com.jazzify.backend.domain.analysis.model.ModalInterchangeInfo.ModalInterchangeMatch;
import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import com.jazzify.backend.domain.analysis.util.NoteUtils.KeyInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jazzify.backend.domain.analysis.util.NoteUtils.mod12;

/**
 * Layer 3‑11: Modal Interchange Detector.
 */
@Component
@RequiredArgsConstructor
public class ModalInterchangeDetector {

    private final AnalysisConfigData configData;

    public List<ParsedChord> detect(List<ParsedChord> chords, String key) {
        KeyInfo ki = NoteUtils.parseKey(key);
        if (!ki.isMajor()) return chords; // only detect for major keys

        int keyRoot = ki.root();
        Map<String, ModeInterchangeData> miConfig = configData.getModalInterchange();

        for (ParsedChord chord : chords) {
            if (Boolean.TRUE.equals(chord.getIsDiatonic())) continue;
            if (chord.getSecondaryDominant() != null) continue;

            String nq = chord.getNormalizedQuality() != null ? chord.getNormalizedQuality() : chord.getQuality();
            int chordInterval = mod12(chord.getRoot() - keyRoot);

            List<ModalInterchangeMatch> matches = new ArrayList<>();
            for (var entry : miConfig.entrySet()) {
                String modeName = entry.getKey();
                ModeInterchangeData modeData = entry.getValue();
                for (DegreeInfo di : modeData.availableDegrees()) {
                    if (di.interval() == chordInterval && di.quality().equals(nq)) {
                        boolean isCommon = modeData.commonBorrows().stream()
                                .anyMatch(cb -> cb.degreeLabel().equals(di.degreeLabel()));
                        matches.add(ModalInterchangeMatch.builder()
                                .sourceMode(modeName)
                                .borrowedDegree(di.degreeLabel())
                                .isCommonBorrow(isCommon)
                                .build());
                    }
                }
            }

            if (!matches.isEmpty()) {
                ModalInterchangeMatch best = matches.stream()
                        .filter(ModalInterchangeMatch::isCommonBorrow)
                        .findFirst()
                        .orElse(matches.get(0));

                chord.setModalInterchange(ModalInterchangeInfo.builder()
                        .sourceMode(best.getSourceMode())
                        .borrowedDegree(best.getBorrowedDegree())
                        .isCommonBorrow(best.isCommonBorrow())
                        .allPossibleSources(matches)
                        .build());

                if (chord.getFunctions().isEmpty()) {
                    chord.setFunctions(List.of(new FunctionEntry("modal_interchange",
                            best.isCommonBorrow() ? 0.7 : 0.5,
                            "Borrowed from " + best.getSourceMode() + " mode (" + best.getBorrowedDegree() + ")")));
                }
            }
        }
        return chords;
    }
}

