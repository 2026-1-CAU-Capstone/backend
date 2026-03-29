package com.jazzify.backend.domain.analysis.controller;

import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jazzify.backend.domain.analysis.dto.request.AnalysisRequest;
import com.jazzify.backend.domain.analysis.dto.response.AnalysisExplanationResponse;
import com.jazzify.backend.domain.analysis.service.HarmonicAnalysisService;
import com.jazzify.backend.shared.web.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@NullMarked
@RestController
@RequestMapping("/v1/analysis")
@RequiredArgsConstructor
public class AnalysisController implements AnalysisControllerSpec {

	private final HarmonicAnalysisService harmonicAnalysisService;
	
	@PostMapping
	public ApiResponse<Map<String, Object>> analyze(@Valid @RequestBody AnalysisRequest request) {
		Map<String, Object> result = harmonicAnalysisService.analyze(
			request.text(),
			request.key(),
			request.title(),
			request.timeSignature()
		);
		return ApiResponse.ok(result);
	}

	@PostMapping("/explain")
	public ApiResponse<AnalysisExplanationResponse> explain(@Valid @RequestBody AnalysisRequest request) {
		AnalysisExplanationResponse result = harmonicAnalysisService.explain(
			request.text(),
			request.key(),
			request.title(),
			request.timeSignature()
		);
		return ApiResponse.ok(result);
	}
}
