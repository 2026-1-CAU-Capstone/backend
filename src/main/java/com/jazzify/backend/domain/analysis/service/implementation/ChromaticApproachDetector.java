package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.ChromaticApproachInfo;
import com.jazzify.backend.domain.analysis.model.FunctionEntry;
import com.jazzify.backend.domain.analysis.model.ParsedChord;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.jazzify.backend.domain.analysis.util.NoteUtils.mod12;

/**
 * Layer 2: 반음계적 접근 감지기.
 * 비다이어토닉 코드가 다음 코드로 반음 위/아래에서 미끄러지듯 접근하는 패턴을 감지한다.
 * 이미 세컨더리 도미넌트나 트라이톤 대리로 설명된 코드는 제외한다.
 */
@Component
public class ChromaticApproachDetector {

    /**
     * 반음계적 접근을 감지하여 chromaticApproach 정보를 설정한다.
     *
     * 알고리즘:
     * 1) 비다이어토닉이면서 아직 다른 패턴으로 설명되지 않은 코드만 대상
     * 2) 다음 코드와의 근음 간격이 1(아래에서 접근) 또는 11(위에서 접근)이면 감지
     * 3) 코드 품질이 같으면 더 확실한 접근으로 판단 (확신도 0.7 vs 0.5)
     */
    public List<ParsedChord> detect(List<ParsedChord> chords) {
        int n = chords.size();
        for (int i = 0; i < n - 1; i++) {
            ParsedChord chord = chords.get(i);
            ParsedChord next = chords.get(i + 1);

            // 이미 설명된 코드는 건너뜀
            if (Boolean.TRUE.equals(chord.getIsDiatonic())) continue;     // 다이어토닉 제외
            if (chord.getSecondaryDominant() != null) continue;            // 세컨더리 도미넌트 제외
            if (chord.getFunctions().stream().anyMatch(f ->                // 트라이톤 대리 제외
                    f.getNote() != null && f.getNote().contains("tritone"))) continue;

            // 반음 간격 확인: 1=아래에서 접근, 11=위에서 접근
            int interval = mod12(next.getRoot() - chord.getRoot());
            String direction = null;
            if (interval == 1) direction = "below";      // 반음 아래에서 위로
            else if (interval == 11) direction = "above"; // 반음 위에서 아래로
            if (direction == null) continue;

            // 품질 일치 여부 확인 (같으면 더 확실한 접근)
            String nq = nq(chord);
            String nextNq = nq(next);
            boolean qualityMatch = nq.equals(nextNq);

            chord.setChromaticApproach(ChromaticApproachInfo.builder()
                    .target(next.getOriginalSymbol())
                    .targetBar(next.getBar())
                    .targetBeat(next.getBeat())
                    .direction(direction)
                    .qualityMatch(qualityMatch)
                    .build());

            // 기능이 아직 비어있으면 chromatic_approach 기능 부여
            if (chord.getFunctions().isEmpty()) {
                chord.setFunctions(List.of(new FunctionEntry("chromatic_approach",
                        qualityMatch ? 0.7 : 0.5,
                        "Chromatic approach from " + direction + " to " + next.getOriginalSymbol())));
            }
        }
        return chords;
    }

    private static String nq(ParsedChord c) {
        return c.getNormalizedQuality() != null ? c.getNormalizedQuality() : c.getQuality();
    }
}

