package com.jazzify.backend.domain.analysis.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PedalInfo {
    private int pedalNote;
    private String pedalNoteName;
    private String pedalType;       // "tonic","dominant","subdominant","on X"
    private boolean isOverPedal;
    private int pedalStartBar;
    private int pedalEndBar;
}

