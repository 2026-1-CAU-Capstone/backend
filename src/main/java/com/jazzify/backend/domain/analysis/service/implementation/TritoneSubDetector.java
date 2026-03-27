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
 * Layer 2: 트라이톤 대리 감지기.
 * ii-V-I 감지에서 놓친 독립적인 트라이톤 대리 코드를 추가로 감지한다.
 * dom7 코드가 반음 아래로 해결되면(interval=11) 트라이톤 대리로 판단한다.
 */
@Component
public class TritoneSubDetector {

    private static final Set<String> DOM7 = Set.of("dom7", "dom7sus4", "aug7");

    /**
     * 트라이톤 대리를 감지하여 D_substitute 기능과 모호성 플래그를 추가한다.
     *
     * 알고리즘:
     * 1) ii-V-I에서 이미 트라이톤으로 태깅된 코드 위치를 수집 (중복 방지)
     * 2) 연속된 두 코드에서: 현재가 dom7이고, 다음 코드로 반음 하행(interval=11)하면
     *    → 트라이톤 대리로 판단
     * 3) 원래 V 근음 = 현재 근음 + 트라이톤(6반음) → D_substitute 기능 추가
     */
    @SuppressWarnings("unchecked")
    public List<ParsedChord> detect(List<ParsedChord> chords, List<Map<String, Object>> groups) {
        // 이미 ii-V-I에서 트라이톤으로 태깅된 위치 수집 (중복 감지 방지)
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
            if (!DOM7.contains(nq)) continue;                                    // dom7만 대상
            if (alreadyTagged.contains(chord.getBar() + ":" + chord.getBeat())) continue; // 이미 처리됨

            // 다음 코드로 반음 하행(interval=11)이면 트라이톤 대리
            int interval = mod12(next.getRoot() - chord.getRoot());
            if (interval == 11) {
                // 원래 V의 근음 계산: 현재 근음에서 트라이톤(6반음) 위
                int origVRoot = mod12(chord.getRoot() + 6);
                String origVName = pcToNoteName(origVRoot);

                // D_substitute 기능 추가 (중복 확인)
                boolean hasDSub = chord.getFunctions().stream()
                        .anyMatch(f -> "D_substitute".equals(f.getFunction()));
                if (!hasDSub) {
                    chord.getFunctions().add(new FunctionEntry("D_substitute", 0.8,
                            "Tritone sub of " + origVName + "7"));
                }
                // 모호성 플래그: bII7 트라이톤 대리 vs 프리지안 bII vs 반음계적 접근
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
