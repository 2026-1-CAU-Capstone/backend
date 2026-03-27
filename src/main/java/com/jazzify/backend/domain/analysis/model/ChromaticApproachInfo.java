package com.jazzify.backend.domain.analysis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChromaticApproachInfo {
    private String target;
    private int targetBar;
    private double targetBeat;
    private String direction;   // "above" or "below"
    private boolean qualityMatch;
}

