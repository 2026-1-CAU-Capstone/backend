package com.jazzify.backend.domain.lick.controller;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jazzify.backend.domain.lick.dto.request.LickCreateRequest;
import com.jazzify.backend.domain.lick.dto.request.LickUpdateRequest;
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
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return ApiResponse.ok(lickService.getAll(pageable));
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
}

