package com.jazzify.backend.domain.solo.controller;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.domain.solo.dto.request.SoloCreateRequest;
import com.jazzify.backend.domain.solo.dto.request.SoloOmrRequest;
import com.jazzify.backend.domain.solo.dto.request.SoloUpdateRequest;
import com.jazzify.backend.domain.solo.dto.request.SoloVideoRequest;
import com.jazzify.backend.domain.solo.dto.response.SoloResponse;
import com.jazzify.backend.domain.solo.service.SoloService;
import com.jazzify.backend.shared.web.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@NullMarked
@RestController
@RequestMapping("/v1/solos")
@RequiredArgsConstructor
public class SoloController implements SoloControllerSpec {

	private final SoloService soloService;

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SoloResponse> create(
		@Valid @RequestBody SoloCreateRequest request) {
		return ApiResponse.ok(soloService.create(request));
	}

	@Override
	@GetMapping
	public ApiResponse<Page<SoloResponse>> getAll(
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return ApiResponse.ok(soloService.getAll(pageable));
	}

	@Override
	@GetMapping("/{publicId}")
	public ApiResponse<SoloResponse> getByPublicId(
		@PathVariable UUID publicId) {
		return ApiResponse.ok(soloService.getByPublicId(publicId));
	}

	@Override
	@PutMapping("/{publicId}")
	public ApiResponse<SoloResponse> update(
		@PathVariable UUID publicId,
		@Valid @RequestBody SoloUpdateRequest request) {
		return ApiResponse.ok(soloService.update(publicId, request));
	}

	@Override
	@DeleteMapping("/{publicId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ApiResponse<Void> delete(
		@PathVariable UUID publicId) {
		soloService.delete(publicId);
		return ApiResponse.ok();
	}

	@Override
	@PutMapping("/{publicId}/video")
	public ApiResponse<SoloResponse> updateVideo(
		@PathVariable UUID publicId,
		@Valid @RequestBody SoloVideoRequest request) {
		return ApiResponse.ok(soloService.updateVideo(publicId, request));
	}

	@Override
	@DeleteMapping("/{publicId}/video")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ApiResponse<Void> deleteVideo(
		@PathVariable UUID publicId) {
		soloService.deleteVideo(publicId);
		return ApiResponse.ok();
	}

	@Override
	@PostMapping(value = "/omr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SoloResponse> createFromOmr(
		@RequestPart("file") MultipartFile file,
		@Valid @ModelAttribute SoloOmrRequest metadata) {
		return ApiResponse.ok(soloService.createFromOmr(file, metadata));
	}
}

