package com.jazzify.backend.shared.musicxml;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * MusicXML 파싱 후 단일 마디 데이터를 담는 공유 모델.
 *
 * @param chord 마디 코드 심볼. 두 코드는 두 칸 공백 구분 (예: {@code "D-7  G7"}). 없으면 null.
 * @param notes 해당 마디의 음표/쉼표 목록
 */
@NullMarked
public record ParsedMeasure(
	@Nullable String chord,
	List<ParsedNote> notes
) {
}

