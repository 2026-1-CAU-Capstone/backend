package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.model.TonicizationInfo;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import com.jazzify.backend.domain.analysis.util.NoteUtils.KeyInfo;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Layer 3: 조성화 vs 전조 감지기.
 * ii-V-I 그룹 정보를 기반으로, 원래 키가 아닌 타겟 키로의 진행이
 * 일시적 조성화(tonicization)인지 완전한 전조(modulation)인지 판별한다.
 */
@Component
public class TonicizationModulationDetector {

    /** 전조로 판별하기 위한 최소 완전 케이던스 수 */
    private static final int MODULATION_MIN_CADENCES = 2;
    /** 전조로 판별하기 위한 최소 마디 수 */
    private static final int MODULATION_MIN_BARS = 6;

    /**
     * 조성화/전조를 판별하여 관련 코드에 TonicizationInfo를 설정한다.
     *
     * 알고리즘:
     * 1) ii-V-I 그룹 중 원래 키가 아닌 타겟 키를 가진 그룹만 필터링
     * 2) 같은 타겟 키를 가진 그룹들을 모아서 분석:
     *    - 다이어토닉 타겟이면 → 조성화 (일시적 방문)
     *    - 불완전 케이던스만 있으면 → 약한 조성화 (확신도 0.4)
     *    - 완전 케이던스 ≥2 AND 기간 ≥6마디 → 전조
     *    - 그 외 → 조성화
     * 3) 해당 그룹 멤버 위치의 코드들에 TonicizationInfo 기록
     */
    @SuppressWarnings("unchecked")
    public List<ParsedChord> detect(List<ParsedChord> chords, String key, List<Map<String, Object>> groups) {
        KeyInfo ki = NoteUtils.parseKey(key);
        int keyRoot = ki.root();

        // 원래 키가 아닌 타겟을 가진 그룹만 필터링
        List<Map<String, Object>> nonTonicGroups = new ArrayList<>();
        for (Map<String, Object> g : groups) {
            String targetKey = (String) g.get("target_key");
            int tkPc = NoteUtils.parseNoteName(targetKey);
            if (tkPc >= 0 && tkPc != keyRoot) nonTonicGroups.add(g);
        }
        if (nonTonicGroups.isEmpty()) return chords;

        // 같은 타겟 키끼리 그룹화
        Map<String, List<Map<String, Object>>> keyEvents = new LinkedHashMap<>();
        for (Map<String, Object> g : nonTonicGroups) {
            String tk = (String) g.getOrDefault("target_key", "unknown");
            keyEvents.computeIfAbsent(tk, k -> new ArrayList<>()).add(g);
        }

        // 타겟 키별로 조성화/전조 판별
        for (var entry : keyEvents.entrySet()) {
            String targetKeyName = entry.getKey();
            List<Map<String, Object>> events = entry.getValue();

            // 멤버 위치와 통계 수집
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

            // ── 판별 규칙 ──
            String tonicType;
            double confidence;
            if (anyDiatonicTarget) {
                // 다이어토닉 타겟 → 조성화 (원래 키 내 ii-V-I)
                tonicType = "tonicization";
                confidence = Math.min(0.8, 0.5 + 0.15 * nCadences);
            } else if (anyIncomplete && nComplete == 0) {
                // 불완전 케이던스만 → 약한 조성화
                tonicType = "tonicization";
                confidence = 0.4;
            } else if (nComplete >= MODULATION_MIN_CADENCES && span >= MODULATION_MIN_BARS) {
                // 완전 케이던스 ≥2 AND 기간 ≥6마디 → 전조!
                tonicType = "modulation";
                confidence = Math.min(0.9, 0.5 + 0.1 * nComplete + 0.03 * span);
            } else {
                // 그 외 → 조성화
                tonicType = "tonicization";
                confidence = Math.min(0.8, 0.4 + 0.2 * nCadences);
            }

            // 증거(evidence) 문자열 생성
            List<String> evidence = new ArrayList<>();
            for (Map<String, Object> g : events) {
                List<Map<String, Object>> members = (List<Map<String, Object>>) g.get("members");
                if (members == null) continue;
                int minB = members.stream().mapToInt(m -> ((Number) m.get("bar")).intValue()).min().orElse(0);
                int maxB = members.stream().mapToInt(m -> ((Number) m.get("bar")).intValue()).max().orElse(0);
                String variant = (String) g.getOrDefault("variant", "");
                evidence.add(g.get("group_type") + "(" + variant + ") to " + targetKeyName + " at bars " + minB + "-" + maxB);
            }

            // 해당 위치의 코드들에 TonicizationInfo 설정
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

