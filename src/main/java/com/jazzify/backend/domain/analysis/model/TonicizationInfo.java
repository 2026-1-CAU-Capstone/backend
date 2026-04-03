package com.jazzify.backend.domain.analysis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TonicizationInfo {
    private String type;            // "tonicization" or "modulation"
    private String temporaryKey;
    private int startBar;
    private int endBar;
    private List<String> evidence;
    private double confidence;
}

