package com.jazzify.backend.domain.lick.controller;

import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.domain.sheetproject.dto.request.OmrCallbackRequest;
import com.jazzify.backend.shared.web.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@NullMarked
@Tag(name = "Lick OMR Callback", description = "Lick OMR 서버 비동기 콜백 수신 API (내부용)")
public interface LickOmrCallbackControllerSpec {

	@Operation(summary = "Lick OMR 처리 결과 콜백 수신")
	ApiResponse<Void> handleCallback(
		@Parameter(description = "OMR 콜백 API 키 (X-OMR-Callback-API-Key)", required = true)
		String callbackApiKey,
		OmrCallbackRequest request
	);
}

