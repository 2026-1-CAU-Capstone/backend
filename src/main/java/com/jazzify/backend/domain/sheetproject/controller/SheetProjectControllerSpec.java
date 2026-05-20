package com.jazzify.backend.domain.sheetproject.controller;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectCreateRequest;
import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectOmrCreateRequest;
import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectUpdateRequest;
import com.jazzify.backend.domain.sheetproject.dto.response.SheetProjectOmrCreateResponse;
import com.jazzify.backend.domain.sheetproject.dto.response.SheetProjectOmrStatusResponse;
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

	@Operation(
		summary = "OMR로 악보 프로젝트 생성 요청",
		description = """
			악보 파일 OMR 처리를 비동기로 요청하고 즉시 `SheetProject`를 생성합니다.
			
			- 생성 직후 `omrStatus=PENDING`, `omrProgress=0`
			- 실제 OMR 처리 및 코드 저장은 이벤트 리스너에서 비동기로 수행
			- 실패 시 `omrStatus=FAILED`, `omrFailureReason`에 원인 기록
			"""
	)
	ApiResponse<SheetProjectOmrCreateResponse> createFromOmr(
		@AuthenticationPrincipal CustomPrincipal principal,
		MultipartFile file,
		SheetProjectOmrCreateRequest request);

	@Operation(summary = "내 악보 프로젝트 목록 조회 (페이징)")
	ApiResponse<Page<SheetProjectResponse>> getAll(
		@AuthenticationPrincipal CustomPrincipal principal,
		Pageable pageable);

	@Operation(summary = "악보 프로젝트 단건 조회")
	ApiResponse<SheetProjectResponse> getByPublicId(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId);

	@Operation(summary = "악보 프로젝트 OMR 진행 상태 조회")
	ApiResponse<SheetProjectOmrStatusResponse> getOmrStatus(
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
