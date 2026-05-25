package com.jazzify.backend.domain.rag.controller;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.chat.service.ChatService;
import com.jazzify.backend.domain.rag.dto.request.RagChatRequest;
import com.jazzify.backend.domain.rag.dto.request.RagDocumentCreateRequest;
import com.jazzify.backend.domain.rag.dto.request.RagDocumentUpdateRequest;
import com.jazzify.backend.domain.rag.dto.response.RagDocumentResponse;
import com.jazzify.backend.domain.rag.dto.response.RagDocumentSummaryResponse;
import com.jazzify.backend.domain.rag.dto.response.RagHealthResponse;
import com.jazzify.backend.domain.rag.dto.response.RagSearchResponse;
import com.jazzify.backend.domain.rag.service.RagService;
import com.jazzify.backend.shared.web.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@NullMarked
@RequiredArgsConstructor
@RequestMapping("/v1/rag")
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagController implements RagControllerSpec {

	private final RagService ragService;

	@Override
	@GetMapping("/documents")
	public ApiResponse<Page<RagDocumentSummaryResponse>> getDocuments(
		@RequestParam(required = false) @Nullable String sourceType,
		@RequestParam(required = false, name = "q") @Nullable String query,
		@PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return ApiResponse.ok(ragService.getDocuments(pageable, sourceType, query));
	}

	@Override
	@GetMapping("/documents/{publicId}")
	public ApiResponse<RagDocumentResponse> getDocumentByPublicId(@PathVariable UUID publicId) {
		return ApiResponse.ok(ragService.getDocumentByPublicId(publicId));
	}

	@Override
	@PostMapping("/documents")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<RagDocumentResponse> createDocument(@Valid @RequestBody RagDocumentCreateRequest request) {
		return ApiResponse.ok(ragService.createDocument(request));
	}

	@Override
	@PutMapping("/documents/{publicId}")
	public ApiResponse<RagDocumentResponse> updateDocument(
		@PathVariable UUID publicId,
		@Valid @RequestBody RagDocumentUpdateRequest request
	) {
		return ApiResponse.ok(ragService.updateDocument(publicId, request));
	}

	@Override
	@DeleteMapping("/documents/{publicId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ApiResponse<Void> deleteDocument(@PathVariable UUID publicId) {
		ragService.deleteDocument(publicId);
		return ApiResponse.ok();
	}

	@Override
	@PostMapping(value = "/chat", produces = MediaType.TEXT_PLAIN_VALUE)
	public ResponseEntity<StreamingResponseBody> chat(
		@AuthenticationPrincipal CustomPrincipal principal,
		@Valid @RequestBody RagChatRequest request
	) {
		ChatService.PreparedChatStream preparedChatStream = ragService.prepareChat(principal, request);
		StreamingResponseBody body = outputStream -> ragService.streamChat(preparedChatStream, request, outputStream);
		return ResponseEntity.ok()
			.contentType(MediaType.TEXT_PLAIN)
			.cacheControl(CacheControl.noCache())
			.header("X-Chat-Public-Id", preparedChatStream.chatPublicId().toString())
			.header("X-Accel-Buffering", "no")
			.header("Cache-Control", "no-cache, no-transform")
			.body(body);
	}

	@Override
	@GetMapping("/search")
	public ApiResponse<RagSearchResponse> search(
		@RequestParam("q") String query,
		@RequestParam(required = false) @Nullable Integer level,
		@RequestParam(defaultValue = "5") int n,
		@RequestParam(required = false) @Nullable String song,
		@RequestParam(required = false) @Nullable String tag,
		@RequestParam(required = false) @Nullable String sourceType
	) {
		return ApiResponse.ok(ragService.search(query, level, n, song, tag, sourceType));
	}

	@Override
	@GetMapping("/health")
	public ApiResponse<RagHealthResponse> health() {
		return ApiResponse.ok(ragService.health());
	}
}



