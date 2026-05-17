package com.jazzify.backend.domain.lick.dto.request;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * OMR API 요청의 메타데이터 부분 (multipart form-data).
 * <p>
 * 파일은 컨트롤러에서 {@code @RequestPart("file")}로 별도 수신한다.
 * 모든 필드가 선택(optional)이며, 미입력 시 MusicXML 파싱 결과로 채워진다.
 *
 * <ul>
 *   <li>{@code title}      – 미입력 시 MusicXML의 work-title 사용</li>
 *   <li>{@code tempo}      – 미입력 시 MusicXML의 sound[tempo] 사용</li>
 *   <li>{@code key}        – 미입력 시 MusicXML의 fifths 값으로 계산</li>
 *   <li>{@code timeSignature} – 미입력 시 MusicXML의 beats/beat-type 사용</li>
 * </ul>
 */
@NullMarked
public record LickOmrRequest(
	// ─── Performance Metadata ──────────────────────────────────────────
	/** 곡 제목. 미입력 시 MusicXML에서 추출. */
	@Nullable @Size(max = 255) String title,

	/** 연주자 이름. */
	@Nullable @Size(max = 255) String performer,

	/** 앨범명. */
	@Nullable @Size(max = 255) String album,

	/**
	 * 출처 코드 문자열. {@code "user"}, {@code "weimar"}, {@code "curated"}.
	 * 미입력 시 {@code "user"}로 설정됨.
	 */
	@Nullable String source,

	/**
	 * 악기 코드 문자열. {@code "as"}, {@code "ts"}, {@code "tp"}, {@code "p"} 등.
	 * 미입력 시 {@code "unknown"}으로 설정됨.
	 */
	@Nullable String instrument,

	/** 재즈 스타일 문자열. {@code "SWING"}, {@code "BEBOP"} 등. */
	@Nullable String style,

	/** 템포 BPM. 미입력 시 MusicXML에서 추출. */
	@Nullable @Min(1) @Max(500) Integer tempo,

	/** 조성. 미입력 시 MusicXML에서 추출. */
	@Nullable @Size(max = 20) String key,

	/** 리듬감 문자열. {@code "SWING"}, {@code "BOSSA"} 등. */
	@Nullable String rhythmFeel,

	/** 사용자 ID (문자열 UUID 형식). */
	@Nullable String userId
) {
}

