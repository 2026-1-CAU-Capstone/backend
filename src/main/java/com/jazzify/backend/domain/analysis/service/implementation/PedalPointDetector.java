package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.model.PedalInfo;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import com.jazzify.backend.domain.analysis.util.NoteUtils.KeyInfo;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.jazzify.backend.domain.analysis.util.NoteUtils.mod12;
import static com.jazzify.backend.domain.analysis.util.NoteUtils.pcToNoteName;

/**
 * Layer 2‑10: Pedal Point Detector.
 */
@Component
public class PedalPointDetector {

    private static final int MIN_DURATION_BARS = 2;

    public List<ParsedChord> detect(List<ParsedChord> chords, String key) {
        if (chords.isEmpty()) return chords;
        KeyInfo ki = NoteUtils.parseKey(key);
        int keyRoot = ki.root();
        int n = chords.size();
        int i = 0;

        while (i < n) {
            int bass = effectiveBass(chords.get(i));
            int startIdx = i;
            int startBar = chords.get(i).getBar();
            int j = i + 1;

            while (j < n && effectiveBass(chords.get(j)) == bass) j++;

            int endIdx = j - 1;
            int endBar = chords.get(endIdx).getBar();
            int spanBars = endBar - startBar + 1;

            if (spanBars >= MIN_DURATION_BARS && endIdx - startIdx >= 1) {
                int bassInterval = mod12(bass - keyRoot);
                String pedalType;
                if (bassInterval == 0) pedalType = "tonic";
                else if (bassInterval == 7) pedalType = "dominant";
                else if (bassInterval == 5) pedalType = "subdominant";
                else pedalType = "on " + pcToNoteName(bass);

                for (int k = startIdx; k <= endIdx; k++) {
                    chords.get(k).setPedalInfo(PedalInfo.builder()
                            .pedalNote(bass)
                            .pedalNoteName(pcToNoteName(bass))
                            .pedalType(pedalType)
                            .isOverPedal(true)
                            .pedalStartBar(startBar)
                            .pedalEndBar(endBar)
                            .build());
                }
            }
            i = j;
        }
        return chords;
    }

    private int effectiveBass(ParsedChord c) {
        return c.getBass() != null ? c.getBass() : c.getRoot();
    }
}

