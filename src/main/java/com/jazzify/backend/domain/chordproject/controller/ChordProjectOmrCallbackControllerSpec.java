package com.jazzify.backend.domain.chordproject.controller;

import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.domain.sheetproject.dto.request.OmrCallbackRequest;
import com.jazzify.backend.shared.web.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * ChordProject OMR 서버 콜백 엔드포인트 Swagger 명세.
 */
@NullMarked
@Tag(name = "ChordProject OMR Callback", description = "ChordProject OMR 서버 비동기 콜백 수신 API (내부용)")
public interface ChordProjectOmrCallbackControllerSpec {

	@Operation(
		summary = "ChordProject OMR 처리 결과 콜백 수신",
		description = """
			OMR 서버가 악보 인식을 완료하거나 실패했을 때 호출하는 콜백 엔드포인트.
			
			- `X-OMR-Callback-API-Key` 헤더로 요청 유효성을 검증한다.
			- `status=completed`: MusicXML과 chord assignments를 파싱하여 ChordProject를 업데이트한다.
			- `status=failed`: ChordProject를 실패 상태로 마킹한다.
			"""
	)
	ApiResponse<Void> handleCallback(
		@Parameter(description = "OMR 콜백 API 키 (X-OMR-Callback-API-Key)", required = true)
		String callbackApiKey,
		OmrCallbackRequest request
	);
}

