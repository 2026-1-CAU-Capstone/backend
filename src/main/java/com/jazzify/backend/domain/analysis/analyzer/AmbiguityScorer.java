package com.jazzify.backend.domain.analysis.analyzer;

import com.jazzify.backend.domain.analysis.model.ParsedChord;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ambiguity Scorer.
 * Computes per‑chord ambiguity_score in [0.0, 1.0].
 */
@Component
public class AmbiguityScorer {

    private static final double W_FUNCTION = 0.30;
    private static final double W_DIATONIC = 0.15;
    private static final double W_GROUP = 0.20;
    private static final double W_COMPETING = 0.20;
    private static final double W_FLAGS = 0.15;

    public List<ParsedChord> score(List<ParsedChord> chords) {
        for (ParsedChord chord : chords) {
            double funcAmb = functionAmbiguity(chord);
            double diaAmb = diatonicAmbiguity(chord);
            double grpAmb = groupAmbiguity(chord);
            double compAmb = competingInterpretations(chord);
            double flagAmb = flagAmbiguity(chord);

            double raw = W_FUNCTION * funcAmb
                    + W_DIATONIC * diaAmb
                    + W_GROUP * grpAmb
                    + W_COMPETING * compAmb
                    + W_FLAGS * flagAmb;

            double finalScore = Math.round(Math.max(0.0, Math.min(1.0, raw)) * 1000.0) / 1000.0;
            chord.setAmbiguityScore(finalScore);
        }
        return chords;
    }

    // ── sub‑scorers ──

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

    private double diatonicAmbiguity(ParsedChord chord) {
        if (Boolean.TRUE.equals(chord.getIsDiatonic())) return 0.0;
        if (Boolean.FALSE.equals(chord.getIsDiatonic())) return 0.6;
        return 0.5;
    }

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

    private double flagAmbiguity(ParsedChord chord) {
        if (chord.getAmbiguityFlags().isEmpty()) return 0.0;
        int n = chord.getAmbiguityFlags().size();
        long contextNeeded = chord.getAmbiguityFlags().stream().filter(f -> f.isContextNeeded()).count();
        return Math.min(1.0, 0.5 * n + 0.3 * contextNeeded);
    }
}

