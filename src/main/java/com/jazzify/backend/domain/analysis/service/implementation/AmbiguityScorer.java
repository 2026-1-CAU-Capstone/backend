package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.ParsedChord;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 모호성 채점기.
 * 각 코드의 분석 확실성을 0.0(매우 확실) ~ 1.0(매우 모호) 사이의 점수로 계산한다.
 * 5가지 하위 점수의 가중 합으로 최종 점수를 산출한다.
 */
@Component
public class AmbiguityScorer {

    // ── 하위 점수별 가중치 (합계 = 1.0) ──
    private static final double W_FUNCTION = 0.30;   // 화성 기능 불확실성
    private static final double W_DIATONIC = 0.15;   // 다이어토닉 여부
    private static final double W_GROUP = 0.20;      // 패턴 설명 가능성
    private static final double W_COMPETING = 0.20;  // 경쟁하는 해석 수
    private static final double W_FLAGS = 0.15;      // 모호성 플래그 수

    /**
     * 모든 코드의 모호성 점수(ambiguityScore)를 계산하여 설정한다.
     * 점수 = 5가지 하위 점수의 가중 합 → [0.0, 1.0] 범위로 클램핑 → 소수점 3자리 반올림
     */
    public List<ParsedChord> score(List<ParsedChord> chords) {
        for (ParsedChord chord : chords) {
            // 5가지 하위 점수 계산
            double funcAmb = functionAmbiguity(chord);
            double diaAmb = diatonicAmbiguity(chord);
            double grpAmb = groupAmbiguity(chord);
            double compAmb = competingInterpretations(chord);
            double flagAmb = flagAmbiguity(chord);

            // 가중 합산
            double raw = W_FUNCTION * funcAmb
                    + W_DIATONIC * diaAmb
                    + W_GROUP * grpAmb
                    + W_COMPETING * compAmb
                    + W_FLAGS * flagAmb;

            // [0.0, 1.0] 클램핑 후 소수점 3자리 반올림
            double finalScore = Math.round(Math.max(0.0, Math.min(1.0, raw)) * 1000.0) / 1000.0;
            chord.setAmbiguityScore(finalScore);
        }
        return chords;
    }

    // ── sub‑scorers (하위 채점기) ──

    /**
     * 화성 기능 불확실성 (가중치 30%).
     * - 기능 없음 → 1.0 (완전 불확실)
     * - 기능 1개 → 1.0 - confidence (확신도가 높을수록 낮은 점수)
     * - 기능 2개+ → 상위 2개의 확신도 비율로 판단 (비율이 1에 가까울수록 경쟁 → 모호)
     */
    private double functionAmbiguity(ParsedChord chord) {
        var funcs = chord.getFunctions();
        if (funcs.isEmpty()) return 1.0;
        if (funcs.size() == 1) {
            double conf = funcs.get(0).getConfidence();
            return Math.max(0.0, 1.0 - conf);
        }
        List<Double> confs = funcs.stream()
                .map(f -> f.getConfidence())
                .sorted((a, b) -> Double.compare(b, a))
                .toList();
        double top = confs.get(0);
        double second = confs.size() > 1 ? confs.get(1) : 0.0;
        if (top <= 0) return 1.0;
        double ratio = second / top;
        double lowConfPenalty = Math.max(0.0, 1.0 - top) * 0.3;
        return Math.min(1.0, ratio * 0.7 + lowConfPenalty);
    }

    /**
     * 다이어토닉 불확실성 (가중치 15%).
     * 다이어토닉(0.0) vs 비다이어토닉(0.6) vs 불확실(0.5)
     */
    private double diatonicAmbiguity(ParsedChord chord) {
        if (Boolean.TRUE.equals(chord.getIsDiatonic())) return 0.0;
        if (Boolean.FALSE.equals(chord.getIsDiatonic())) return 0.6;
        return 0.5;
    }

    /**
     * 패턴 설명 가능성 (가중치 20%).
     * 비다이어토닉 코드가 어떤 패턴(ii-V-I, 세컨더리 도미넌트 등)으로 설명되면 0.1, 안 되면 0.9.
     * 다이어토닉은 설명이 불필요하므로 항상 0.0.
     */
    private double groupAmbiguity(ParsedChord chord) {
        boolean explained = !chord.getGroupMemberships().isEmpty()
                || chord.getSecondaryDominant() != null
                || chord.getModalInterchange() != null
                || chord.getDiminishedFunction() != null
                || chord.getChromaticApproach() != null
                || chord.getDeceptiveResolution() != null
                || chord.getPedalInfo() != null;

        if (Boolean.TRUE.equals(chord.getIsDiatonic())) return 0.0;
        return explained ? 0.1 : 0.9;
    }

    /**
     * 경쟁 해석 수 (가중치 20%).
     * 하나의 코드에 동시에 붙은 분석 레이어(세컨더리 도미넌트, 모달 인터체인지 등)가 많을수록 모호.
     * 0~1개: 0.0, 2개: 0.4, 3개+: 0.7
     */
    private double competingInterpretations(ParsedChord chord) {
        int layers = 0;
        if (chord.getSecondaryDominant() != null) layers++;
        if (chord.getModalInterchange() != null) layers++;
        if (chord.getTonicization() != null) layers++;
        if (chord.getChromaticApproach() != null) layers++;
        if (chord.getDeceptiveResolution() != null) layers++;

        if (layers <= 1) return 0.0;
        if (layers == 2) return 0.4;
        return 0.7;
    }

    /**
     * 모호성 플래그 기반 점수 (가중치 15%).
     * 플래그 수 × 0.5 + contextNeeded 수 × 0.3, 최대 1.0
     */
    private double flagAmbiguity(ParsedChord chord) {
        if (chord.getAmbiguityFlags().isEmpty()) return 0.0;
        int n = chord.getAmbiguityFlags().size();
        long contextNeeded = chord.getAmbiguityFlags().stream().filter(f -> f.isContextNeeded()).count();
        return Math.min(1.0, 0.5 * n + 0.3 * contextNeeded);
    }
}
