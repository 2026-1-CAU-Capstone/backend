package com.jazzify.backend.domain.sheetproject.controller;

import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.domain.sheetproject.dto.request.OmrCallbackRequest;
import com.jazzify.backend.shared.web.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * OMR 서버 콜백 엔드포인트 Swagger 명세.
 * <p>
 * 이 엔드포인트는 JWT 인증 없이 {@code X-OMR-Callback-API-Key} 헤더로 보호된다.
 */
@NullMarked
@Tag(name = "SheetProject OMR Callback", description = "OMR 서버 비동기 콜백 수신 API (내부용)")
public interface SheetProjectOmrCallbackControllerSpec {

	@Operation(
		summary = "OMR 처리 결과 콜백 수신",
		description = """
			OMR 서버가 악보 인식을 완료하거나 실패했을 때 호출하는 콜백 엔드포인트.
			
			- `X-OMR-Callback-API-Key` 헤더로 요청 유효성을 검증한다.
			- `status=completed`: MusicXML과 chord assignments를 가져와 SheetProject를 업데이트한다.
			- `status=failed`: SheetProject를 실패 상태로 마킹한다.
			"""
	)
	ApiResponse<Void> handleCallback(
		@Parameter(description = "OMR 콜백 API 키 (X-OMR-Callback-API-Key)", required = true)
		String callbackApiKey,
		OmrCallbackRequest request
	);
}

