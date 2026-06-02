package com.jazzify.backend.domain.embedding.controller;

import org.jspecify.annotations.NullMarked;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jazzify.backend.domain.embedding.dto.request.EmbeddingProbeRequest;
import com.jazzify.backend.domain.embedding.dto.response.EmbeddingHealthResponse;
import com.jazzify.backend.domain.embedding.dto.response.EmbeddingProbeResponse;
import com.jazzify.backend.domain.embedding.service.EmbeddingService;
import com.jazzify.backend.shared.web.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 임베딩 서버 테스트 및 헬스체크 컨트롤러.
 *
 * <ul>
 *   <li>{@code POST /v1/embedding/probe} – 텍스트 → 벡터 변환 테스트 (ADMIN/MANAGE 권한)</li>
 *   <li>{@code GET  /v1/embedding/health} – 서버 연결 상태 확인 (인증 불필요)</li>
 * </ul>
 */
@RestController
@NullMarked
@RequiredArgsConstructor
@RequestMapping("/v1/embedding")
public class EmbeddingController implements EmbeddingControllerSpec {

	private final EmbeddingService embeddingService;

	@Override
	@PostMapping("/probe")
	public ApiResponse<EmbeddingProbeResponse> probe(@Valid @RequestBody EmbeddingProbeRequest request) {
		return ApiResponse.ok(embeddingService.probe(request));
	}

	@Override
	@GetMapping("/health")
	public ApiResponse<EmbeddingHealthResponse> health() {
		return ApiResponse.ok(embeddingService.health());
	}
}

