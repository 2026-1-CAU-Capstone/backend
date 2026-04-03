package com.jazzify.backend.domain.analysis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Parsed chord internal representation.
 * Mutable – analyzers fill in fields progressively through the pipeline.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedChord {

    // ── Parsing result ──
    private String originalSymbol;          // e.g. "Dm7(11)"
    private int root;                       // pitch class 0‑11 (C=0)
    private String quality;                 // "maj7","min7","dom7", …
    @Builder.Default
    private List<String> tensions = new ArrayList<>();
    private Integer bass;                   // slash‑chord bass (pitch class), null if absent
    private int bar;
    @Builder.Default
    private double beat = 1.0;
    private double durationBeats;

    // ── Layer 1 ──
    private String degree;                  // "I","ii","bVII", …
    private Boolean isDiatonic;
    @Builder.Default
    private List<FunctionEntry> functions = new ArrayList<>();

    // ── Layer 2 ──
    private SecondaryDominantInfo secondaryDominant;
    @Builder.Default
    private List<GroupMembership> groupMemberships = new ArrayList<>();
    private String diminishedFunction;      // "passing","auxiliary","dominant_function"
    private ChromaticApproachInfo chromaticApproach;
    private DeceptiveResolutionInfo deceptiveResolution;
    private PedalInfo pedalInfo;

    // ── Layer 3 ──
    private ModalInterchangeInfo modalInterchange;
    private String modeSegment;             // "dorian","lydian", …
    private TonicizationInfo tonicization;

    // ── Ambiguity ──
    @Builder.Default
    private List<AmbiguityFlag> ambiguityFlags = new ArrayList<>();
    private String normalizedQuality;
    @Builder.Default
    private double ambiguityScore = 0.0;
}

