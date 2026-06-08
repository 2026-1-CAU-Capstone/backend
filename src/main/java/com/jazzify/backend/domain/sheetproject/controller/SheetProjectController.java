package com.jazzify.backend.domain.sheetproject.controller;

import com.jazzify.backend.domain.chordproject.dto.request.ChordProjectOmrCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectCreateRequest;
import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectOmrCreateRequest;
import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectUpdateRequest;
import com.jazzify.backend.domain.sheetproject.dto.response.SheetProjectOmrCreateResponse;
import com.jazzify.backend.domain.sheetproject.dto.response.SheetProjectOmrStatusResponse;
import com.jazzify.backend.domain.sheetproject.dto.response.SheetProjectResponse;
import com.jazzify.backend.domain.sheetproject.service.SheetProjectService;
import com.jazzify.backend.shared.web.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tools.jackson.databind.ObjectMapper;

@NullMarked
@RestController
@RequestMapping("/v1/sheet-projects")
@RequiredArgsConstructor
public class SheetProjectController implements SheetProjectControllerSpec {
	private final ObjectMapper objectMapper;
	private final Validator validator;
	private final SheetProjectService sheetProjectService;

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	@Override
	public ApiResponse<SheetProjectResponse> create(
		@AuthenticationPrincipal CustomPrincipal principal,
		@Valid @RequestBody SheetProjectCreateRequest request) {
		return ApiResponse.ok(sheetProjectService.create(principal.publicId(), request));
	}

	@Override
	@PostMapping(value = "/omr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SheetProjectOmrCreateResponse> createFromOmr(
		@AuthenticationPrincipal CustomPrincipal principal,
		@RequestPart("file") MultipartFile file,
		@RequestPart("request") String requestJson) {
		SheetProjectOmrCreateRequest request =
			objectMapper.readValue(requestJson, SheetProjectOmrCreateRequest.class);

		Set<ConstraintViolation<SheetProjectOmrCreateRequest>> violations =
			validator.validate(request);

		if (!violations.isEmpty()) {
			throw new ConstraintViolationException(violations);
		}
		return ApiResponse.ok(sheetProjectService.createFromOmr(principal.publicId(), file, request));
	}

	@GetMapping
	@Override
	public ApiResponse<Page<SheetProjectResponse>> getAll(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return ApiResponse.ok(sheetProjectService.getAll(principal.publicId(), pageable));
	}

	@GetMapping("/{publicId}")
	@Override
	public ApiResponse<SheetProjectResponse> getByPublicId(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId) {
		return ApiResponse.ok(sheetProjectService.getByPublicId(principal.publicId(), publicId));
	}

	@GetMapping("/{publicId}/omr-status")
	public ApiResponse<SheetProjectOmrStatusResponse> getOmrStatus(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId) {
		return ApiResponse.ok(sheetProjectService.getOmrStatus(principal.publicId(), publicId));
	}

	@PutMapping("/{publicId}")
	@Override
	public ApiResponse<SheetProjectResponse> update(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId,
		@Valid @RequestBody SheetProjectUpdateRequest request) {
		return ApiResponse.ok(sheetProjectService.update(principal.publicId(), publicId, request));
	}

	@DeleteMapping("/{publicId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@Override
	public ApiResponse<Void> delete(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId) {
		sheetProjectService.delete(principal.publicId(), publicId);
		return ApiResponse.ok();
	}
}
