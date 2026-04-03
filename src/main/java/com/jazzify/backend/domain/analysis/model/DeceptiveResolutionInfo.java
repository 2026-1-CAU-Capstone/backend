package com.jazzify.backend.domain.analysis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeceptiveResolutionInfo {
    private String dominantChord;
    private String expectedResolution;
    private String actualResolution;
    private String actualDegree;
    private boolean commonPattern;
}

