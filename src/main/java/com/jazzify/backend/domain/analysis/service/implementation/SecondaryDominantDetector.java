package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.FunctionEntry;
import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.model.SecondaryDominantInfo;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import com.jazzify.backend.domain.analysis.util.NoteUtils.KeyInfo;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.jazzify.backend.domain.analysis.util.NoteUtils.mod12;
import static com.jazzify.backend.domain.analysis.util.NoteUtils.pcToNoteName;

/**
 * Layer 2: 세컨더리 도미넌트 감지기.
 * 다이어토닉 V7이 아닌 dom7 코드가 다른 코드를 임시 토닉으로 만드는 패턴을 감지한다.
 * 예: C장조에서 A7 → Dm7 이면 A7은 "V/ii" (Dm으로의 세컨더리 도미넌트)
 */
@Component
public class SecondaryDominantDetector {

    private static final Set<String> DOM7 = Set.of("dom7", "dom7sus4", "aug7");

    /** 장조에서 반음 간격 → 디그리 이름 매핑 (타겟 디그리 결정용) */
    private static final Map<Integer, String> MAJOR_DEGREE_ROOTS = Map.of(
            0, "I", 2, "ii", 4, "iii", 5, "IV", 7, "V", 9, "vi", 11, "vii"
    );
    /** 단조에서 반음 간격 → 디그리 이름 매핑 */
    private static final Map<Integer, String> MINOR_DEGREE_ROOTS = Map.of(
            0, "i", 2, "ii", 3, "bIII", 5, "iv", 7, "V", 8, "bVI", 10, "bVII"
    );

    /**
     * 세컨더리 도미넌트를 감지하여 secondaryDominant 정보와 D 기능을 설정한다.
     *
     * 알고리즘:
     * 1) dom7 코드 중 다이어토닉 V가 아닌 것만 대상
     * 2) 타겟 근음 = 코드 근음 + 완전4도(5반음) = 이 코드가 V7일 때의 해결 대상
     * 3) 타겟의 디그리를 키 기준으로 계산 → "V/ii", "V/vi" 등
     * 4) 다음 코드가 실제로 타겟이면 resolved=true (확신도 0.9), 아니면 false (확신도 0.6)
     */
    public List<ParsedChord> detect(List<ParsedChord> chords, String key) {
        KeyInfo ki = NoteUtils.parseKey(key);
        int keyRoot = ki.root();
        Map<Integer, String> degreeMap = ki.isMajor() ? MAJOR_DEGREE_ROOTS : MINOR_DEGREE_ROOTS;

        int n = chords.size();
        for (int i = 0; i < n; i++) {
            ParsedChord chord = chords.get(i);
            String nq = nq(chord);
            if (!DOM7.contains(nq)) continue; // dom7만 대상
            // 다이어토닉 V/v는 세컨더리 도미넌트가 아니므로 건너뜀
            if (Boolean.TRUE.equals(chord.getIsDiatonic())
                    && ("V".equals(chord.getDegree()) || "v".equals(chord.getDegree()))) continue;

            // 타겟 근음 계산: 완전4도 위 (= 이 코드를 V7으로 봤을 때 해결 대상)
            int targetRoot = mod12(chord.getRoot() + 5);
            // 타겟의 디그리 계산 (키 기준)
            int targetInterval = mod12(targetRoot - keyRoot);
            String targetDegree = degreeMap.getOrDefault(targetInterval, "(" + pcToNoteName(targetRoot) + ")");

            // 다음 코드가 타겟인지 확인하여 해결 여부 판단
            boolean resolved = false;
            String targetChordSymbol = null;
            if (i + 1 < n) {
                ParsedChord next = chords.get(i + 1);
                if (next.getRoot() == targetRoot) {
                    resolved = true;
                    targetChordSymbol = next.getOriginalSymbol();
                }
            }

            // SecondaryDominantInfo 설정 및 기능을 D(도미넌트)로 교체
            String secDomType = "V/" + targetDegree;
            chord.setSecondaryDominant(SecondaryDominantInfo.builder()
                    .type(secDomType)
                    .targetDegree(targetDegree)
                    .targetChord(targetChordSymbol != null ? targetChordSymbol : pcToNoteName(targetRoot))
                    .resolved(resolved)
                    .originPosition(Map.of("bar", chord.getBar(), "beat", chord.getBeat()))
                    .build());

            chord.setFunctions(List.of(new FunctionEntry("D",
                    resolved ? 0.9 : 0.6, // 해결되면 높은 확신도, 미해결이면 낮은 확신도
                    "Secondary dominant " + secDomType + (resolved ? " (resolved)" : " (unresolved)"))));
        }
        return chords;
    }

    private static String nq(ParsedChord c) {
        return c.getNormalizedQuality() != null ? c.getNormalizedQuality() : c.getQuality();
    }
}

