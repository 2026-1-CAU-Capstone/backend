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
public class AmbiguityFlag {
    private String aspect;
    private List<String> interpretations;
    private boolean contextNeeded;
}

