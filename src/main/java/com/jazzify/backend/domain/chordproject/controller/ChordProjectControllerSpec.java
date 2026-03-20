package com.jazzify.backend.domain.chordproject.controller;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.chordproject.dto.request.ChordProjectCreateRequest;
import com.jazzify.backend.domain.chordproject.dto.request.ChordProjectUpdateRequest;
import com.jazzify.backend.domain.chordproject.dto.response.ChordProjectResponse;
import com.jazzify.backend.shared.web.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@NullMarked
@Tag(name = "ChordProject", description = "코드 프로젝트 API")
@SecurityRequirement(name = "BearerAuth")
public interface ChordProjectControllerSpec {

    @Operation(summary = "코드 프로젝트 생성")
    ApiResponse<ChordProjectResponse> create(
            @AuthenticationPrincipal CustomPrincipal principal,
            @Valid @RequestBody ChordProjectCreateRequest request);

    @Operation(summary = "내 코드 프로젝트 목록 조회 (페이징)")
    ApiResponse<Page<ChordProjectResponse>> getAll(
            @AuthenticationPrincipal CustomPrincipal principal,
            Pageable pageable);

    @Operation(summary = "코드 프로젝트 단건 조회")
    ApiResponse<ChordProjectResponse> getByPublicId(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID publicId);

    @Operation(summary = "코드 프로젝트 수정")
    ApiResponse<ChordProjectResponse> update(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID publicId,
            @Valid @RequestBody ChordProjectUpdateRequest request);

    @Operation(summary = "코드 프로젝트 삭제")
    ApiResponse<Void> delete(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID publicId);
}
