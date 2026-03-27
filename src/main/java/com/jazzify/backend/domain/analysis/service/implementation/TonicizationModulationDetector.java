package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.model.TonicizationInfo;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import com.jazzify.backend.domain.analysis.util.NoteUtils.KeyInfo;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Layer 3‑13: Tonicization vs Modulation Detector.
 */
@Component
public class TonicizationModulationDetector {

    private static final int MODULATION_MIN_CADENCES = 2;
    private static final int MODULATION_MIN_BARS = 6;

    @SuppressWarnings("unchecked")
    public List<ParsedChord> detect(List<ParsedChord> chords, String key, List<Map<String, Object>> groups) {
        KeyInfo ki = NoteUtils.parseKey(key);
        int keyRoot = ki.root();

        List<Map<String, Object>> nonTonicGroups = new ArrayList<>();
        for (Map<String, Object> g : groups) {
            String targetKey = (String) g.get("target_key");
            int tkPc = NoteUtils.parseNoteName(targetKey);
            if (tkPc >= 0 && tkPc != keyRoot) nonTonicGroups.add(g);
        }
        if (nonTonicGroups.isEmpty()) return chords;

        // Group by target key
        Map<String, List<Map<String, Object>>> keyEvents = new LinkedHashMap<>();
        for (Map<String, Object> g : nonTonicGroups) {
            String tk = (String) g.getOrDefault("target_key", "unknown");
            keyEvents.computeIfAbsent(tk, k -> new ArrayList<>()).add(g);
        }

        for (var entry : keyEvents.entrySet()) {
            String targetKeyName = entry.getKey();
            List<Map<String, Object>> events = entry.getValue();

            Set<String> memberPositions = new HashSet<>();
            List<Integer> allBars = new ArrayList<>();
            boolean anyDiatonicTarget = events.stream()
                    .anyMatch(g -> Boolean.TRUE.equals(g.get("is_diatonic_target")));
            boolean anyIncomplete = events.stream()
                    .anyMatch(g -> "incomplete".equals(g.get("variant")));
            long nComplete = events.stream()
                    .filter(g -> !"incomplete".equals(g.get("variant"))).count();

            for (Map<String, Object> g : events) {
                List<Map<String, Object>> members = (List<Map<String, Object>>) g.get("members");
                if (members == null) continue;
                for (Map<String, Object> m : members) {
                    int bar = ((Number) m.get("bar")).intValue();
                    double beat = ((Number) m.get("beat")).doubleValue();
                    memberPositions.add(bar + ":" + beat);
                    allBars.add(bar);
                }
            }
            if (allBars.isEmpty()) continue;

            int startBar = Collections.min(allBars);
            int endBar = Collections.max(allBars);
            int span = endBar - startBar + 1;
            int nCadences = events.size();

            String tonicType;
            double confidence;
            if (anyDiatonicTarget) {
                tonicType = "tonicization";
                confidence = Math.min(0.8, 0.5 + 0.15 * nCadences);
            } else if (anyIncomplete && nComplete == 0) {
                tonicType = "tonicization";
                confidence = 0.4;
            } else if (nComplete >= MODULATION_MIN_CADENCES && span >= MODULATION_MIN_BARS) {
                tonicType = "modulation";
                confidence = Math.min(0.9, 0.5 + 0.1 * nComplete + 0.03 * span);
            } else {
                tonicType = "tonicization";
                confidence = Math.min(0.8, 0.4 + 0.2 * nCadences);
            }

            List<String> evidence = new ArrayList<>();
            for (Map<String, Object> g : events) {
                List<Map<String, Object>> members = (List<Map<String, Object>>) g.get("members");
                if (members == null) continue;
                int minB = members.stream().mapToInt(m -> ((Number) m.get("bar")).intValue()).min().orElse(0);
                int maxB = members.stream().mapToInt(m -> ((Number) m.get("bar")).intValue()).max().orElse(0);
                String variant = (String) g.getOrDefault("variant", "");
                evidence.add(g.get("group_type") + "(" + variant + ") to " + targetKeyName + " at bars " + minB + "-" + maxB);
            }

            double finalConf = Math.round(confidence * 100.0) / 100.0;
            for (ParsedChord chord : chords) {
                String pos = chord.getBar() + ":" + chord.getBeat();
                if (memberPositions.contains(pos)) {
                    chord.setTonicization(TonicizationInfo.builder()
                            .type(tonicType)
                            .temporaryKey(targetKeyName)
                            .startBar(startBar)
                            .endBar(endBar)
                            .evidence(evidence)
                            .confidence(finalConf)
                            .build());
                }
            }
        }
        return chords;
    }
}

