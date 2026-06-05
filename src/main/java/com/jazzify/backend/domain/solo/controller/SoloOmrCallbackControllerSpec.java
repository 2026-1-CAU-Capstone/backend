package com.jazzify.backend.domain.solo.controller;

import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.domain.sheetproject.dto.request.OmrCallbackRequest;
import com.jazzify.backend.shared.web.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@NullMarked
@Tag(name = "Solo OMR Callback", description = "Solo OMR 서버 비동기 콜백 수신 API (내부용)")
public interface SoloOmrCallbackControllerSpec {

	@Operation(
		summary = "Solo OMR 처리 결과 콜백 수신",
		description = """
			MusicVision이 Solo OMR 작업을 완료하거나 실패했을 때 호출하는 내부 엔드포인트입니다.
			프론트엔드가 직접 호출하지 않습니다.
			
			- JWT 인증 없이 `X-OMR-Callback-API-Key` 헤더로 요청 유효성을 검증합니다.
			- `job_id`는 Solo `publicId` 문자열이어야 합니다.
			- `status=completed`: `/musicxml`과 `/chord-assignments`를 조회하고 sheetData, harmonic context, similarity features를 채운 뒤 `COMPLETED`로 마킹합니다.
			- `status=failed`: Solo를 `FAILED`로 마킹하고 실패 사유를 저장합니다.
			- `queued`, `processing` 같은 중간 상태 callback은 상태 변경 없이 무시됩니다.
			"""
	)
	ApiResponse<Void> handleCallback(
		@Parameter(description = "OMR 콜백 API 키 (X-OMR-Callback-API-Key)", required = true)
		String callbackApiKey,
		OmrCallbackRequest request
	);
}
