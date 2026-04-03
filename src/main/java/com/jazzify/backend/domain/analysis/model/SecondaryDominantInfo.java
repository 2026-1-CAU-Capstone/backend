package com.jazzify.backend.domain.analysis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SecondaryDominantInfo {
    private String type;            // "V/ii"
    private String impliedDominant; // for dim7 function
    private String targetDegree;
    private String targetChord;
    private boolean resolved;
    private Map<String, Object> originPosition;
}

