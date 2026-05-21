package com.jazzify.backend.domain.rag.controller;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.rag.dto.request.RagChatRequest;
import com.jazzify.backend.domain.rag.dto.request.RagDocumentCreateRequest;
import com.jazzify.backend.domain.rag.dto.request.RagDocumentUpdateRequest;
import com.jazzify.backend.domain.rag.dto.response.RagDocumentResponse;
import com.jazzify.backend.domain.rag.dto.response.RagDocumentSummaryResponse;
import com.jazzify.backend.domain.rag.dto.response.RagHealthResponse;
import com.jazzify.backend.domain.rag.dto.response.RagSearchResponse;
import com.jazzify.backend.shared.web.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@NullMarked
@Tag(name = "Rag", description = "RAG 문서 CRUD 및 검색 API")
@SecurityRequirement(name = "BearerAuth")
public interface RagControllerSpec {

	@Operation(
		summary = "RAG 문서 목록 조회",
		description = "RAG 문서를 페이지 단위로 조회합니다."
	)
	ApiResponse<Page<RagDocumentSummaryResponse>> getDocuments(
		@Nullable String sourceType,
		@Nullable String query,
		Pageable pageable);

	@Operation(
		summary = "RAG 문서 단건 조회",
		description = "publicId로 RAG 문서를 조회합니다."
	)
	ApiResponse<RagDocumentResponse> getDocumentByPublicId(@PathVariable UUID publicId);

	@Operation(
		summary = "RAG 문서 생성",
		description = "RAG 문서를 생성하고 즉시 청크/임베딩을 재색인합니다."
	)
	ApiResponse<RagDocumentResponse> createDocument(@Valid @RequestBody RagDocumentCreateRequest request);

	@Operation(
		summary = "RAG 문서 수정",
		description = "RAG 문서를 수정하고 청크/임베딩을 다시 생성합니다."
	)
	ApiResponse<RagDocumentResponse> updateDocument(@PathVariable UUID publicId, @Valid @RequestBody RagDocumentUpdateRequest request);

	@Operation(
		summary = "RAG 문서 삭제",
		description = "RAG 문서와 연결된 청크를 함께 삭제합니다."
	)
	ApiResponse<Void> deleteDocument(@PathVariable UUID publicId);

	@Operation(
		summary = "RAG 채팅",
		description = "멀티쿼리 + RRF로 RAG 컨텍스트를 구성한 뒤 Spring AI 기반 LLM 텍스트 스트림을 반환합니다. 응답 헤더 `X-Chat-Public-Id`로 채팅 식별자를 반환합니다."
	)
	ResponseEntity<StreamingResponseBody> chat(
		@AuthenticationPrincipal CustomPrincipal principal,
		@Valid @RequestBody RagChatRequest request
	);

	@Operation(
		summary = "RAG 검색",
		description = "pgvector 기반 임베딩 검색으로 관련 청크를 조회합니다."
	)
	com.jazzify.backend.shared.web.ApiResponse<RagSearchResponse> search(
		@RequestParam("q") String query,
		@RequestParam(required = false) @Nullable Integer level,
		@RequestParam(defaultValue = "5") int n,
		@RequestParam(required = false) @Nullable String song,
		@RequestParam(required = false) @Nullable String tag,
		@RequestParam(required = false) @Nullable String sourceType);

	@Operation(
		summary = "RAG 헬스체크",
		description = "RAG 활성화 여부와 청크 적재 상태를 조회합니다."
	)
	ApiResponse<RagHealthResponse> health();
}



