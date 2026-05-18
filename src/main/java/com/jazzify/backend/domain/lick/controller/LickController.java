package com.jazzify.backend.domain.lick.controller;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.domain.lick.dto.request.LickCreateRequest;
import com.jazzify.backend.domain.lick.dto.request.LickOmrRequest;
import com.jazzify.backend.domain.lick.dto.request.LickUpdateRequest;
import com.jazzify.backend.domain.lick.dto.request.LickVideoRequest;
import com.jazzify.backend.domain.lick.dto.response.LickMetadataValueCountResponse;
import com.jazzify.backend.domain.lick.dto.response.LickResponse;
import com.jazzify.backend.domain.lick.service.LickService;
import com.jazzify.backend.shared.web.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@NullMarked
@RestController
@RequestMapping("/v1/licks")
@RequiredArgsConstructor
public class LickController implements LickControllerSpec {

	private final LickService lickService;

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<LickResponse> create(
		@Valid @RequestBody LickCreateRequest request) {
		return ApiResponse.ok(lickService.create(request));
	}

	@Override
	@GetMapping
	public ApiResponse<Page<LickResponse>> getAll(
		@RequestParam(required = false) @Nullable String composer,
		@RequestParam(required = false) @Nullable String performer,
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return ApiResponse.ok(lickService.getAll(pageable, composer, performer));
	}

	@Override
	@GetMapping("/composers")
	public ApiResponse<List<LickMetadataValueCountResponse>> getComposerCounts(
		@RequestParam(required = false) @Nullable String performer) {
		return ApiResponse.ok(lickService.getComposerCounts(performer));
	}

	@Override
	@GetMapping("/performers")
	public ApiResponse<List<LickMetadataValueCountResponse>> getPerformerCounts(
		@RequestParam(required = false) @Nullable String composer) {
		return ApiResponse.ok(lickService.getPerformerCounts(composer));
	}

	@Override
	@GetMapping("/{publicId}")
	public ApiResponse<LickResponse> getByPublicId(
		@PathVariable UUID publicId) {
		return ApiResponse.ok(lickService.getByPublicId(publicId));
	}

	@Override
	@PutMapping("/{publicId}")
	public ApiResponse<LickResponse> update(
		@PathVariable UUID publicId,
		@Valid @RequestBody LickUpdateRequest request) {
		return ApiResponse.ok(lickService.update(publicId, request));
	}

	@Override
	@DeleteMapping("/{publicId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ApiResponse<Void> delete(
		@PathVariable UUID publicId) {
		lickService.delete(publicId);
		return ApiResponse.ok();
	}

	@Override
	@PutMapping("/{publicId}/video")
	public ApiResponse<LickResponse> updateVideo(
		@PathVariable UUID publicId,
		@Valid @RequestBody LickVideoRequest request) {
		return ApiResponse.ok(lickService.updateVideo(publicId, request));
	}

	@Override
	@DeleteMapping("/{publicId}/video")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ApiResponse<Void> deleteVideo(
		@PathVariable UUID publicId) {
		lickService.deleteVideo(publicId);
		return ApiResponse.ok();
	}

	@Override
	@PostMapping(value = "/omr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<LickResponse> createFromOmr(
		@RequestPart("file") MultipartFile file,
		@Valid @ModelAttribute LickOmrRequest metadata) {
		return ApiResponse.ok(lickService.createFromOmr(file, metadata));
	}
}

