package com.jazzify.backend.domain.lick.controller;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.domain.lick.dto.request.LickCreateRequest;
import com.jazzify.backend.domain.lick.dto.request.LickOmrRequest;
import com.jazzify.backend.domain.lick.dto.request.LickUpdateRequest;
import com.jazzify.backend.domain.lick.dto.request.LickVideoRequest;
import com.jazzify.backend.domain.lick.dto.response.LickMetadataValueCountResponse;
import com.jazzify.backend.domain.lick.dto.response.LickResponse;
import com.jazzify.backend.shared.web.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;

@NullMarked
@Tag(name = "Lick", description = "릭(Lick) CRUD API — 재즈 프레이즈(릭) 저장·조회·수정·삭제")
public interface LickControllerSpec {

	@Operation(
		summary = "릭 생성",
		description = """
			새로운 릭을 생성합니다.
			
			## 섹션 구조
			
			요청 바디는 5개 섹션으로 구성됩니다.
			**섹션 3(화성 맥락)과 섹션 5(유사도 피처)는 완전 생략이 가능**하며, 미입력 시 `sheetData`로부터 자동 계산됩니다.
			
			---
			
			### 1. IDENTITY — 식별 & 출처
			
			| 필드 | 타입 | 필수 | 설명 |
			|------|------|------|------|
			| `source` | string | ✅ | 출처: `user` (사용자 입력) · `weimar` (Weimar Jazz Database) · `curated` (큐레이션) |
			| `userId` | UUID | - | 릭 소유자 ID. 생략하거나 `null`이면 공개/내장 릭으로 처리됩니다. |
			| `sourceUrl` | string | - | 출처 영상·링크 URL (YouTube, Spotify, SoundCloud 등). 최대 512자. |
			
			---
			
			### 2. PERFORMANCE METADATA — 연주 정보
			
			| 필드 | 타입 | 필수 | 설명 |
			|------|------|------|------|
			| `performer` | string | - | 연주자 이름 (예: `"Cannonball Adderley"`) |
			| `composer` | string | - | 작곡자 이름 (예: `"Joseph Kosma"`) |
			| `title` | string | ✅ | 곡 제목 (최대 255자) |
			| `album` | string | - | 앨범명 (예: `"Somethin' Else"`) |
			| `instrument` | string | ✅ | 악기 코드: `as`(알토 색소폰) · `ts`(테너 색소폰) · `tp`(트럼펫) · `p`(피아노) · `g`(기타) · `b`(베이스) · `voc`(보컬) · `cl`(클라리넷) |
			| `style` | string | - | 재즈 스타일: `SWING` · `BEBOP` · `HARDBOP` · `COOL` · `MODAL` · `FUSION` |
			| `tempo` | integer | - | 템포 BPM (1 ~ 500) |
			| `key` | string | - | 조성. Weimar 포맷 사용 (예: `"Bb-maj"`, `"C-min"`) |
			| `rhythmFeel` | string | - | 리듬감: `SWING` · `STRAIGHT` · `BOSSA` · `LATIN` |
			| `timeSignature` | string | - | 박자표 (예: `"4/4"`, `"3/4"`) |
			
			---
			
			### 3. HARMONIC CONTEXT — 화성 맥락 _(자동 추출 가능)_
			
			생략 시 `sheetData`의 마디 코드 표기로부터 자동 추출됩니다.
			
			| 필드 | 타입 | 설명 |
			|------|------|------|
			| `chords` | string[] | 이 릭에 등장하는 코드 목록 (중복 없는 순서 보존). 예: `["D-7", "G7", "CMaj7"]` |
			| `chordsPerNote` | string[] | 각 선율 음표 아래의 코드. 음표 수와 배열 길이가 일치해야 합니다. |
			| `harmonicContext` | string | 화성 진행 패턴: `ii-V-I` · `minor-ii-V` · `blues` · `modal` · `turnaround` · `other` |
			| `targetChord` | string | 마지막 해결 코드 (예: `"CMaj7"`). 생략 시 chords 마지막 요소로 설정됩니다. |
			
			> **자동 감지 규칙**: 단7 → 속7 → 장7 패턴이면 `ii-V-I`, 반감음7 → 속7 이면 `minor-ii-V`, 속7 코드가 2개 이상이면 `blues`, 그 외는 `other`
			
			---
			
			### 4. SHEET DATA — 악보 데이터 _(필수)_
			
			VexFlow로 렌더링하는 악보 데이터입니다.
			
			#### sheetData 최상위 필드
			
			| 필드 | 타입 | 설명 |
			|------|------|------|
			| `title` | string | 악보 제목 |
			| `key` | string | 악보 조성 (예: `"Bb"`, `"F#"`) |
			| `timeSignature` | string | 악보 박자표 (예: `"4/4"`) |
			| `tempo` | integer | 악보 템포 BPM |
			| `measures` | array | 마디 배열 **(필수, 1개 이상)** |
			
			#### measures 배열 요소
			
			| 필드 | 타입 | 설명 |
			|------|------|------|
			| `chord` | string | 마디 코드 표기 (예: `"D-7"`, 두 코드는 `"D-7  G7"`) |
			| `notes` | array | 음표 배열 **(필수, 1개 이상)** |
			
			#### notes 배열 요소 (NoteInfo)
			
			| 필드 | 타입 | 필수 | 설명 |
			|------|------|------|------|
			| `keys` | string[] | ✅ | 음표 음고 (예: `["d/5"]`). 포맷: `음이름소문자/옥타브`. 화음이면 여러 개. |
			| `duration` | string | ✅ | 음길이 코드: `w`(온음표) · `h`(2분) · `q`(4분) · `8`(8분) · `16`(16분). 쉼표는 `r` 접미사 (`"qr"`, `"8r"` 등) |
			| `accidentals` | object | - | 임시표 맵 (예: `{"0": "b"}`, `{"0": "#"}`, `{"0": "n"}`). 키는 notes 내 인덱스 |
			| `tuplet` | integer | - | 셋잇단음표 그룹 크기 (항상 `3`). 반드시 연속 3음에 지정해야 합니다. |
			| `dotted` | boolean | - | `true`이면 점음표 (음길이 × 1.5). 쉼표에도 적용 가능. |
			| `tie` | boolean | - | `true`이면 다음 음표로 타이 연결 (같은 음고여야 함). 마디를 넘는 타이도 가능. |
			| `gliss` | boolean | - | `true`이면 다음 음까지 글리산도. |
			| `beamBreak` | boolean | - | `true`이면 이 음 이후 빔 강제 분리. 셋잇단 그룹 사이 분리에 주로 사용. |
			
			> **`keys` 포맷**: `"c/4"` = middle C (MIDI 60), `"d/5"` = D5, `"f#5"` 같은 샤프 표기는 사용하지 않고 반드시 `accidentals` 로 처리합니다.
			
			**sheetData 예시:**
			```json
			{
			  "title": "Autumn Leaves",
			  "composer": "Cannonball Adderley",
			  "key": "Bb",
			  "timeSignature": "4/4",
			  "tempo": 160,
			  "measures": [
			    {
			      "chord": "D-7",
			      "notes": [
			        { "keys": ["d/5"], "duration": "8" },
			        { "keys": ["f/5"], "duration": "8" },
			        { "keys": ["a/5"], "duration": "q" }
			      ]
			    },
			    {
			      "chord": "G7",
			      "notes": [
			        { "keys": ["b/4"], "duration": "8", "accidentals": {"0": "b"} },
			        { "keys": ["d/5"], "duration": "8" },
			        { "keys": ["f/5"], "duration": "q", "tie": true }
			      ]
			    },
			    {
			      "chord": "CMaj7",
			      "notes": [
			        { "keys": ["f/5"], "duration": "q" },
			        { "keys": ["e/5"], "duration": "h" }
			      ]
			    }
			  ]
			}
			```
			
			---
			
			### 5. SIMILARITY FEATURES — 유사도 피처 _(자동 계산 가능)_
			
			`features` 객체 자체를 생략하거나 `null`로 보내면 **모든 피처가 자동 계산**됩니다.
			`features` 내 개별 필드를 부분 입력하면 해당 필드만 사용하고, 나머지는 자동 계산됩니다.
			
			| 필드 | 타입 | 설명 |
			|------|------|------|
			| `nEvents` | integer | 음표 수 (쉼표 제외, 타이는 1로 합산) |
			| `pitches` | int[] | MIDI 절대 음고 배열 (예: `[62, 65, 69]`) |
			| `intervals` | int[] | 반음 단위 음정 차이 배열 (예: `[3, 4, -2]`) |
			| `parsons` | int[] | 선율 윤곽 (`1`=상행, `-1`=하행, `0`=동일) |
			| `fuzzyIntervals` | int[] | 묶음 음정 (`±1`=반음/온음, `±2`=3~5반음 도약, `±3`=6반음 이상 큰도약) |
			| `durationClasses` | int[] | 음길이 범주 (`2`=2박 이상, `1`=1박, `0`=8분음표, `-1`=16분음표) |
			| `pitchMin` | integer | 최저음 MIDI |
			| `pitchMax` | integer | 최고음 MIDI |
			| `pitchRange` | integer | 음역대 (`pitchMax - pitchMin`) |
			| `pitchMean` | double | 평균 음고 |
			| `startPitch` | integer | 첫 음 MIDI |
			| `endPitch` | integer | 끝 음 MIDI |
			
			> **타이 처리**: 타이로 묶인 음은 첫 번째 음만 피처 계산에 포함됩니다.
			> **쉼표 처리**: `nEvents`, `pitches`, `intervals` 계산 시 쉼표는 제외됩니다.
			""",
		requestBody = @RequestBody(
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					name = "Charlie Parker — ii-V-I Bebop Lick (알토 색소폰, 비밥) — 특수 기능 전체 포함",
					summary = "임시표·점음표·셋잇단·beamBreak·쉼표·타이·글리산도·화음 전 기능 포함 예시",
					value = """
						{
						  "source": "user",
						  "userId": null,
						  "sourceUrl": "https://www.youtube.com/watch?v=example",
						  "performer": "Charlie Parker",
									  "composer": "Charlie Parker",
						  "title": "Ko-Ko — ii-V-I Bebop Lick",
						  "album": null,
						  "instrument": "as",
						  "style": "BEBOP",
						  "tempo": 200,
						  "key": "C-maj",
						  "rhythmFeel": "SWING",
						  "timeSignature": "4/4",
						  "chords": ["D-7", "G7", "CMaj7"],
						  "chordsPerNote": null,
						  "harmonicContext": "ii-V-I",
						  "targetChord": "CMaj7",
						  "sheetData": {
						    "title": "Ko-Ko — ii-V-I Bebop Lick",
						    "key": "C",
						    "timeSignature": "4/4",
						    "tempo": 200,
						    "measures": [
						      {
						        "chord": "D-7",
						        "notes": [
						          { "keys": ["d/5"], "duration": "8" },
						          { "keys": ["f/5"], "duration": "8" },
						          { "keys": ["a/5"], "duration": "8" },
						          { "keys": ["e/5"], "duration": "8", "accidentals": {"0": "b"} },
						          { "keys": ["d/5"], "duration": "q", "dotted": true },
						          { "keys": ["c/5"], "duration": "8" }
						        ]
						      },
						      {
						        "chord": "G7",
						        "notes": [
						          { "keys": ["b/4"], "duration": "8", "tuplet": 3 },
						          { "keys": ["d/5"], "duration": "8", "tuplet": 3 },
						          { "keys": ["f/5"], "duration": "8", "tuplet": 3, "beamBreak": true },
						          { "keys": ["g/5"], "duration": "8", "tuplet": 3 },
						          { "keys": ["b/4"], "duration": "8r", "tuplet": 3 },
						          { "keys": ["a/5"], "duration": "8", "tuplet": 3, "beamBreak": true },
						          { "keys": ["g/5"], "duration": "q" },
						          { "keys": ["f/5"], "duration": "q", "tie": true }
						        ]
						      },
						      {
						        "chord": "CMaj7",
						        "notes": [
						          { "keys": ["f/5"], "duration": "8" },
						          { "keys": ["b/4"], "duration": "8r" },
						          { "keys": ["e/5"], "duration": "8", "gliss": true },
						          { "keys": ["g/5"], "duration": "8" },
						          { "keys": ["c/5", "e/5", "g/5"], "duration": "h" }
						        ]
						      }
						    ]
						  },
						  "features": null
						}
						"""
				)
			)
		)
	)
	ApiResponse<LickResponse> create(LickCreateRequest request);

	@Operation(
		summary = "릭 목록 조회 (페이징)",
		description = """
			릭 목록을 페이징하여 반환합니다.
			
			### Query Parameters
			
			| 파라미터 | 기본값 | 설명 |
			|---------|-------|------|
			| `composer` | - | Performance Metadata의 작곡자명과 정확히 일치하는 릭만 조회합니다. |
			| `performer` | - | 연주자명과 정확히 일치하는 릭만 조회합니다. |
			| `page` | `0` | 페이지 번호 (0부터 시작) |
			| `size` | `20` | 페이지당 항목 수 |
			| `sort` | `createdAt,desc` | 정렬 기준. 정렬 가능한 필드: `createdAt`, `updatedAt`, `title` |
			
			### 응답
			
			`Page<LickResponse>` 형태로 반환합니다.
			각 릭은 5개 섹션(Identity, Performance, Harmonic, SheetData, Features) 정보를 모두 포함합니다.
			"""
	)
	ApiResponse<Page<LickResponse>> getAll(@RequestParam(required = false) @Nullable String composer,
		@RequestParam(required = false) @Nullable String performer,
		Pageable pageable);

	@Operation(
		summary = "릭 composer 목록 조회",
		description = """
			현재 저장된 릭의 composer 목록과 각 composer에 해당하는 데이터 수를 반환합니다.
			
			### Query Parameters
			| 파라미터 | 기본값 | 설명 |
			|---------|-------|------|
			| `performer` | - | 특정 연주자에 해당하는 릭만 대상으로 composer 목록을 집계합니다. |
			"""
	)
	ApiResponse<List<LickMetadataValueCountResponse>> getComposerCounts(
		@RequestParam(required = false) @Nullable String performer);

	@Operation(
		summary = "릭 performer 목록 조회",
		description = """
			현재 저장된 릭의 performer 목록과 각 performer에 해당하는 데이터 수를 반환합니다.
			
			### Query Parameters
			| 파라미터 | 기본값 | 설명 |
			|---------|-------|------|
			| `composer` | - | 특정 composer에 해당하는 릭만 대상으로 performer 목록을 집계합니다. |
			"""
	)
	ApiResponse<List<LickMetadataValueCountResponse>> getPerformerCounts(
		@RequestParam(required = false) @Nullable String composer);

	@Operation(
		summary = "릭 단건 조회",
		description = """
			`publicId`에 해당하는 릭 1건을 조회합니다.
			
			### 응답 구조
			
			응답 바디는 생성 시 저장된 5개 섹션 데이터를 모두 포함합니다.
			- **섹션 1 (identity)**: `publicId`, `source`, `userId`, `isOMR`, `sourceUrl`, `createdAt`, `updatedAt`
			- **섹션 2 (performance)**: `performer`, `composer`, `title`, `album`, `instrument`, `style`, `tempo`, `key`, `rhythmFeel`, `timeSignature`
			- **섹션 3 (harmonic)**: `chords`, `chordsPerNote`, `harmonicContext`, `targetChord`
			- **섹션 4 (sheetData)**: VexFlow 렌더링용 중첩 객체 (`measures[].notes[]` 포함)
			- **섹션 5 (features)**: `nEvents`, `pitches`, `intervals`, `parsons`, `fuzzyIntervals`, `durationClasses`, `pitchMin`, `pitchMax`, `pitchRange`, `pitchMean`, `startPitch`, `endPitch`
			
			### 에러
			- `404 LICK_001`: 해당 `publicId`의 릭이 존재하지 않을 경우 반환됩니다.
			"""
	)
	ApiResponse<LickResponse> getByPublicId(UUID publicId);

	@Operation(
		summary = "릭 수정",
		description = """
			릭의 내용을 수정합니다. **(PUT — 전체 교체 방식)**
			
			### 수정 불가 필드
			- `source`, `userId`: 생성 시 설정된 출처·소유자 정보는 변경되지 않습니다.
			- `publicId`, `createdAt`: BaseEntity 관리 필드로 변경 불가.
			
			### 수정 가능 필드
			- **섹션 1 (identity partial)**: `sourceUrl` — 출처 영상·링크 URL
			- **섹션 2 (performance)**: `performer`, `composer`, `title`, `album`, `instrument`, `style`, `tempo`, `key`, `rhythmFeel`, `timeSignature`
			- **섹션 3 (harmonic)**: `chords`, `chordsPerNote`, `harmonicContext`, `targetChord`
			  — 생략 시 변경된 `sheetData` 기반으로 **자동 재계산**됩니다.
			- **섹션 4 (sheetData)**: 악보 전체 교체 (measures 배열 필수)
			- **섹션 5 (features)**: `features` 객체
			  — 생략 시 변경된 `sheetData` 기반으로 **자동 재계산**됩니다.
			
			### 유효성 규칙
			요청 스키마는 생성(`POST /v1/licks`) API와 동일하며, `source`·`userId` 필드만 없습니다.
			
			### 에러
			- `404 LICK_001`: 해당 `publicId`의 릭이 존재하지 않을 경우 반환됩니다.
			"""
	)
	ApiResponse<LickResponse> update(UUID publicId, LickUpdateRequest request);

	@Operation(
		summary = "릭 삭제",
		description = """
			`publicId`에 해당하는 릭을 삭제합니다.
			
			삭제된 릭은 복구할 수 없으며, 저장된 모든 데이터(악보, 화성 맥락, 유사도 피처)가 함께 삭제됩니다.
			
			### 응답
			- 성공 시 HTTP `204 No Content`를 반환합니다.
			
			### 에러
			- `404 LICK_001`: 해당 `publicId`의 릭이 존재하지 않을 경우 반환됩니다.
			"""
	)
	ApiResponse<Void> delete(UUID publicId);

	@Operation(
		summary = "릭 영상 연결/갱신",
		description = """
			릭에 영상을 연결하거나 기존 영상을 갱신합니다. **(PUT — upsert 방식)**
			
			이미 영상이 연결되어 있으면 새 영상 정보로 교체되고, 없으면 새로 생성됩니다.
			
			### 요청 필드
			
			| 필드 | 타입 | 필수 | 설명 |
			|------|------|------|------|
			| `videoId` | string | ✅ | 영상 플랫폼 내부 ID (예: YouTube `dQw4w9WgXcQ`). 최대 255자. |
			| `startSec` | double | - | 재생 시작 시각 (초). null이면 처음부터 재생. |
			| `endSec` | double | - | 재생 종료 시각 (초). null이면 끝까지 재생. |
			| `url` | string | ✅ | 영상 전체 URL. 최대 512자. |
			
			### 에러
			- `404 LICK_001`: 해당 `publicId`의 릭이 존재하지 않을 경우 반환됩니다.
			""",
		requestBody = @RequestBody(
			content = @Content(
				mediaType = "application/json",
				examples = @ExampleObject(
					name = "YouTube 영상 연결 예시",
					value = """
						{
						  "videoId": "dQw4w9WgXcQ",
						  "startSec": 32.5,
						  "endSec": 45.0,
						  "url": "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
						}
						"""
				)
			)
		)
	)
	ApiResponse<LickResponse> updateVideo(UUID publicId, LickVideoRequest request);

	@Operation(
		summary = "릭 영상 연결 해제",
		description = """
			릭에 연결된 영상 정보를 삭제합니다.
			
			### 응답
			- 성공 시 HTTP `204 No Content`를 반환합니다.
			
			### 에러
			- `404 LICK_001`: 해당 `publicId`의 릭이 존재하지 않을 경우 반환됩니다.
			"""
	)
	ApiResponse<Void> deleteVideo(UUID publicId);

	@Operation(
		summary = "OMR로 릭 생성 (악보 파일 업로드)",
			description = """
				악보 파일(PNG/JPG/JPEG)을 업로드하여 MusicVision OMR 서버에서 인식한 뒤,
				반환된 `job_id`로 MusicXML과 chord assignments를 조회하고,
				안전하게 매핑 가능한 마디만 결합하여 Lick으로 저장합니다.
			
			### 요청 형식
			`multipart/form-data`로 전송해야 합니다.
			
			### 필수 파트
			| 파라미터 | 타입 | 설명 |
			|---------|------|------|
			| `file` | file | 악보 파일. **PNG · JPG · JPEG** 만 허용. |
			
			### 선택 파라미터 (form field)
			모든 필드가 선택(optional)이며, 미입력 시 MusicXML에서 추출된 값이 사용됩니다.
			
			| 파라미터 | 타입 | 설명 |
			|---------|------|------|
			| `title` | string | 곡 제목 (미입력 시 MusicXML work-title 사용) |
			| `performer` | string | 연주자 이름 |
			| `composer` | string | 작곡자 이름 (미입력 시 MusicXML creator[type=composer] 사용) |
			| `album` | string | 앨범명 |
			| `source` | string | 출처: `user` · `weimar` · `curated` (기본: `user`) |
			| `instrument` | string | 악기 코드: `as` · `ts` · `tp` · `p` · `g` 등 |
			| `style` | string | 재즈 스타일: `SWING` · `BEBOP` · `HARDBOP` 등 |
			| `tempo` | integer | 템포 BPM (미입력 시 MusicXML 값 사용) |
			| `key` | string | 조성 (미입력 시 MusicXML fifths 값 사용) |
			| `rhythmFeel` | string | 리듬감: `SWING` · `STRAIGHT` · `BOSSA` · `LATIN` |
			| `userId` | string (UUID) | 소유자 ID |
			
			### 처리 흐름
				1. 파일 확장자 검증 (`png`, `jpg`, `jpeg`)
			2. MusicVision `POST /omr/process`로 파일 전송
			3. 응답의 `job_id`로 `/omr/jobs/{job_id}/musicxml` 조회
			4. `/omr/jobs/{job_id}/chord-assignments` 조회
				5. `measure_alignment.status`가 `aligned`면 전체 결합, `partial`이면 `musicxml_measure_number`가 있는 마디만 결합, `mismatch`면 자동 결합 생략
			6. MusicXML 파싱 (조성·박자표·마디·음표 추출)
			7. 화성 맥락 및 유사도 피처 자동 계산
			8. DB 저장 후 릭 응답 반환
			
			### 에러
			- `400 OMR_004`: 지원하지 않는 파일 형식
			- `400 OMR_005`: 빈 파일
			- `503 OMR_001`: OMR 서버 미설정
			- `502 OMR_002`: OMR 서버 인식 실패
			- `422 OMR_003`: MusicXML 파싱 실패
			- `422 OMR_006`: MusicXML과 chord assignments의 마디 정렬 불일치
			
			### 태깅
			- OMR로 생성된 릭은 응답/저장 데이터의 `isOMR = true`로 태깅됩니다.
			"""
	)
	ApiResponse<LickResponse> createFromOmr(MultipartFile file, LickOmrRequest metadata);
}
