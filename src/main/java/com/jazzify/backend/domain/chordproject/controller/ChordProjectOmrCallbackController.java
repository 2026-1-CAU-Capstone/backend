package com.jazzify.backend.domain.chordproject.controller;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jazzify.backend.domain.chordproject.service.ChordProjectService;
import com.jazzify.backend.domain.sheetproject.dto.request.OmrCallbackRequest;
import com.jazzify.backend.shared.web.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * ChordProject OMR 서버 비동기 콜백을 수신하는 컨트롤러.
 */
@NullMarked
@RestController
@RequestMapping("/v1/chord-projects/omr")
@RequiredArgsConstructor
public class ChordProjectOmrCallbackController implements ChordProjectOmrCallbackControllerSpec {

	private static final String CALLBACK_API_KEY_HEADER = "X-OMR-Callback-API-Key";

	private final ChordProjectService chordProjectService;

	@Override
	@PostMapping("/callback")
	@ResponseStatus(HttpStatus.OK)
	public ApiResponse<Void> handleCallback(
		@RequestHeader(CALLBACK_API_KEY_HEADER) String callbackApiKey,
		@RequestBody OmrCallbackRequest request
	) {
		chordProjectService.handleOmrCallback(callbackApiKey, request);
		return ApiResponse.ok();
	}
}
