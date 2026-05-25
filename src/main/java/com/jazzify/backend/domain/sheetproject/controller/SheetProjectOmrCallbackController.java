package com.jazzify.backend.domain.sheetproject.controller;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jazzify.backend.domain.sheetproject.dto.request.OmrCallbackRequest;
import com.jazzify.backend.domain.sheetproject.service.SheetProjectService;
import com.jazzify.backend.shared.web.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * OMR 서버 비동기 콜백을 수신하는 컨트롤러.
 * <p>
 * JWT 인증 없이 동작하며, {@code X-OMR-Callback-API-Key} 헤더 값으로 요청 출처를 검증한다.
 * Security 설정에서 이 경로는 {@code permitAll()}로 열려 있어야 한다.
 */
@NullMarked
@RestController
@RequestMapping("/v1/sheet-projects/omr")
@RequiredArgsConstructor
public class SheetProjectOmrCallbackController implements SheetProjectOmrCallbackControllerSpec {

	private static final String CALLBACK_API_KEY_HEADER = "X-OMR-Callback-API-Key";

	private final SheetProjectService sheetProjectService;

	@Override
	@PostMapping("/callback")
	@ResponseStatus(HttpStatus.OK)
	public ApiResponse<Void> handleCallback(
		@RequestHeader(CALLBACK_API_KEY_HEADER) String callbackApiKey,
		@RequestBody OmrCallbackRequest request
	) {
		sheetProjectService.handleOmrCallback(callbackApiKey, request);
		return ApiResponse.ok();
	}
}

