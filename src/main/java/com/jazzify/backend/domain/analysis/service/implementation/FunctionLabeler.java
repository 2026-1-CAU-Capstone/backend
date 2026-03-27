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
 * Layer 1: 화성 기능 라벨러.
 * AnalysisConfigData의 기능 매핑 테이블을 참조하여 각 코드에 T(토닉)/SD(서브도미넌트)/D(도미넌트) 기능을 부여한다.
 */
@Component
@RequiredArgsConstructor
public class FunctionLabeler {

    private final AnalysisConfigData configData;

    /**
     * 1차 기능 부여: 스케일 디그리 기반으로 각 코드에 화성 기능을 할당한다.
     *
     * 알고리즘:
     * 1) 키의 장/단조에 따라 참조할 기능 맵 섹션 결정 (major_key / minor_key)
     * 2) 다이어토닉 코드 → 해당 키 섹션에서 기능 조회
     * 3) 비다이어토닉 코드 → chromatic_degrees에서 먼저 조회, 없으면 키 섹션에서 조회
     * 4) 어디서도 기능을 찾지 못하면 모호성 플래그(contextNeeded=true) 추가
     */
    public List<ParsedChord> label(List<ParsedChord> chords, String key) {
        Map<String, Map<String, List<FunctionEntry>>> funcMap = configData.getFunctionMap();
        KeyInfo ki = NoteUtils.parseKey(key);

        // 장/단조에 따라 참조할 기능 맵 섹션 결정
        String keySection = ki.isMajor() ? "major_key" : "minor_key";
        Map<String, List<FunctionEntry>> degreeMap = funcMap.getOrDefault(keySection, Map.of());
        Map<String, List<FunctionEntry>> chromaticMap = funcMap.getOrDefault("chromatic_degrees", Map.of());

        for (ParsedChord chord : chords) {
            if (chord.getDegree() == null) continue;
            // 룩업 키 생성: 디그리에서 °, + 등 접미사 제거 (예: "vii°" → "vii")
            String lookup = normalizeDegreeForLookup(chord.getDegree());

            if (Boolean.TRUE.equals(chord.getIsDiatonic())) {
                // 다이어토닉 → 키 섹션에서 직접 조회
                List<FunctionEntry> funcs = findFunctions(degreeMap, lookup);
                if (funcs != null) chord.setFunctions(copyFunctions(funcs));
            } else {
                // 비다이어토닉 → chromatic_degrees 우선 조회, 없으면 키 섹션에서 조회
                List<FunctionEntry> funcs = findFunctions(chromaticMap, lookup);
                if (funcs != null) {
                    chord.setFunctions(copyFunctions(funcs));
                } else {
                    funcs = findFunctions(degreeMap, lookup);
                    if (funcs != null) chord.setFunctions(copyFunctions(funcs));
                }
                // 기능을 찾지 못한 경우 모호성 플래그 추가
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
     * 2차 기능 부여(사후 보완): ii-V-I 그룹 역할을 기반으로 아직 기능이 없는 코드에 기능을 할당한다.
     * 예: "ii" 역할 → SD(0.9), "V" 역할 → D(1.0), "I" 역할 → T(1.0)
     */
    public List<ParsedChord> labelFromGroups(List<ParsedChord> chords) {
        // ii-V-I 역할 → 화성 기능 매핑 테이블
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
            if (!chord.getFunctions().isEmpty()) continue; // 이미 기능이 있으면 건너뜀
            // ii-V-I 그룹 소속 확인 → 역할에 맞는 기능 할당
            for (GroupMembership gm : chord.getGroupMemberships()) {
                if ("ii-V-I".equals(gm.getGroupType())) {
                    List<FunctionEntry> funcs = roleFunctions.get(gm.getRole());
                    if (funcs != null) {
                        chord.setFunctions(copyFunctions(funcs));
                        // 기능이 확정되었으므로 "function" 모호성 플래그 제거
                        chord.getAmbiguityFlags().removeIf(a -> "function".equals(a.getAspect()));
                        break;
                    }
                }
            }
        }
        return chords;
    }

    // ── helpers ──

    /** 디그리에서 °, + 등 접미사를 제거하여 기능 맵 룩업 키를 만든다 */
    private String normalizeDegreeForLookup(String degree) {
        if (degree == null) return "";
        return degree.replaceAll("[°+]+$", "");
    }

    /** 기능 맵에서 디그리 키로 기능을 조회한다 (대소문자 무시 폴백 포함) */
    private List<FunctionEntry> findFunctions(Map<String, List<FunctionEntry>> map, String lookup) {
        List<FunctionEntry> result = map.get(lookup);
        if (result != null) return result;
        for (var entry : map.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(lookup)) return entry.getValue();
        }
        return null;
    }

    /** FunctionEntry 리스트를 깊은 복사한다 (원본 설정 데이터 변경 방지) */
    private List<FunctionEntry> copyFunctions(List<FunctionEntry> src) {
        List<FunctionEntry> copy = new ArrayList<>();
        for (FunctionEntry f : src) {
            copy.add(new FunctionEntry(f.getFunction(), f.getConfidence(), f.getNote()));
        }
        return copy;
    }
}

