package com.jazzify.backend.domain.chat.controller;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.chat.dto.request.ChatStreamRequest;
import com.jazzify.backend.domain.chat.dto.response.ChatDetailResponse;
import com.jazzify.backend.domain.chat.dto.response.ChatSummaryResponse;
import com.jazzify.backend.shared.web.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@NullMarked
@Tag(name = "Chat", description = "LLM 스트리밍 채팅 API")
@SecurityRequirement(name = "BearerAuth")
public interface ChatControllerSpec {

	@Operation(
		summary = "내 채팅 목록 조회",
		description = "로그인한 사용자의 direct/RAG 채팅 세션 목록을 조회합니다."
	)
	ApiResponse<Page<ChatSummaryResponse>> getChats(
		@AuthenticationPrincipal CustomPrincipal principal,
		Pageable pageable
	);

	@Operation(
		summary = "채팅 상세 조회",
		description = "저장된 채팅 메시지 이력을 순서대로 조회합니다."
	)
	ApiResponse<ChatDetailResponse> getChat(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId
	);

	@Operation(
		summary = "Claude 스트리밍 채팅",
		description = "Spring AI를 통해 Anthropic 모델을 호출하고 텍스트 스트림을 그대로 전달합니다. 응답 헤더 `X-Chat-Public-Id`로 채팅 식별자를 반환합니다."
	)
	ResponseEntity<StreamingResponseBody> stream(
		@AuthenticationPrincipal CustomPrincipal principal,
		@Valid @RequestBody ChatStreamRequest request
	);
}

