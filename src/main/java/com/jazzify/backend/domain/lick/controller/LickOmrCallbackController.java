package com.jazzify.backend.domain.lick.controller;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jazzify.backend.domain.lick.service.LickService;
import com.jazzify.backend.domain.sheetproject.dto.request.OmrCallbackRequest;
import com.jazzify.backend.shared.web.ApiResponse;

import lombok.RequiredArgsConstructor;

@NullMarked
@RestController
@RequestMapping("/v1/licks/omr")
@RequiredArgsConstructor
public class LickOmrCallbackController implements LickOmrCallbackControllerSpec {

	private static final String CALLBACK_API_KEY_HEADER = "X-OMR-Callback-API-Key";

	private final LickService lickService;

	@Override
	@PostMapping("/callback")
	@ResponseStatus(HttpStatus.OK)
	public ApiResponse<Void> handleCallback(
		@RequestHeader(CALLBACK_API_KEY_HEADER) String callbackApiKey,
		@RequestBody OmrCallbackRequest request
	) {
		lickService.handleOmrCallback(callbackApiKey, request);
		return ApiResponse.ok();
	}
}

