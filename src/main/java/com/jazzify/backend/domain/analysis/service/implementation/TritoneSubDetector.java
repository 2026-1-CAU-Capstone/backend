package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.AmbiguityFlag;
import com.jazzify.backend.domain.analysis.model.FunctionEntry;
import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.jazzify.backend.domain.analysis.util.NoteUtils.mod12;
import static com.jazzify.backend.domain.analysis.util.NoteUtils.pcToNoteName;

/**
 * Layer 2‑5: Tritone Substitution Detector.
 */
@Component
public class TritoneSubDetector {

    private static final Set<String> DOM7 = Set.of("dom7", "dom7sus4", "aug7");

    @SuppressWarnings("unchecked")
    public List<ParsedChord> detect(List<ParsedChord> chords, List<Map<String, Object>> groups) {
        Set<String> alreadyTagged = new HashSet<>();
        for (Map<String, Object> g : groups) {
            String variant = (String) g.getOrDefault("variant", "");
            if (variant.contains("tritone_sub")) {
                List<Map<String, Object>> members = (List<Map<String, Object>>) g.get("members");
                if (members != null) {
                    for (Map<String, Object> m : members) {
                        String role = (String) m.getOrDefault("role", "");
                        if (role.toLowerCase().contains("tritone")) {
                            alreadyTagged.add(m.get("bar") + ":" + m.get("beat"));
                        }
                    }
                }
            }
        }

        int n = chords.size();
        for (int i = 0; i < n - 1; i++) {
            ParsedChord chord = chords.get(i);
            ParsedChord next = chords.get(i + 1);
            String nq = nq(chord);
            if (!DOM7.contains(nq)) continue;
            if (alreadyTagged.contains(chord.getBar() + ":" + chord.getBeat())) continue;

            int interval = mod12(next.getRoot() - chord.getRoot());
            if (interval == 11) {
                int origVRoot = mod12(chord.getRoot() + 6);
                String origVName = pcToNoteName(origVRoot);

                boolean hasDSub = chord.getFunctions().stream()
                        .anyMatch(f -> "D_substitute".equals(f.getFunction()));
                if (!hasDSub) {
                    chord.getFunctions().add(new FunctionEntry("D_substitute", 0.8,
                            "Tritone sub of " + origVName + "7"));
                }
                chord.getAmbiguityFlags().add(AmbiguityFlag.builder()
                        .aspect("tritone_substitution")
                        .interpretations(List.of(
                                "bII7 tritone sub of " + origVName + "7",
                                "Could be Phrygian bII or chromatic approach if no preceding ii"))
                        .contextNeeded(false)
                        .build());
            }
        }
        return chords;
    }

    private static String nq(ParsedChord c) {
        return c.getNormalizedQuality() != null ? c.getNormalizedQuality() : c.getQuality();
    }
}

