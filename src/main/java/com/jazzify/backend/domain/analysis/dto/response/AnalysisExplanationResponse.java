package com.jazzify.backend.domain.analysis.dto.response;

import java.util.List;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record AnalysisExplanationResponse(
	String title,
	String key,
	String timeSignature,
	String overview,
	List<ChordExplanation> chordExplanations,
	List<String> sectionExplanations,
	List<String> notablePatterns,
	String harmonicSummary
) {
}

