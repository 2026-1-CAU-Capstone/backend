package com.jazzify.backend.domain.chordproject.controller;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.chordproject.dto.request.ChordProjectCreateRequest;
import com.jazzify.backend.domain.chordproject.dto.request.ChordProjectUpdateRequest;
import com.jazzify.backend.domain.chordproject.dto.response.ChordProjectResponse;
import com.jazzify.backend.domain.chordproject.service.ChordProjectService;
import com.jazzify.backend.shared.web.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@NullMarked
@RestController
@RequestMapping("/v1/chord-projects")
@RequiredArgsConstructor
public class ChordProjectController implements ChordProjectControllerSpec {

    private final ChordProjectService chordProjectService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ChordProjectResponse> create(
            @AuthenticationPrincipal CustomPrincipal principal,
            @Valid @RequestBody ChordProjectCreateRequest request) {
        return ApiResponse.ok(chordProjectService.create(principal.publicId(), request));
    }

    @GetMapping
    public ApiResponse<Page<ChordProjectResponse>> getAll(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ApiResponse.ok(chordProjectService.getAll(principal.publicId(), pageable));
    }

    @GetMapping("/{publicId}")
    public ApiResponse<ChordProjectResponse> getByPublicId(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID publicId) {
        return ApiResponse.ok(chordProjectService.getByPublicId(principal.publicId(), publicId));
    }

    @PutMapping("/{publicId}")
    public ApiResponse<ChordProjectResponse> update(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID publicId,
            @Valid @RequestBody ChordProjectUpdateRequest request) {
        return ApiResponse.ok(chordProjectService.update(principal.publicId(), publicId, request));
    }

    @DeleteMapping("/{publicId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal CustomPrincipal principal,
            @PathVariable UUID publicId) {
        chordProjectService.delete(principal.publicId(), publicId);
        return ApiResponse.ok();
    }
}
