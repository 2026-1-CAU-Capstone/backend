package com.jazzify.backend.shared.musicxml;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * MusicXML 전체 파싱 결과를 담는 공유 모델.
 * <p>
 * TypeScript {@code NoteSheetData} 인터페이스와 동일한 구조를 Java로 표현한다.
 *
 * @param title         곡 제목
 * @param composer      작곡자 (없으면 null)
 * @param key           조성 (예: {@code "C"}, {@code "Bb"}, {@code "F#"})
 * @param timeSignature 박자표 (예: {@code "4/4"})
 * @param tempo         템포 BPM (없으면 null)
 * @param measures      마디 목록
 */
@NullMarked
public record ParsedSheetData(
	String title,
	@Nullable String composer,
	String key,
	String timeSignature,
	@Nullable Integer tempo,
	List<ParsedMeasure> measures
) {
}

