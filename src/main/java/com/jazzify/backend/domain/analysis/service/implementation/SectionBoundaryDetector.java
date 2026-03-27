package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import com.jazzify.backend.domain.analysis.util.NoteUtils.KeyInfo;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Layer 3: 섹션 경계 감지기.
 * 키와 모드가 동일한 연속 마디들을 하나의 섹션으로 묶어 곡의 구조를 파악한다.
 * 전조(modulation) 정보를 기반으로 섹션을 나누고, 짧은 섹션은 인접 섹션에 흡수한다.
 */
@Component
public class SectionBoundaryDetector {

    /**
     * 곡을 키와 모드 기반으로 섹션 단위로 분할한다.
     *
     * 알고리즘 (6단계):
     * Step 1: 각 마디의 유효 키 결정 (기본=원래 키, 전조 코드가 있는 마디=전조 키)
     * Step 2: 각 마디의 모드 결정 (마디 내 코드들의 modeSegment 중 최빈값)
     * Step 3: 키가 바뀌는 지점에서 섹션 분할
     * Step 4: 인접한 같은 키의 섹션 병합
     * Step 5: 2마디 이하 짧은 섹션을 이전 섹션에 흡수
     * Step 6: 각 섹션에 조성화(tonicization) 어노테이션 추가
     */
    public List<Map<String, Object>> detect(List<ParsedChord> chords, String key) {
        if (chords.isEmpty()) return List.of();

        KeyInfo ki = NoteUtils.parseKey(key);
        String defaultMode = ki.isMinor() ? "aeolian" : "ionian";
        int maxBar = chords.stream().mapToInt(ParsedChord::getBar).max().orElse(1);

        // Step 1: 마디별 유효 키 결정 (전조 코드가 있으면 해당 키로 변경)
        Map<Integer, String> barKey = new HashMap<>();
        Map<Integer, String> barType = new HashMap<>();
        for (int b = 1; b <= maxBar; b++) { barKey.put(b, key); barType.put(b, "original_key"); }

        for (ParsedChord c : chords) {
            if (c.getTonicization() != null && "modulation".equals(c.getTonicization().getType())) {
                barKey.put(c.getBar(), c.getTonicization().getTemporaryKey());
                barType.put(c.getBar(), "modulation");
            }
        }

        // Step 2: 마디별 모드 결정 (마디 내 코드들의 modeSegment 최빈값)
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

        // Step 3: 키가 바뀌는 지점에서 섹션 분할
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

        // Step 4: 인접한 같은 키의 섹션 병합
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

        // Step 5: 2마디 이하 짧은 섹션을 이전 섹션에 흡수 (의미 없는 짧은 "전조" 방지)
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

        // Step 6: 각 섹션에 조성화(tonicization) 어노테이션 추가
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

    /** 섹션 Map 객체를 빌드한다 */
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

    /** 지정된 마디 범위의 모드 최빈값을 구한다 */
    private String modeForSpan(int start, int end, Map<Integer, String> barMode, String defaultMode) {
        List<String> modes = new ArrayList<>();
        for (int b = start; b <= end; b++) {
            modes.add(barMode.getOrDefault(b, defaultMode));
        }
        return modes.isEmpty() ? defaultMode : mostFrequent(modes);
    }

    /** 문자열 리스트에서 가장 빈도가 높은 항목을 반환한다 */
    private String mostFrequent(List<String> list) {
        Map<String, Integer> freq = new HashMap<>();
        for (String s : list) freq.merge(s, 1, Integer::sum);
        return freq.entrySet().stream().max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(list.get(0));
    }
}

