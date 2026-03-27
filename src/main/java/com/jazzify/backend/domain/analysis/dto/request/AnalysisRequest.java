package com.jazzify.backend.domain.analysis.dto.request;

import jakarta.validation.constraints.NotBlank;

public record AnalysisRequest(
        @NotBlank(message = "Chord progression text is required")
        String text,

        String key,

        String title,

        String timeSignature
) {
    public AnalysisRequest {
        if (key == null || key.isBlank()) key = "C";
        if (title == null || title.isBlank()) title = "Untitled";
        if (timeSignature == null || timeSignature.isBlank()) timeSignature = "4/4";
    }
}

