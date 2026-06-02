package com.jazzify.backend.domain.embedding.controller;

import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.domain.embedding.dto.request.EmbeddingProbeRequest;
import com.jazzify.backend.domain.embedding.dto.response.EmbeddingHealthResponse;
import com.jazzify.backend.domain.embedding.dto.response.EmbeddingProbeResponse;
import com.jazzify.backend.shared.web.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * 임베딩 서버 테스트 및 헬스체크 API 명세.
 * <p>
 * Swagger 문서용 어노테이션은 이 인터페이스에만 작성한다.
 */
@NullMarked
@Tag(name = "Embedding", description = "임베딩 서버 연동 테스트 및 헬스체크 API")
@SecurityRequirement(name = "BearerAuth")
public interface EmbeddingControllerSpec {

	@Operation(
		summary = "임베딩 프로브",
		description = """
			텍스트 목록을 임베딩 서버로 전송하여 부동소수점 벡터로 변환한 결과를 반환합니다.
			임베딩 서버 연동 및 벡터 품질 확인에 활용합니다.
			배치 크기는 최대 64개이며, ADMIN 또는 MANAGE 권한이 필요합니다.
			"""
	)
	ApiResponse<EmbeddingProbeResponse> probe(@Valid EmbeddingProbeRequest request);

	@Operation(
		summary = "임베딩 서버 헬스체크",
		description = """
			임베딩 서버의 설정 여부와 실제 HTTP 연결 가능 여부를 확인합니다.
			서버가 미설정 상태이면 configured=false, reachable=false를 반환합니다.
			인증 없이 호출 가능합니다.
			"""
	)
	ApiResponse<EmbeddingHealthResponse> health();
}

