package com.jazzify.backend.domain.analysis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Harmonic function entry: T, SD, D, D_substitute, etc.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionEntry {
    private String function;
    private double confidence;
    private String note;

    public FunctionEntry(String function, double confidence) {
        this.function = function;
        this.confidence = confidence;
    }
}

