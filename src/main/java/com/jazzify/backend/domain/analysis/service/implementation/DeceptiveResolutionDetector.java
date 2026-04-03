package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.DeceptiveResolutionInfo;
import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import com.jazzify.backend.domain.analysis.util.NoteUtils.KeyInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.jazzify.backend.domain.analysis.util.NoteUtils.mod12;
import static com.jazzify.backend.domain.analysis.util.NoteUtils.pcToNoteName;

/**
 * Layer 2: 기만 종지 감지기.
 * dom7이 기대되는 I(완전4도 위)이 아닌 다른 코드로 해결되는 기만 종지를 감지한다.
 * 예: G7 → Am7 (V → vi, 가장 고전적인 기만 종지)
 */
@Component
public class DeceptiveResolutionDetector {

    private static final Set<String> DOM7 = Set.of("dom7", "dom7sus4", "aug7");

    /** 기대 I로부터의 반음 간격 → (실제 도착 디그리, 흔한 패턴 여부) 매핑 */
    private record DeceptiveTarget(String degree, boolean common) {}
    private static final Map<Integer, DeceptiveTarget> COMMON_DECEPTIVE = Map.of(
            9, new DeceptiveTarget("vi", true),       // V → vi (가장 대표적)
            8, new DeceptiveTarget("bVI", true),      // V → bVI
            5, new DeceptiveTarget("IV", true),       // V → IV
            4, new DeceptiveTarget("iii", true),      // V → iii
            10, new DeceptiveTarget("bVII", true),    // V → bVII
            3, new DeceptiveTarget("bIII", false)     // V → bIII (드문 패턴)
    );

    /**
     * 기만 종지를 감지하여 deceptiveResolution 정보를 설정한다.
     *
     * 알고리즘:
     * 1) dom7 코드를 찾고, 기대 해결 근음(= 완전4도 위) 계산
     * 2) 다음 코드가 기대 근음이면 정상 해결 → 건너뜀
     * 3) 백도어로 태깅된 코드도 건너뜀
     * 4) 기대 근음이 아니면 → 기만 종지! COMMON_DECEPTIVE에서 패턴 분류
     */
    public List<ParsedChord> detect(List<ParsedChord> chords, String key) {
        NoteUtils.parseKey(key); // 키 유효성 검증
        int n = chords.size();

        for (int i = 0; i < n - 1; i++) {
            ParsedChord chord = chords.get(i);
            String nq = nq(chord);
            if (!DOM7.contains(nq)) continue;

            ParsedChord next = chords.get(i + 1);
            // 기대 해결 근음: 완전4도 위 (= 완전5도 아래)
            int expectedRoot = mod12(chord.getRoot() + 5);
            if (next.getRoot() == expectedRoot) continue; // 정상 해결이면 건너뜀

            // 백도어 진행으로 이미 태깅된 코드는 제외
            boolean isBackdoor = chord.getGroupMemberships().stream()
                    .anyMatch(gm -> gm.getVariant() != null && gm.getVariant().contains("backdoor"));
            if (isBackdoor) continue;

            // 기대 I 근음으로부터 실제 도착 코드까지의 간격으로 패턴 분류
            int intervalFromExpected = mod12(next.getRoot() - expectedRoot);
            String expectedName = pcToNoteName(expectedRoot);

            DeceptiveTarget dt = COMMON_DECEPTIVE.get(intervalFromExpected);
            String actualDegree;
            boolean common;
            if (dt != null) {
                actualDegree = dt.degree;
                common = dt.common;
            } else {
                actualDegree = "(" + pcToNoteName(next.getRoot()) + ")";
                common = false;
            }

            chord.setDeceptiveResolution(DeceptiveResolutionInfo.builder()
                    .dominantChord(chord.getOriginalSymbol())
                    .expectedResolution(expectedName + "maj7")
                    .actualResolution(next.getOriginalSymbol())
                    .actualDegree(actualDegree)
                    .commonPattern(common)
                    .build());
        }
        return chords;
    }

    private static String nq(ParsedChord c) {
        return c.getNormalizedQuality() != null ? c.getNormalizedQuality() : c.getQuality();
    }
}
