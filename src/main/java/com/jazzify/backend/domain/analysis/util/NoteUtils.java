package com.jazzify.backend.domain.analysis.util;

import java.util.Map;

import org.jspecify.annotations.NullMarked;

/**
 * Music‑theory note/key utilities.
 * Ported from Python text_parser.py utility functions.
 */
@NullMarked
public final class NoteUtils {

	private NoteUtils() {
	}

	private static final Map<Character, Integer> NOTE_TO_PC = Map.of(
		'C', 0, 'D', 2, 'E', 4, 'F', 5, 'G', 7, 'A', 9, 'B', 11
	);

	private static final String[] PC_TO_NOTE = {
		"C", "Db", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"
	};

	/** Parse note name (e.g. "C", "Db", "F#") → pitch class 0‑11. Returns -1 on failure. */
	public static int parseNoteName(String name) {
		if (name == null || name.isEmpty()) {
			return -1;
		}
		char base = Character.toUpperCase(name.charAt(0));
		Integer pc = NOTE_TO_PC.get(base);
		if (pc == null) {
			return -1;
		}
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

	/** Pitch class → canonical note name. */
	public static String pcToNoteName(int pc) {
		return PC_TO_NOTE[mod12(pc)];
	}

	/**
	 * Parse key string like "C", "Bb", "F#m" → KeyInfo(rootPc, mode).
	 */
	public static KeyInfo parseKey(String keyStr) {
		keyStr = keyStr.strip();
		String mode = "major";
		if (keyStr.endsWith("m") && !keyStr.endsWith("maj")) {
			mode = "minor";
			keyStr = keyStr.substring(0, keyStr.length() - 1);
		} else if (keyStr.endsWith("min")) {
			mode = "minor";
			keyStr = keyStr.substring(0, keyStr.length() - 3);
		}
		int pc = parseNoteName(keyStr);
		if (pc < 0) {
			throw new IllegalArgumentException("Cannot parse key: " + keyStr);
		}
		return new KeyInfo(pc, mode);
	}

	/** Interval between two pitch classes (ascending, mod 12). */
	public static int interval(int from, int to) {
		return mod12(to - from);
	}

	public static int mod12(int v) {
		return ((v % 12) + 12) % 12;
	}

	public record KeyInfo(int root, String mode) {

		public boolean isMajor() {
			return "major".equals(mode);
		}

		public boolean isMinor() {
			return "minor".equals(mode);
		}
	}
}

