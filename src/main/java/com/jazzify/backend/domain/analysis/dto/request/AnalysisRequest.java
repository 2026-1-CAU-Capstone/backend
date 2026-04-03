package com.jazzify.backend.domain.analysis.dto.request;

import org.jspecify.annotations.NullMarked;

import jakarta.validation.constraints.NotBlank;

@NullMarked
public record AnalysisRequest(
	@NotBlank(message = "코드 진행 텍스트는 필수입니다.")
	String text,

	String key,

	String title,

	String timeSignature
) {

	public AnalysisRequest {
		if (key == null || key.isBlank()) {
			key = "C";
		}
		if (title == null || title.isBlank()) {
			title = "Untitled";
		}
		if (timeSignature == null || timeSignature.isBlank()) {
			timeSignature = "4/4";
		}
	}
}
