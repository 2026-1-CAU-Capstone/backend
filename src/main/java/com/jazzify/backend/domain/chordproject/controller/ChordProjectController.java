package com.jazzify.backend.domain.chordproject.controller;

import java.util.List;
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
import com.jazzify.backend.domain.analysis.dto.response.AnalysisExplanationResponse;
import com.jazzify.backend.domain.chordproject.dto.request.AddChordsRequest;
import com.jazzify.backend.domain.chordproject.dto.request.ChordProjectCreateRequest;
import com.jazzify.backend.domain.chordproject.dto.request.ChordProjectUpdateRequest;
import com.jazzify.backend.domain.chordproject.dto.response.AnalysisResultResponse;
import com.jazzify.backend.domain.chordproject.dto.response.ChordInfoResponse;
import com.jazzify.backend.domain.chordproject.dto.response.ChordProjectResponse;
import com.jazzify.backend.domain.chordproject.service.ChordProjectService;
import com.jazzify.backend.shared.web.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@NullMarked
@RestController
@RequestMapping("/v1/chord-projects")
@RequiredArgsConstructor
public class ChordProjectController implements ChordProjectControllerSpec {

	private final ChordProjectService chordProjectService;

	@Override
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<ChordProjectResponse> create(
		@AuthenticationPrincipal CustomPrincipal principal,
		@Valid @RequestBody ChordProjectCreateRequest request) {
		return ApiResponse.ok(chordProjectService.create(principal.publicId(), request));
	}

	@Override
	@GetMapping
	public ApiResponse<Page<ChordProjectResponse>> getAll(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
		return ApiResponse.ok(chordProjectService.getAll(principal.publicId(), pageable));
	}

	@Override
	@GetMapping("/{publicId}")
	public ApiResponse<ChordProjectResponse> getByPublicId(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId) {
		return ApiResponse.ok(chordProjectService.getByPublicId(principal.publicId(), publicId));
	}

	@Override
	@PutMapping("/{publicId}")
	public ApiResponse<ChordProjectResponse> update(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId,
		@Valid @RequestBody ChordProjectUpdateRequest request) {
		return ApiResponse.ok(chordProjectService.update(principal.publicId(), publicId, request));
	}

	@Override
	@DeleteMapping("/{publicId}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public ApiResponse<Void> delete(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId) {
		chordProjectService.delete(principal.publicId(), publicId);
		return ApiResponse.ok();
	}

	@Override
	@PostMapping("/{publicId}/chords")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<List<ChordInfoResponse>> addChords(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId,
		@Valid @RequestBody AddChordsRequest request) {
		return ApiResponse.ok(chordProjectService.addChords(principal.publicId(), publicId, request));
	}

	@Override
	@PostMapping("/{publicId}/analyze")
	public ApiResponse<AnalysisResultResponse> analyze(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId) {
		return ApiResponse.ok(chordProjectService.analyze(principal.publicId(), publicId));
	}

	@Override
	@GetMapping("/{publicId}/analysis")
	public ApiResponse<AnalysisResultResponse> getAnalysisResult(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId) {
		return ApiResponse.ok(chordProjectService.getAnalysisResult(principal.publicId(), publicId));
	}

	@Override
	@GetMapping("/{publicId}/explain")
	public ApiResponse<AnalysisExplanationResponse> explain(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId) {
		return ApiResponse.ok(chordProjectService.explain(principal.publicId(), publicId));
	}
}
