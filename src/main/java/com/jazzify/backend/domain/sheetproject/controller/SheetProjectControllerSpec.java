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
			악보 이미지 파일을 MusicVision 일반 악보 OMR 서버에 비동기로 제출하고 즉시 `SheetProject`를 생성합니다.
			이 API는 최종 인식 결과를 즉시 반환하지 않습니다. 응답의 `project.publicId`로 상태 조회 API를 폴링하세요.
			
			### 요청 형식
			`multipart/form-data`로 전송합니다.
			
			| 필드 | 필수 | 설명 |
			| --- | --- | --- |
			| `file` | 예 | 이미지 파일. `png`, `jpg`, `jpeg`만 허용 |
			| `title` | 아니오 | 생성 직후/완료 시 사용자 입력값을 최우선 사용. 미입력 시 생성 직후 `Untitled`, 완료 시 OMR 제목, 둘 다 없으면 `Untitled` |
			| `key` | 아니오 | `MusicKey` enum 이름 또는 조성 표기. 예: `B_FLAT_MAJOR`, `Bb`, `B flat major`, `F#m`. 미입력 시 OMR 결과에서 추론 |
			
			### MusicVision 제출 경로
			- dev profile: `/omr/dev/process`
			- prod profile: `/omr/prod/process`
			- 설정된 `omr.api-key`를 `X-OMR-API-Key`로 사용합니다.
			
			### 처리 결과
			- 백엔드가 PENDING 프로젝트와 파일 엔티티를 만든 뒤 MusicVision 제출까지 성공하면 보통 `omrStatus=PROCESSING`, `omrProgress=10` 상태로 반환합니다.
			- 생성 직후 제목은 사용자 입력값 또는 `Untitled`입니다. 더 이상 `OMR Processing`을 제목으로 저장하지 않습니다.
			- 생성 직후 `chords[]`는 비어 있습니다.
			- 실제 MusicXML/chord assignments 조회, `ChordInfo` 저장, 프로젝트 제목/조성 확정은 MusicVision callback 수신 후 수행됩니다.
			- 실패 시 `omrStatus=FAILED`, `omrFailureReason`에 원인을 기록합니다.
			
			### 에러
			- `400 OMR_004`: 지원하지 않는 파일 형식
			- `400 OMR_005`: 빈 파일
			- `500 OMR_007`: 업로드 파일 읽기 실패
			- `400 GLOBAL_002`: 유효하지 않은 `key` 문자열
			- `503 OMR_001`: OMR 서버 미설정
			- `502 OMR_002`, `502 OMR_008`, `422 OMR_003`, `422 OMR_006`: 제출 또는 callback 처리 실패 시 `omrStatus=FAILED`와 `omrFailureReason`에 반영
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

	@Operation(
		summary = "악보 프로젝트 OMR 진행 상태 조회",
		description = """
			비동기 OMR 프로젝트 생성의 현재 진행 상태를 조회합니다.
			
			- `status`: `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`
			- `progress`: 0~100 진행률. `PENDING`/`PROCESSING`이고 `omrJobId`가 있으면 MusicVision `GET /omr/jobs/{jobId}`의 최신 progress를 우선 사용합니다.
			- `failureReason`: 실패 시 원인 메시지
			
			MusicVision status 조회가 일시적으로 실패하면 DB에 마지막으로 저장된 progress를 fallback으로 반환합니다.
			"""
	)
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
