package com.jazzify.backend.domain.lick.controller;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.jazzify.backend.domain.lick.dto.request.LickCreateRequest;
import com.jazzify.backend.domain.lick.dto.request.LickUpdateRequest;
import com.jazzify.backend.domain.lick.dto.response.LickResponse;
import com.jazzify.backend.shared.web.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@NullMarked
@Tag(name = "Lick", description = "릭(Lick) API")
public interface LickControllerSpec {

	@Operation(
		summary = "릭 생성",
		description = """
			새로운 릭을 생성합니다.
			
			- `title`: 릭 제목 (최대 255자, 필수)
			- `composer`: 작곡자 (최대 255자, 선택)
			- `content`: 릭 내용 (JSON 문자열, 필수)
			"""
	)
	ApiResponse<LickResponse> create(
		@Valid @RequestBody LickCreateRequest request);

	@Operation(
		summary = "릭 목록 조회 (페이징)",
		description = """
			릭 목록을 페이징하여 반환합니다.
			
			**Query Parameters**
			- `page`: 페이지 번호 (0부터 시작, 기본값 `0`)
			- `size`: 페이지당 항목 수 (기본값 `20`)
			- `sort`: 정렬 기준 (기본값 `createdAt,desc`)
			"""
	)
	ApiResponse<Page<LickResponse>> getAll(Pageable pageable);

	@Operation(
		summary = "릭 단건 조회",
		description = "`publicId`에 해당하는 릭을 조회합니다."
	)
	ApiResponse<LickResponse> getByPublicId(
		@PathVariable UUID publicId);

	@Operation(
		summary = "릭 수정",
		description = """
			릭의 제목(`title`), 작곡자(`composer`), 내용(`content`)을 수정합니다.
			
			- `composer`는 생략하거나 `null`로 전달하면 작곡자 정보가 제거됩니다.
			"""
	)
	ApiResponse<LickResponse> update(
		@PathVariable UUID publicId,
		@Valid @RequestBody LickUpdateRequest request);

	@Operation(
		summary = "릭 삭제",
		description = "`publicId`에 해당하는 릭을 삭제합니다."
	)
	ApiResponse<Void> delete(
		@PathVariable UUID publicId);
}

