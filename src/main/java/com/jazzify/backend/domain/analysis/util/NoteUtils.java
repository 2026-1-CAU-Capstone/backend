package com.jazzify.backend.domain.analysis.util;

import java.util.Map;

import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.shared.exception.code.AnalysisErrorCode;

/**
 * 음악 이론 음표/키 유틸리티.
 * 음이름 ↔ 피치클래스(0~11) 변환, 키 문자열 파싱 등 기초 연산을 담당한다.
 * Python text_parser.py 유틸리티 함수에서 포팅됨.
 */
@NullMarked
public final class NoteUtils {

	private NoteUtils() {
	}

	/** 알파벳 음이름 → 피치클래스 매핑 (C=0, D=2, E=4, F=5, G=7, A=9, B=11) */
	private static final Map<Character, Integer> NOTE_TO_PC = Map.of(
		'C', 0, 'D', 2, 'E', 4, 'F', 5, 'G', 7, 'A', 9, 'B', 11
	);

	/** 피치클래스(0~11) → 표준 음이름 배열 (플랫 우선 표기) */
	private static final String[] PC_TO_NOTE = {
		"C", "Db", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"
	};

	/**
	 * 음이름 문자열을 피치클래스(0~11)로 변환한다.
	 * 예: "C"→0, "Db"→1, "F#"→6. 파싱 실패 시 -1 반환.
	 *
	 * 알고리즘: 첫 글자로 기본 피치클래스를 구한 뒤, 이후 #/b 기호에 따라 ±1 보정
	 */
	public static int parseNoteName(String name) {
		if (name == null || name.isEmpty()) {
			return -1;
		}
		// 첫 글자(알파벳)로 기본 피치클래스 조회
		char base = Character.toUpperCase(name.charAt(0));
		Integer pc = NOTE_TO_PC.get(base);
		if (pc == null) {
			return -1;
		}
		// 이후 문자에서 #(샵)은 +1, b(플랫)은 -1 보정
		int val = pc;
		for (int i = 1; i < name.length(); i++) {
			char ch = name.charAt(i);
			if (ch == '#' || ch == '\u266F') {
				val = mod12(val + 1);
			} else if (ch == 'b' || ch == '\u266D') {
				val = mod12(val - 1);
			} else {
				break;
			}
		}
		return val;
	}

	/**
	 * 피치클래스(0~11)를 표준 음이름 문자열로 변환한다.
	 * 예: 0→"C", 1→"Db", 6→"F#"
	 */
	public static String pcToNoteName(int pc) {
		return PC_TO_NOTE[mod12(pc)];
	}

	/**
	 * 키 문자열("C", "Bb", "F#m" 등)을 파싱하여 KeyInfo(근음 피치클래스, 장/단조)를 반환한다.
	 *
	 * 알고리즘: 끝에 "m" 또는 "min"이 있으면 단조, 그 외는 장조.
	 * 접미사를 제거한 나머지 문자열을 parseNoteName으로 근음 피치클래스를 구한다.
	 */
	public static KeyInfo parseKey(String keyStr) {
		keyStr = keyStr.strip();
		// 접미사로 장/단조 판별 ("m" → minor, "min" → minor, 그 외 → major)
		String mode = "major";
		if (keyStr.endsWith("m") && !keyStr.endsWith("maj")) {
			mode = "minor";
			keyStr = keyStr.substring(0, keyStr.length() - 1);
		} else if (keyStr.endsWith("min")) {
			mode = "minor";
			keyStr = keyStr.substring(0, keyStr.length() - 3);
		}
		// 접미사 제거 후 근음 파싱
		int pc = parseNoteName(keyStr);
		if (pc < 0) {
			throw AnalysisErrorCode.INVALID_KEY.toException(keyStr);
		}
		return new KeyInfo(pc, mode);
	}

	/** 두 피치클래스 사이의 상행 반음 간격을 구한다 (mod 12). 예: interval(7, 0) = 5 */
	public static int interval(int from, int to) {
		return mod12(to - from);
	}

	/** 어떤 정수든 0~11 범위로 정규화하는 모듈로 12 연산. 음수도 올바르게 처리한다. */
	public static int mod12(int v) {
		return ((v % 12) + 12) % 12;
	}

	/** 키 정보를 담는 레코드 (근음 피치클래스 + 장/단조 모드) */
	public record KeyInfo(int root, String mode) {

		public boolean isMajor() {
			return "major".equals(mode);
		}

		public boolean isMinor() {
			return "minor".equals(mode);
		}
	}
}

