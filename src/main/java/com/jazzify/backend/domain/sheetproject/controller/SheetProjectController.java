package com.jazzify.backend.domain.sheetproject.controller;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectCreateRequest;
import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectUpdateRequest;
import com.jazzify.backend.domain.sheetproject.dto.response.SheetProjectResponse;
import com.jazzify.backend.domain.sheetproject.service.SheetProjectService;
import com.jazzify.backend.shared.web.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@NullMarked
@RestController
@RequestMapping("/v1/sheet-projects")
@RequiredArgsConstructor
public class SheetProjectController implements SheetProjectControllerSpec {

	private final SheetProjectService sheetProjectService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SheetProjectResponse> create(
		@AuthenticationPrincipal CustomPrincipal principal,
		@Valid @RequestBody SheetProjectCreateRequest request) {
		return ApiResponse.ok(sheetProjectService.create(principal.publicId(), request));
	}

	@GetMapping
	public ApiResponse<Page<SheetProjectResponse>> getAll(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return ApiResponse.ok(sheetProjectService.getAll(principal.publicId(), pageable));
	}

	@GetMapping("/{publicId}")
	public ApiResponse<SheetProjectResponse> getByPublicId(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId) {
		return ApiResponse.ok(sheetProjectService.getByPublicId(principal.publicId(), publicId));
	}

	@PutMapping("/{publicId}")
	public ApiResponse<SheetProjectResponse> update(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId,
		@Valid @RequestBody SheetProjectUpdateRequest request) {
		return ApiResponse.ok(sheetProjectService.update(principal.publicId(), publicId, request));
	}

	@DeleteMapping("/{publicId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ApiResponse<Void> delete(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId) {
		sheetProjectService.delete(principal.publicId(), publicId);
		return ApiResponse.ok();
	}
}
