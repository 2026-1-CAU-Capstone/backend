package com.jazzify.backend.domain.storagefile.controller;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.storagefile.dto.response.StorageFileResponse;
import com.jazzify.backend.domain.storagefile.service.StorageFileService;
import com.jazzify.backend.shared.web.ApiResponse;

import lombok.RequiredArgsConstructor;

@NullMarked
@RestController
@RequestMapping("/v1/storage-files")
@RequiredArgsConstructor
public class StorageFileController implements StorageFileControllerSpec {

	private final StorageFileService storageFileService;

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<StorageFileResponse> upload(
		@AuthenticationPrincipal CustomPrincipal principal,
		@RequestParam("file") MultipartFile file) {
		return ApiResponse.ok(storageFileService.upload(file));
	}

	@GetMapping("/{publicId}")
	public ApiResponse<StorageFileResponse> getByPublicId(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId) {
		return ApiResponse.ok(storageFileService.getByPublicId(publicId));
	}
}

