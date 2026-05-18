package com.jazzify.backend.shared.musicxml;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * MusicXML 파싱 후 개별 음표/쉼표 데이터를 담는 공유 모델.
 * <p>
 * VexFlow 렌더링과 호환되는 형식으로 저장된다.
 *
 * @param keys        음고 목록. 단음: {@code ["d/5"]}, 쉼표: {@code ["b/4"]}
 * @param duration    음가 코드. 음표: {@code "w","h","q","8","16"}, 쉼표: {@code "wr","hr","qr","8r"}
 * @param dotted      점음표 여부
 * @param accidentals 임시표 맵. 예: {@code {"0":"#"}}. 없으면 null.
 */
@NullMarked
public record ParsedNote(
	List<String> keys,
	String duration,
	boolean dotted,
	@Nullable Map<String, String> accidentals
) {
}

