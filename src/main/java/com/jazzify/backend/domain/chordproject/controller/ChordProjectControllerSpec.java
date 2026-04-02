package com.jazzify.backend.domain.chordproject.controller;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.chordproject.dto.request.AddChordsRequest;
import com.jazzify.backend.domain.chordproject.dto.request.ChordProjectCreateRequest;
import com.jazzify.backend.domain.chordproject.dto.request.ChordProjectUpdateRequest;
import com.jazzify.backend.domain.chordproject.dto.response.AnalysisResultResponse;
import com.jazzify.backend.domain.chordproject.dto.response.ChordInfoResponse;
import com.jazzify.backend.domain.chordproject.dto.response.ChordProjectResponse;
import com.jazzify.backend.shared.web.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@NullMarked
@Tag(name = "ChordProject", description = "코드 프로젝트 API")
@SecurityRequirement(name = "BearerAuth")
public interface ChordProjectControllerSpec {

	@Operation(
		summary = "코드 프로젝트 생성",
		description = """
			새로운 코드 프로젝트를 생성합니다.
			
			- `title`: 프로젝트 제목 (최대 255자, 필수)
			- `key`: 조성. MusicKey enum 이름을 문자열로 전달합니다. (예: `"G_MAJOR"`, `"B_FLAT_MAJOR"`, `"C_MINOR"`)
			- `timeSignature`: 박자 표기 (선택, 미입력 시 기본값 `"4/4"` 적용)
			"""
	)
	ApiResponse<ChordProjectResponse> create(
		@AuthenticationPrincipal CustomPrincipal principal,
		@Valid @RequestBody ChordProjectCreateRequest request);

	@Operation(
		summary = "내 코드 프로젝트 목록 조회 (페이징)",
		description = """
			로그인한 사용자 본인의 코드 프로젝트 목록을 페이징하여 반환합니다.
			
			**Query Parameters**
			- `page`: 페이지 번호 (0부터 시작, 기본값 `0`)
			- `size`: 페이지당 항목 수 (기본값 `20`)
			- `sort`: 정렬 기준 (기본값 `createdAt,desc` / 예: `title,asc`)
			"""
	)
	ApiResponse<Page<ChordProjectResponse>> getAll(
		@AuthenticationPrincipal CustomPrincipal principal,
		Pageable pageable);

	@Operation(
		summary = "코드 프로젝트 단건 조회",
		description = """
			`publicId`에 해당하는 코드 프로젝트를 조회합니다.
			본인 소유의 프로젝트만 조회할 수 있습니다.
			"""
	)
	ApiResponse<ChordProjectResponse> getByPublicId(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId);

	@Operation(
		summary = "코드 프로젝트 수정",
		description = """
			코드 프로젝트의 제목(`title`)과 조성(`key`)을 수정합니다.
			박자(`timeSignature`)는 수정할 수 없습니다.
			본인 소유의 프로젝트만 수정할 수 있습니다.
			"""
	)
	ApiResponse<ChordProjectResponse> update(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId,
		@Valid @RequestBody ChordProjectUpdateRequest request);

	@Operation(
		summary = "코드 프로젝트 삭제",
		description = """
			`publicId`에 해당하는 코드 프로젝트를 삭제합니다.
			프로젝트에 속한 코드 정보 및 분석 결과도 함께 삭제됩니다.
			본인 소유의 프로젝트만 삭제할 수 있습니다.
			"""
	)
	ApiResponse<Void> delete(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId);

	@Operation(
		summary = "코드 프로젝트에 코드 정보 등록 (기존 코드 덮어쓰기)",
		description = """
			프로젝트에 속한 **기존 코드를 전부 삭제**하고 새로운 코드 목록으로 교체합니다.
			처음 입력할 때와 수정할 때 모두 이 API를 사용합니다.
			
			**`chords` 배열 항목 필드**
			- `chord`: 코드 이름 (예: `"Dm7"`, `"G7b9"`). 쉬는 마디나 반복 마디는 `null`로 전달합니다.
			- `bar`: 마디 번호 (1부터 시작, 필수)
			- `beat`: 마디 안에서 코드가 시작하는 박 위치 (예: 1번째 박 → `1.0`, 3번째 박 → `3.0`, 필수)
			- `durationBeats`: 코드가 지속되는 박 수 (0 이상, 필수)
			
			**예시 – 4/4 박자 한 마디에 코드 2개:**
			```json
			[
			  { "chord": "Dm7", "bar": 1, "beat": 1.0, "durationBeats": 2.0 },
			  { "chord": "G7",  "bar": 1, "beat": 3.0, "durationBeats": 2.0 }
			]
			```
			
			등록 직후 각 코드의 `analysis` 필드는 `null`입니다. 분석 API 호출 후 채워집니다.
			"""
	)
	ApiResponse<List<ChordInfoResponse>> addChords(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId,
		@Valid @RequestBody AddChordsRequest request);

	@Operation(
		summary = "코드 프로젝트 화성 분석 실행",
		description = """
			저장된 코드 정보를 기반으로 규칙 기반 화성 분석을 실행하고, 결과를 DB에 저장한 뒤 반환합니다.
			**코드를 먼저 등록(`POST /{publicId}/chords`)한 후 호출해야 합니다.**
			
			**응답 구조 요약**
			- `chords[]`: 각 코드의 분석 결과 (`degree`, `isDiatonic`, `functions`, `ambiguityScore` 등)
			- `groups[]`: 여러 코드가 함께 이루는 화성 패턴 그룹 (예: II-V-I, 세컨더리 도미넌트)
			  - `members[].chordInfoPublicId`를 `chords[].publicId`와 매칭하면 어떤 코드가 그룹에 속하는지 확인할 수 있습니다.
			- `sections[]`: 곡 전체를 조성/모드 기준으로 나눈 구간 분석
			- `ambiguityStats`: 전체 코드의 해석 모호성 통계 (`0.0` = 명확, `1.0` = 매우 모호)
			
			분석 결과는 DB에 저장되므로 재호출 시 이전 결과를 덮어씁니다.
			"""
	)
	ApiResponse<AnalysisResultResponse> analyze(
		@AuthenticationPrincipal CustomPrincipal principal,
		@PathVariable UUID publicId);
}
