package com.jazzify.backend.domain.solo.controller;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jazzify.backend.domain.sheetproject.dto.request.OmrCallbackRequest;
import com.jazzify.backend.domain.solo.service.SoloService;
import com.jazzify.backend.shared.web.ApiResponse;

import lombok.RequiredArgsConstructor;

@NullMarked
@RestController
@RequestMapping("/v1/solos/omr")
@RequiredArgsConstructor
public class SoloOmrCallbackController implements SoloOmrCallbackControllerSpec {

	private static final String CALLBACK_API_KEY_HEADER = "X-OMR-Callback-API-Key";

	private final SoloService soloService;

	@Override
	@PostMapping("/callback")
	@ResponseStatus(HttpStatus.OK)
	public ApiResponse<Void> handleCallback(
		@RequestHeader(CALLBACK_API_KEY_HEADER) String callbackApiKey,
		@RequestBody OmrCallbackRequest request
	) {
		soloService.handleOmrCallback(callbackApiKey, request);
		return ApiResponse.ok();
	}
}

