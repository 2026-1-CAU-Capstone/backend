package com.jazzify.backend.domain.analysis.service;

import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.parser.NoteUtils;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Aggregates all analysis results into the final output map.
 * Ported from Python aggregator.py.
 */
@Component
public class AnalysisAggregator {

    private static final String ENGINE_VERSION = "0.1.0";

    private static final List<String> COVERAGE = List.of(
            "diatonic_classification",
            "scale_degree_calculation",
            "T_SD_D_function_labeling",
            "chord_normalization",
            "ii-V-I_detection",
            "tritone_substitution_detection",
            "secondary_dominant_detection",
            "diminished_chord_classification",
            "chromatic_approach_detection",
            "deceptive_resolution_detection",
            "pedal_point_detection",
            "modal_interchange_detection",
            "mode_segment_detection",
            "tonicization_modulation_detection",
            "section_boundary_detection",
            "ambiguity_scoring"
    );

    public Map<String, Object> aggregate(String title, String key, String timeSignature,
                                          List<ParsedChord> chords,
                                          List<Map<String, Object>> groups,
                                          List<Map<String, Object>> sections) {

        List<Map<String, Object>> chordDicts = chords.stream()
                .map(this::chordToDict).toList();

        // Ambiguity statistics
        List<Double> scores = chords.stream().map(ParsedChord::getAmbiguityScore).toList();
        int n = scores.size();
        long highConf = scores.stream().filter(s -> s <= 0.1).count();
        long ambiguous = scores.stream().filter(s -> s > 0.3).count();
        double mean = n > 0 ? scores.stream().mapToDouble(Double::doubleValue).sum() / n : 0;
        double max = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("song", Map.of("title", title, "key", key, "time_signature", timeSignature));
        output.put("chords", chordDicts);
        output.put("groups", groups);
        output.put("sections", sections);
        output.put("ambiguity_stats", Map.of(
                "total_chords", n,
                "high_confidence_count", highConf,
                "high_confidence_pct", n > 0 ? Math.round(highConf * 1000.0 / n) / 10.0 : 0,
                "ambiguous_count", ambiguous,
                "ambiguous_pct", n > 0 ? Math.round(ambiguous * 1000.0 / n) / 10.0 : 0,
                "mean_score", Math.round(mean * 1000.0) / 1000.0,
                "max_score", Math.round(max * 1000.0) / 1000.0
        ));
        output.put("engine_version", ENGINE_VERSION);
        output.put("coverage", COVERAGE);

        return output;
    }

    // ── helpers ──

    private Map<String, Object> chordToDict(ParsedChord c) {
        Map<String, Object> analysis = new LinkedHashMap<>();
        analysis.put("root", c.getRoot());
        analysis.put("root_name", NoteUtils.pcToNoteName(c.getRoot()));
        analysis.put("quality", c.getQuality());
        analysis.put("normalized_quality", c.getNormalizedQuality() != null ? c.getNormalizedQuality() : c.getQuality());
        analysis.put("tensions", c.getTensions());
        analysis.put("bass", c.getBass());
        analysis.put("bass_name", c.getBass() != null ? NoteUtils.pcToNoteName(c.getBass()) : null);
        analysis.put("degree", c.getDegree());
        analysis.put("is_diatonic", c.getIsDiatonic());
        analysis.put("functions", c.getFunctions());
        analysis.put("secondary_dominant", c.getSecondaryDominant());
        analysis.put("group_memberships", c.getGroupMemberships());
        analysis.put("diminished_function", c.getDiminishedFunction());
        analysis.put("chromatic_approach", c.getChromaticApproach());
        analysis.put("deceptive_resolution", c.getDeceptiveResolution());
        analysis.put("pedal_info", c.getPedalInfo());
        analysis.put("modal_interchange", c.getModalInterchange());
        analysis.put("mode_segment", c.getModeSegment());
        analysis.put("tonicization", c.getTonicization());
        analysis.put("ambiguity_flags", c.getAmbiguityFlags());
        analysis.put("ambiguity_score", c.getAmbiguityScore());

        Map<String, Object> dict = new LinkedHashMap<>();
        dict.put("bar", c.getBar());
        dict.put("beat", c.getBeat());
        dict.put("symbol", c.getOriginalSymbol());
        dict.put("duration_beats", c.getDurationBeats());
        dict.put("analysis", analysis);
        return dict;
    }
}

