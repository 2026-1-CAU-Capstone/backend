package com.jazzify.backend.domain.chat.controller;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.chat.dto.request.ChatStreamRequest;
import com.jazzify.backend.domain.chat.dto.response.ChatDetailResponse;
import com.jazzify.backend.domain.chat.dto.response.ChatSummaryResponse;
import com.jazzify.backend.domain.chat.service.ChatService;
import com.jazzify.backend.shared.web.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@NullMarked
@RequiredArgsConstructor
@RequestMapping("/v1/chat")
public class ChatController implements ChatControllerSpec {

	private final ChatService chatService;

	@Override
	@GetMapping
	public ApiResponse<Page<ChatSummaryResponse>> getChats(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return ApiResponse.ok(chatService.getChats(principal, pageable));
	}

	@Override
	@GetMapping("/{publicId}")
	public ApiResponse<ChatDetailResponse> getChat(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable java.util.UUID publicId
	) {
		return ApiResponse.ok(chatService.getChat(principal, publicId));
	}

	@Override
	@PostMapping(value = "/stream", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<StreamingResponseBody> stream(
		@AuthenticationPrincipal CustomPrincipal principal,
		@Valid @RequestBody ChatStreamRequest request
	) {
		ChatService.PreparedChatStream preparedChatStream = chatService.prepareDirectStream(principal, request);
		StreamingResponseBody body = outputStream -> chatService.streamPreparedDirect(preparedChatStream, request, outputStream);
		return ResponseEntity.ok()
			.contentType(MediaType.TEXT_PLAIN)
			.cacheControl(CacheControl.noCache())
			.header("X-Chat-Public-Id", preparedChatStream.chatPublicId().toString())
			.header("X-Accel-Buffering", "no")
			.header("Cache-Control", "no-cache, no-transform")
			.body(body);
	}
}

