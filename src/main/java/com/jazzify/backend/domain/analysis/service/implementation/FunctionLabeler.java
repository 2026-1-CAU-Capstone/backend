package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.config.AnalysisConfigData;
import com.jazzify.backend.domain.analysis.model.AmbiguityFlag;
import com.jazzify.backend.domain.analysis.model.FunctionEntry;
import com.jazzify.backend.domain.analysis.model.GroupMembership;
import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import com.jazzify.backend.domain.analysis.util.NoteUtils.KeyInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Layer 1‑2: Function Labeler.
 * Assigns T / SD / D functions based on scale degree.
 */
@Component
@RequiredArgsConstructor
public class FunctionLabeler {

    private final AnalysisConfigData configData;

    public List<ParsedChord> label(List<ParsedChord> chords, String key) {
        Map<String, Map<String, List<FunctionEntry>>> funcMap = configData.getFunctionMap();
        KeyInfo ki = NoteUtils.parseKey(key);

        String keySection = ki.isMajor() ? "major_key" : "minor_key";
        Map<String, List<FunctionEntry>> degreeMap = funcMap.getOrDefault(keySection, Map.of());
        Map<String, List<FunctionEntry>> chromaticMap = funcMap.getOrDefault("chromatic_degrees", Map.of());

        for (ParsedChord chord : chords) {
            if (chord.getDegree() == null) continue;
            String lookup = normalizeDegreeForLookup(chord.getDegree());

            if (Boolean.TRUE.equals(chord.getIsDiatonic())) {
                List<FunctionEntry> funcs = findFunctions(degreeMap, lookup);
                if (funcs != null) chord.setFunctions(copyFunctions(funcs));
            } else {
                List<FunctionEntry> funcs = findFunctions(chromaticMap, lookup);
                if (funcs != null) {
                    chord.setFunctions(copyFunctions(funcs));
                } else {
                    funcs = findFunctions(degreeMap, lookup);
                    if (funcs != null) chord.setFunctions(copyFunctions(funcs));
                }
                if (chord.getFunctions().isEmpty()) {
                    chord.getAmbiguityFlags().add(AmbiguityFlag.builder()
                            .aspect("function")
                            .interpretations(List.of("unknown - awaiting contextual analysis"))
                            .contextNeeded(true)
                            .build());
                }
            }
        }
        return chords;
    }

    /**
     * Post‑hoc pass: assign functions from ii‑V‑I group roles.
     */
    public List<ParsedChord> labelFromGroups(List<ParsedChord> chords) {
        Map<String, List<FunctionEntry>> roleFunctions = Map.of(
                "ii", List.of(new FunctionEntry("SD", 0.9, "ii role in ii-V-I group")),
                "iv (backdoor)", List.of(new FunctionEntry("SD", 0.8, "iv role in backdoor ii-V-I")),
                "V", List.of(new FunctionEntry("D", 1.0, "V role in ii-V-I group")),
                "V (tritone sub bII7)", List.of(new FunctionEntry("D", 0.9, "Tritone sub V in ii-V-I")),
                "V (backdoor bVII7)", List.of(new FunctionEntry("D", 0.8, "Backdoor V in ii-V-I")),
                "V (resolved from sus4)", List.of(new FunctionEntry("D", 1.0, "V (sus resolved) in ii-V-I")),
                "I", List.of(new FunctionEntry("T", 1.0, "I role in ii-V-I group")),
                "I (iii substitute)", List.of(new FunctionEntry("T", 0.7, "iii substitute for I in ii-V-I"))
        );

        for (ParsedChord chord : chords) {
            if (!chord.getFunctions().isEmpty()) continue;
            for (GroupMembership gm : chord.getGroupMemberships()) {
                if ("ii-V-I".equals(gm.getGroupType())) {
                    List<FunctionEntry> funcs = roleFunctions.get(gm.getRole());
                    if (funcs != null) {
                        chord.setFunctions(copyFunctions(funcs));
                        chord.getAmbiguityFlags().removeIf(a -> "function".equals(a.getAspect()));
                        break;
                    }
                }
            }
        }
        return chords;
    }

    // ── helpers ──

    private String normalizeDegreeForLookup(String degree) {
        if (degree == null) return "";
        return degree.replaceAll("[°+]+$", "");
    }

    private List<FunctionEntry> findFunctions(Map<String, List<FunctionEntry>> map, String lookup) {
        List<FunctionEntry> result = map.get(lookup);
        if (result != null) return result;
        for (var entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(lookup)) return entry.getValue();
        }
        return null;
    }

    private List<FunctionEntry> copyFunctions(List<FunctionEntry> src) {
        List<FunctionEntry> copy = new ArrayList<>();
        for (FunctionEntry f : src) {
            copy.add(new FunctionEntry(f.getFunction(), f.getConfidence(), f.getNote()));
        }
        return copy;
    }
}

