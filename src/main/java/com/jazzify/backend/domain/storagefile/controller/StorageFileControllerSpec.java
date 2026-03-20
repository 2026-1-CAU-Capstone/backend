package com.jazzify.backend.domain.storagefile.controller;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.storagefile.dto.response.StorageFileResponse;
import com.jazzify.backend.shared.web.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@NullMarked
@Tag(name = "StorageFile", description = "파일 저장소 API")
@SecurityRequirement(name = "BearerAuth")
public interface StorageFileControllerSpec {

    @Operation(summary = "파일 업로드")
    ApiResponse<StorageFileResponse> upload(
            @AuthenticationPrincipal CustomPrincipal principal,
            @RequestParam("file") MultipartFile file);

    @Operation(summary = "파일 정보 조회")
    ApiResponse<StorageFileResponse> getByPublicId(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID publicId);
}

