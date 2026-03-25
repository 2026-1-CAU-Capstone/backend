package com.jazzify.backend.domain.sheetproject.controller;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectCreateRequest;
import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectUpdateRequest;
import com.jazzify.backend.domain.sheetproject.dto.response.SheetProjectResponse;
import com.jazzify.backend.shared.web.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@NullMarked
@Tag(name = "SheetProject", description = "악보 프로젝트 API")
@SecurityRequirement(name = "BearerAuth")
public interface SheetProjectControllerSpec {

	@Operation(summary = "악보 프로젝트 생성")
	ApiResponse<SheetProjectResponse> create(
		@AuthenticationPrincipal CustomPrincipal principal,
		@Valid @RequestBody SheetProjectCreateRequest request);

	@Operation(summary = "내 악보 프로젝트 목록 조회 (페이징)")
	ApiResponse<Page<SheetProjectResponse>> getAll(
		@AuthenticationPrincipal CustomPrincipal principal,
		Pageable pageable);

	@Operation(summary = "악보 프로젝트 단건 조회")
	ApiResponse<SheetProjectResponse> getByPublicId(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId);

	@Operation(summary = "악보 프로젝트 수정")
	ApiResponse<SheetProjectResponse> update(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId,
		@Valid @RequestBody SheetProjectUpdateRequest request);

	@Operation(summary = "악보 프로젝트 삭제")
	ApiResponse<Void> delete(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId);
}
