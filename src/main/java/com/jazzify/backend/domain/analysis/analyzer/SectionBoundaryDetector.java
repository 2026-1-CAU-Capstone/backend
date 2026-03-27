package com.jazzify.backend.domain.analysis.analyzer;

import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.parser.NoteUtils;
import com.jazzify.backend.domain.analysis.parser.NoteUtils.KeyInfo;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Layer 3‑14: Section Boundary Detector.
 */
@Component
public class SectionBoundaryDetector {

    public List<Map<String, Object>> detect(List<ParsedChord> chords, String key) {
        if (chords.isEmpty()) return List.of();

        KeyInfo ki = NoteUtils.parseKey(key);
        String defaultMode = ki.isMinor() ? "aeolian" : "ionian";
        int maxBar = chords.stream().mapToInt(ParsedChord::getBar).max().orElse(1);

        // Step 1: bar → effective key
        Map<Integer, String> barKey = new HashMap<>();
        Map<Integer, String> barType = new HashMap<>();
        for (int b = 1; b <= maxBar; b++) { barKey.put(b, key); barType.put(b, "original_key"); }

        for (ParsedChord c : chords) {
            if (c.getTonicization() != null && "modulation".equals(c.getTonicization().getType())) {
                barKey.put(c.getBar(), c.getTonicization().getTemporaryKey());
                barType.put(c.getBar(), "modulation");
            }
        }

        // Step 2: bar → mode
        Map<Integer, List<ParsedChord>> barChords = new HashMap<>();
        for (ParsedChord c : chords) barChords.computeIfAbsent(c.getBar(), k -> new ArrayList<>()).add(c);

        Map<Integer, String> barMode = new HashMap<>();
        for (int b = 1; b <= maxBar; b++) {
            List<String> modes = new ArrayList<>();
            for (ParsedChord c : barChords.getOrDefault(b, List.of())) {
                if (c.getModeSegment() != null) modes.add(c.getModeSegment());
            }
            barMode.put(b, modes.isEmpty() ? defaultMode : mostFrequent(modes));
        }

        // Step 3: build sections
        List<Map<String, Object>> sections = new ArrayList<>();
        int currentStart = 1;
        String currentKey = barKey.get(1);
        String currentType = barType.get(1);

        for (int b = 2; b <= maxBar; b++) {
            if (!barKey.get(b).equals(currentKey)) {
                sections.add(buildSection(currentStart, b - 1, currentKey, currentType, barMode, defaultMode));
                currentStart = b;
                currentKey = barKey.get(b);
                currentType = barType.get(b);
            }
        }
        sections.add(buildSection(currentStart, maxBar, currentKey, currentType, barMode, defaultMode));

        // Step 4: merge adjacent same‑key
        List<Map<String, Object>> merged = new ArrayList<>();
        for (Map<String, Object> s : sections) {
            if (!merged.isEmpty() && merged.getLast().get("key").equals(s.get("key"))) {
                Map<String, Object> last = merged.getLast();
                last.put("end_bar", s.get("end_bar"));
                last.put("mode", modeForSpan((int) last.get("start_bar"), (int) s.get("end_bar"), barMode, defaultMode));
            } else {
                merged.add(new LinkedHashMap<>(s));
            }
        }

        // Step 5: absorb short sections (≤2 bars)
        if (merged.size() > 1) {
            List<Map<String, Object>> cleaned = new ArrayList<>();
            for (Map<String, Object> s : merged) {
                int span = (int) s.get("end_bar") - (int) s.get("start_bar") + 1;
                if (span <= 2 && !cleaned.isEmpty()) {
                    cleaned.getLast().put("end_bar", s.get("end_bar"));
                } else {
                    cleaned.add(s);
                }
            }
            merged = cleaned;
        }

        // Step 6: tonicization annotations
        for (Map<String, Object> s : merged) {
            Set<String> tonics = new TreeSet<>();
            for (ParsedChord c : chords) {
                if (c.getBar() >= (int) s.get("start_bar") && c.getBar() <= (int) s.get("end_bar")
                        && c.getTonicization() != null
                        && "tonicization".equals(c.getTonicization().getType())) {
                    tonics.add(c.getTonicization().getTemporaryKey());
                }
            }
            if (!tonics.isEmpty()) s.put("tonicizations", new ArrayList<>(tonics));
        }

        return merged;
    }

    // ── helpers ──

    private Map<String, Object> buildSection(int start, int end, String key, String type,
                                              Map<Integer, String> barMode, String defaultMode) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("start_bar", start);
        s.put("end_bar", end);
        s.put("key", key);
        s.put("mode", modeForSpan(start, end, barMode, defaultMode));
        s.put("type", type);
        return s;
    }

    private String modeForSpan(int start, int end, Map<Integer, String> barMode, String defaultMode) {
        List<String> modes = new ArrayList<>();
        for (int b = start; b <= end; b++) {
            modes.add(barMode.getOrDefault(b, defaultMode));
        }
        return modes.isEmpty() ? defaultMode : mostFrequent(modes);
    }

    private String mostFrequent(List<String> list) {
        Map<String, Integer> freq = new HashMap<>();
        for (String s : list) freq.merge(s, 1, Integer::sum);
        return freq.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(list.get(0));
    }
}

