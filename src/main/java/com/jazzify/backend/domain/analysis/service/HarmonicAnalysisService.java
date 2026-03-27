package com.jazzify.backend.domain.analysis.service;

import com.jazzify.backend.domain.analysis.analyzer.*;
import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.parser.ChordSymbolParser;
import com.jazzify.backend.domain.analysis.parser.ChordSymbolParser.ParseResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Main harmonic analysis pipeline.
 * Ported from Python main.py → analyze().
 */
@Service
@RequiredArgsConstructor
public class HarmonicAnalysisService {

    private final ChordSymbolParser parser;
    private final ChordNormalizer chordNormalizer;
    private final DiatonicClassifier diatonicClassifier;
    private final FunctionLabeler functionLabeler;
    private final IiViDetector iiViDetector;
    private final TritoneSubDetector tritoneSubDetector;
    private final SecondaryDominantDetector secondaryDominantDetector;
    private final DiminishedClassifier diminishedClassifier;
    private final ChromaticApproachDetector chromaticApproachDetector;
    private final DeceptiveResolutionDetector deceptiveResolutionDetector;
    private final PedalPointDetector pedalPointDetector;
    private final ModalInterchangeDetector modalInterchangeDetector;
    private final ModeSegmentDetector modeSegmentDetector;
    private final TonicizationModulationDetector tonicizationDetector;
    private final SectionBoundaryDetector sectionBoundaryDetector;
    private final AmbiguityScorer ambiguityScorer;
    private final AnalysisAggregator aggregator;

    /**
     * Run the full analysis pipeline.
     *
     * @param text            plain‑text chord progression (bars separated by |)
     * @param key             song key, e.g. "C", "Bb", "F#m"
     * @param title           song title
     * @param timeSignature   e.g. "4/4"
     * @return complete analysis output as a Map (ready for JSON serialization)
     */
    public Map<String, Object> analyze(String text, String key, String title, String timeSignature) {
        // Phase 1: Parse
        ParseResult pr = parser.parseProgressionText(text, title, key, timeSignature);
        List<ParsedChord> chords = pr.chords();

        if (chords.isEmpty()) {
            return Map.of("error", "No chords parsed from input");
        }

        // Phase 2: Layer 1 – individual chord analysis
        chordNormalizer.normalize(chords);
        diatonicClassifier.classify(chords, key);
        functionLabeler.label(chords, key);

        // Phase 3: Layer 2 – contextual pattern detection
        IiViDetector.IiViResult iiViResult = iiViDetector.detect(chords, key);
        chords = iiViResult.chords();
        List<Map<String, Object>> groups = iiViResult.groups();

        functionLabeler.labelFromGroups(chords);
        tritoneSubDetector.detect(chords, groups);
        secondaryDominantDetector.detect(chords, key);
        diminishedClassifier.detect(chords, key);
        chromaticApproachDetector.detect(chords);
        deceptiveResolutionDetector.detect(chords, key);
        pedalPointDetector.detect(chords, key);

        // Phase 4: Layer 3 – structural analysis
        modalInterchangeDetector.detect(chords, key);
        modeSegmentDetector.detect(chords, key);
        tonicizationDetector.detect(chords, key, groups);
        List<Map<String, Object>> sections = sectionBoundaryDetector.detect(chords, key);

        // Phase 5: Ambiguity scoring
        ambiguityScorer.score(chords);

        // Phase 6: Aggregate
        return aggregator.aggregate(title, key, timeSignature, chords, groups, sections);
    }
}

