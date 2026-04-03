package com.jazzify.backend.domain.chordproject.util;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.shared.exception.code.ChordProjectErrorCode;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * iRealPro 스타일 코드 진행 문자열을 {@link ChordInfo} 리스트로 변환하는 파서.
 * <p>
 * 규칙:
 * <ul>
 *   <li>{@code |} 로 마디를 구분한다.</li>
 *   <li>한 마디 안의 코드는 공백으로 구분하며, beatsPerBar를 코드 수로 나누어 박자를 분배한다.
 *       나머지가 생기면 앞쪽 코드부터 1박씩 추가 배분한다 (예: 3/4에 코드 2개 → 2박 + 1박).</li>
 *   <li>동일 마디 내에서 연속되는 동일 코드는 하나로 병합한다 (durationBeats 합산).</li>
 *   <li>{@code "N.C."} 또는 빈 문자열은 null chord로 처리한다.</li>
 * </ul>
 */
@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class IRealProChordParser {

	private static final String BAR_DELIMITER = "\\|";
	private static final String NO_CHORD = "N.C.";

	/**
	 * iRealPro 스타일 문자열을 파싱하여 ChordInfo 리스트를 반환한다.
	 *
	 * @param progression   코드 진행 문자열 (e.g., "Cmaj7 | Dm7 G7 | Cmaj7")
	 * @param timeSignature 박자표 (e.g., "4/4", "3/4")
	 * @param project       연관 ChordProject
	 * @return 병합된 ChordInfo 리스트
	 */
	public static List<ChordInfo> parse(String progression, String timeSignature, ChordProject project) {
		int beatsPerBar = extractBeatsPerBar(timeSignature);
		String[] bars = progression.split(BAR_DELIMITER);

		List<ChordInfo> rawEntries = new ArrayList<>();

		int barNumber = 1;
		for (String barContent : bars) {
			String trimmed = barContent.trim();
			if (trimmed.isEmpty()) {
				continue;
			}

			String[] tokens = trimmed.split("\\s+");
			int base = beatsPerBar / tokens.length;
			int remainder = beatsPerBar % tokens.length;

			double currentBeat = 1.0;
			for (int i = 0; i < tokens.length; i++) {
				String token = tokens[i].trim();
				String chord = normalizeChord(token);
				// 앞쪽 remainder개 코드에 1박씩 추가 배분
				int duration = base + (i < remainder ? 1 : 0);

				rawEntries.add(ChordInfo.builder()
					.chord(chord)
					.bar(barNumber)
					.beat(currentBeat)
					.durationBeats(duration)
					.chordProject(project)
					.build());

				currentBeat += duration;
			}
			barNumber++;
		}

		return mergeConsecutive(rawEntries);
	}

	/**
	 * 동일 마디 내에서 연속되는 동일 코드를 병합한다.
	 * 마디 경계를 넘는 병합은 수행하지 않는다.
	 */
	private static List<ChordInfo> mergeConsecutive(List<ChordInfo> entries) {
		if (entries.isEmpty()) {
			return entries;
		}

		List<ChordInfo> merged = new ArrayList<>();
		ChordInfo current = entries.getFirst();

		for (int i = 1; i < entries.size(); i++) {
			ChordInfo next = entries.get(i);

			if (current.getBar() == next.getBar() && isSameChord(current.getChord(), next.getChord())) {
				// 동일 마디 내 동일 코드 → durationBeats 합산, 앞쪽 위치 유지
				current = ChordInfo.builder()
					.chord(current.getChord())
					.bar(current.getBar())
					.beat(current.getBeat())
					.durationBeats(current.getDurationBeats() + next.getDurationBeats())
					.chordProject(current.getChordProject())
					.build();
			} else {
				merged.add(current);
				current = next;
			}
		}
		merged.add(current);

		return merged;
	}

	private static boolean isSameChord(@Nullable String a, @Nullable String b) {
		if (a == null && b == null) {
			return true;
		}
		if (a == null || b == null) {
			return false;
		}
		return a.equals(b);
	}

	/**
	 * "N.C." 등을 null로 정규화한다.
	 */
	private static @Nullable String normalizeChord(String token) {
		if (token.isBlank() || NO_CHORD.equalsIgnoreCase(token)) {
			return null;
		}
		return token;
	}

	/**
	 * 박자표 문자열에서 한 마디의 박수를 추출한다.
	 * "4/4" → 4, "3/4" → 3, "6/8" → 6
	 */
	private static int extractBeatsPerBar(String timeSignature) {
		try {
			String[] parts = timeSignature.split("/");
			return Integer.parseInt(parts[0].trim());
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			throw ChordProjectErrorCode.INVALID_TIME_SIGNATURE.toException();
		}
	}
}


