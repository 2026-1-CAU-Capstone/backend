package com.jazzify.backend.domain.analysis.controller;

import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.springframework.web.bind.annotation.RequestBody;

import com.jazzify.backend.domain.analysis.dto.request.AnalysisRequest;
import com.jazzify.backend.shared.web.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@NullMarked
@Tag(name = "Analysis", description = "화성 분석 API")
public interface AnalysisControllerSpec {

	@Operation(summary = "코드 진행 화성 분석")
	ApiResponse<Map<String, Object>> analyze(@Valid @RequestBody AnalysisRequest request);
}

