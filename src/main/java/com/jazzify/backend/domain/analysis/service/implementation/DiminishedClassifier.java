package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.AmbiguityFlag;
import com.jazzify.backend.domain.analysis.model.FunctionEntry;
import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.model.SecondaryDominantInfo;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import com.jazzify.backend.domain.analysis.util.NoteUtils.KeyInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.jazzify.backend.domain.analysis.util.NoteUtils.mod12;
import static com.jazzify.backend.domain.analysis.util.NoteUtils.pcToNoteName;

/**
 * Layer 2: 감화음 분류기.
 * dim/dim7 코드의 문맥적 기능을 전후 코드 관계로 분류한다.
 * 경과음(passing), 보조음(auxiliary), 도미넌트 기능(dominant_function) 중 하나로 판별.
 */
@Component
public class DiminishedClassifier {

    private static final Set<String> DIM = Set.of("dim7", "dim");
    private static final Map<Integer, String> TARGET_DEGREES = Map.of(
            0, "I", 2, "ii", 4, "iii", 5, "IV", 7, "V", 9, "vi", 11, "vii"
    );

    /**
     * 감화음의 문맥적 기능을 분류한다.
     *
     * 알고리즘 (우선순위 순):
     * 1) 보조음(auxiliary): prev와 next의 근음이 같고, dim이 반음 위에 있으면 → 같은 코드로 돌아가는 장식
     * 2) 경과음(passing): prev→dim→next가 반음씩 상행 또는 하행이면 → 두 코드 사이 반음 연결
     * 3) 도미넌트 기능: dim의 근음+1 == next 근음이면 → rootless V7b9로 기능
     *    (암시된 dom 근음 = dim 근음 - 장3도)
     * 4) 위 어느 것에도 해당 안 되면 → "unknown" + 모호성 플래그
     */
    public List<ParsedChord> detect(List<ParsedChord> chords, String key) {
        KeyInfo ki = NoteUtils.parseKey(key);
        int keyRoot = ki.root();
        int n = chords.size();

        for (int i = 0; i < n; i++) {
            ParsedChord chord = chords.get(i);
            String nq = nq(chord);
            if (!DIM.contains(nq)) continue; // dim/dim7만 대상

            ParsedChord prev = i > 0 ? chords.get(i - 1) : null;
            ParsedChord next = i + 1 < n ? chords.get(i + 1) : null;
            boolean classified = false;

            // (1) 보조음: 앞뒤 코드가 같고, dim이 같거나 반음 위에 있으면 장식적 기능
            if (prev != null && next != null && prev.getRoot() == next.getRoot()) {
                int diff = mod12(chord.getRoot() - prev.getRoot());
                if (diff == 0 || diff == 1) {
                    chord.setDiminishedFunction("auxiliary");
                    classified = true;
                }
            }

            // (2) 경과음: prev→dim→next가 각각 반음 간격이면 연결 기능
            if (!classified && prev != null && next != null) {
                boolean asc = mod12(chord.getRoot() - prev.getRoot()) == 1
                        && mod12(next.getRoot() - chord.getRoot()) == 1;
                boolean desc = mod12(prev.getRoot() - chord.getRoot()) == 1
                        && mod12(chord.getRoot() - next.getRoot()) == 1;
                if (asc || desc) {
                    chord.setDiminishedFunction("passing");
                    classified = true;
                }
            }

            // (3) 도미넌트 기능: dim 근음+1 == next 근음이면 rootless V7b9로 해석
            if (!classified && next != null) {
                if (mod12(chord.getRoot() + 1) == next.getRoot()) {
                    // 암시된 도미넌트 근음 = dim 근음에서 장3도(4반음) 아래
                    int impliedDomRoot = mod12(chord.getRoot() - 4);
                    String impliedDomName = pcToNoteName(impliedDomRoot);
                    chord.setDiminishedFunction("dominant_function");
                    classified = true;

                    // 타겟 디그리 계산 및 SecondaryDominantInfo 설정
                    int targetInterval = mod12(next.getRoot() - keyRoot);
                    String targetDeg = TARGET_DEGREES.getOrDefault(targetInterval,
                            "(" + pcToNoteName(next.getRoot()) + ")");

                    chord.setSecondaryDominant(SecondaryDominantInfo.builder()
                            .type("V/" + targetDeg + " (as dim7)")
                            .impliedDominant(impliedDomName + "7b9")
                            .targetDegree(targetDeg)
                            .targetChord(next.getOriginalSymbol())
                            .resolved(true)
                            .originPosition(Map.of("bar", chord.getBar(), "beat", chord.getBeat()))
                            .build());

                    chord.setFunctions(List.of(new FunctionEntry("D", 0.8,
                            "Diminished chord functioning as " + impliedDomName + "7b9 (V/" + targetDeg + ")")));
                }
            }

            // (4) 분류 불가: 모호성 플래그 추가
            if (!classified) {
                chord.setDiminishedFunction("unknown");
                chord.getAmbiguityFlags().add(AmbiguityFlag.builder()
                        .aspect("diminished_function")
                        .interpretations(List.of("passing", "auxiliary", "dominant_function"))
                        .contextNeeded(true).build());
            }
        }
        return chords;
    }

    private static String nq(ParsedChord c) {
        return c.getNormalizedQuality() != null ? c.getNormalizedQuality() : c.getQuality();
    }
}
