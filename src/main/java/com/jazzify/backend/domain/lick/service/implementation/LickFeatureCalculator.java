package com.jazzify.backend.domain.lick.service.implementation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.jazzify.backend.domain.lick.dto.request.MeasureRequest;
import com.jazzify.backend.domain.lick.dto.request.NoteInfoRequest;
import com.jazzify.backend.domain.lick.dto.request.SheetDataRequest;
import com.jazzify.backend.domain.lick.dto.request.SimilarityFeaturesRequest;
import com.jazzify.backend.domain.lick.entity.HarmonicContext;
import com.jazzify.backend.domain.lick.model.LickFeatures;
import com.jazzify.backend.domain.lick.model.LickHarmonicData;

/**
 * 섹션 3(화성 맥락)과 섹션 5(유사도 피처)를 계산하는 컴포넌트.
 * 요청에 값이 제공된 경우 그대로 사용하고, 없으면 sheetData로부터 자동 계산한다.
 */
@NullMarked
@Component
public class LickFeatureCalculator {

	private static final Map<Character, Integer> NOTE_OFFSET = Map.of(
		'c', 0, 'd', 2, 'e', 4, 'f', 5, 'g', 7, 'a', 9, 'b', 11
	);

	// ─── 섹션 3: 화성 맥락 ─────────────────────────────────────────────

	public LickHarmonicData computeHarmonicData(
		SheetDataRequest sheetData,
		@Nullable List<String> overrideChords,
		@Nullable List<String> overrideChordsPerNote,
		@Nullable HarmonicContext overrideContext,
		@Nullable String overrideTargetChord
	) {
		List<String> chords = overrideChords != null
			? overrideChords
			: extractChords(sheetData);

		List<String> chordsPerNote = overrideChordsPerNote != null
			? overrideChordsPerNote
			: extractChordsPerNote(sheetData);

		HarmonicContext harmonicContext = overrideContext != null
			? overrideContext
			: detectHarmonicContext(chords);

		String targetChord = overrideTargetChord != null
			? overrideTargetChord
			: (chords.isEmpty() ? null : chords.get(chords.size() - 1));

		return new LickHarmonicData(chords, chordsPerNote, harmonicContext, targetChord);
	}

	// ─── 섹션 5: 유사도 피처 ───────────────────────────────────────────

	public LickFeatures computeFeatures(
		SheetDataRequest sheetData,
		@Nullable SimilarityFeaturesRequest overrides
	) {
		List<NoteInfoRequest> notes = extractMelodicNotes(sheetData);

		if (notes.isEmpty()) {
			return overrides != null ? fromOverrides(overrides) : emptyFeatures();
		}

		List<Integer> pitches = (overrides != null && overrides.pitches() != null)
			? overrides.pitches()
			: notes.stream()
				.map(n -> noteToMidi(n.keys().get(0), n.accidentals()))
				.toList();

		List<Integer> intervals = (overrides != null && overrides.intervals() != null)
			? overrides.intervals()
			: computeIntervals(pitches);

		List<Integer> parsons = (overrides != null && overrides.parsons() != null)
			? overrides.parsons()
			: computeParsons(intervals);

		List<Integer> fuzzyIntervals = (overrides != null && overrides.fuzzyIntervals() != null)
			? overrides.fuzzyIntervals()
			: computeFuzzyIntervals(intervals);

		List<Integer> durationClasses = (overrides != null && overrides.durationClasses() != null)
			? overrides.durationClasses()
			: notes.stream().map(n -> toDurationClass(n.duration())).toList();

		int nEvents = (overrides != null && overrides.nEvents() != null)
			? overrides.nEvents()
			: notes.size();

		int pitchMin = pitches.stream().mapToInt(Integer::intValue).min().orElse(0);
		int pitchMax = pitches.stream().mapToInt(Integer::intValue).max().orElse(0);
		int pitchRange = pitchMax - pitchMin;
		double pitchMean = pitches.stream().mapToInt(Integer::intValue).average().orElse(0.0);
		int startPitch = pitches.get(0);
		int endPitch = pitches.get(pitches.size() - 1);

		return new LickFeatures(
			nEvents, pitches, intervals, parsons, fuzzyIntervals, durationClasses,
			pitchMin, pitchMax, pitchRange, pitchMean, startPitch, endPitch
		);
	}

	// ─── 내부: 음표 추출 ────────────────────────────────────────────────

	/**
	 * 쉼표를 제외하고, 타이로 이어진 음은 첫 번째 음만 남긴다.
	 */
	private List<NoteInfoRequest> extractMelodicNotes(SheetDataRequest sheetData) {
		List<NoteInfoRequest> result = new ArrayList<>();
		boolean skipNext = false;
		for (MeasureRequest measure : sheetData.measures()) {
			for (NoteInfoRequest note : measure.notes()) {
				// 쉼표 제외
				if (note.duration().endsWith("r")) {
					continue;
				}
				// 타이로 이어진 음(이전 음의 연장) 제외 — 체인 타이도 처리
				if (skipNext) {
					skipNext = Boolean.TRUE.equals(note.tie());
					continue;
				}
				result.add(note);
				skipNext = Boolean.TRUE.equals(note.tie());
			}
		}
		return result;
	}

	// ─── 내부: MIDI 변환 ────────────────────────────────────────────────

	/**
	 * keys 포맷 "d/5" → MIDI 정수. accidentals["0"] 적용.
	 */
	private int noteToMidi(String key, @Nullable Map<String, String> accidentals) {
		String[] parts = key.split("/");
		char noteName = parts[0].charAt(0);
		int octave = Integer.parseInt(parts[1]);
		int semitone = NOTE_OFFSET.getOrDefault(noteName, 0);
		int midi = (octave + 1) * 12 + semitone;
		if (accidentals != null) {
			String acc = accidentals.get("0");
			if ("#".equals(acc)) {
				midi += 1;
			} else if ("b".equals(acc)) {
				midi -= 1;
			}
			// "n" (제자리표) = 변화 없음
		}
		return midi;
	}

	// ─── 내부: 피처 계산 ────────────────────────────────────────────────

	private List<Integer> computeIntervals(List<Integer> pitches) {
		List<Integer> result = new ArrayList<>();
		for (int i = 1; i < pitches.size(); i++) {
			result.add(pitches.get(i) - pitches.get(i - 1));
		}
		return result;
	}

	private List<Integer> computeParsons(List<Integer> intervals) {
		return intervals.stream()
			.map(i -> {
				if (i > 0) return 1;
				if (i < 0) return -1;
				return 0;
			})
			.toList();
	}

	/**
	 * 음정 크기를 3단계 범주로 압축.
	 * |interval| 1-2 → ±1(반음), 3-5 → ±2(도약), 6+ → ±3(큰도약)
	 */
	private List<Integer> computeFuzzyIntervals(List<Integer> intervals) {
		return intervals.stream()
			.map(i -> {
				int abs = Math.abs(i);
				int sign = i > 0 ? 1 : (i < 0 ? -1 : 0);
				if (abs <= 2) return sign * 1;
				if (abs <= 5) return sign * 2;
				return sign * 3;
			})
			.toList();
	}

	/**
	 * duration 문자열 → 음길이 범주 (2=2박↑, 1=1박, 0=8분, -1=16분)
	 */
	private int toDurationClass(String duration) {
		String base = duration.replace("r", "");
		return switch (base) {
			case "w", "h" -> 2;
			case "q" -> 1;
			case "16" -> -1;
			default -> 0; // "8" 및 그 외
		};
	}

	// ─── 내부: 화성 맥락 추출 ──────────────────────────────────────────

	/**
	 * sheetData의 마디 코드 표기에서 중복 없는 순서 보존 코드 목록 추출.
	 * "D-7  G7" 처럼 두 칸 이상 공백으로 구분된 경우 분리한다.
	 */
	private List<String> extractChords(SheetDataRequest sheetData) {
		List<String> result = new ArrayList<>();
		for (MeasureRequest measure : sheetData.measures()) {
			String chord = measure.chord();
			if (chord == null || chord.isBlank()) {
				continue;
			}
			for (String part : chord.trim().split("\\s{2,}")) {
				String c = part.trim();
				if (!c.isEmpty() && !result.contains(c)) {
					result.add(c);
				}
			}
		}
		return result;
	}

	/**
	 * 각 선율 음표(쉼표 제외)에 해당 코드를 할당한다.
	 * 한 마디에 두 코드가 있을 경우(두 칸 이상 공백) 음표를 절반씩 나눈다.
	 */
	private List<String> extractChordsPerNote(SheetDataRequest sheetData) {
		List<String> result = new ArrayList<>();
		for (MeasureRequest measure : sheetData.measures()) {
			List<NoteInfoRequest> melodicNotes = measure.notes().stream()
				.filter(n -> !n.duration().endsWith("r"))
				.toList();
			if (melodicNotes.isEmpty()) {
				continue;
			}
			String chord = measure.chord();
			if (chord == null || chord.isBlank()) {
				melodicNotes.forEach(n -> result.add(""));
				continue;
			}
			String[] chordParts = chord.trim().split("\\s{2,}");
			if (chordParts.length == 1) {
				melodicNotes.forEach(n -> result.add(chordParts[0].trim()));
			} else {
				int half = (melodicNotes.size() + 1) / 2;
				for (int i = 0; i < melodicNotes.size(); i++) {
					result.add(i < half ? chordParts[0].trim() : chordParts[1].trim());
				}
			}
		}
		return result;
	}

	/**
	 * 코드 목록에서 화성 진행 패턴 감지.
	 * minor-ii-V → ii-V-I → blues 순으로 우선 확인 후 OTHER 반환.
	 */
	private HarmonicContext detectHarmonicContext(List<String> chords) {
		// minor-ii-V: 반감음7 → 속7
		for (int i = 0; i < chords.size() - 1; i++) {
			if (isHalfDim(chords.get(i)) && isDom7(chords.get(i + 1))) {
				return HarmonicContext.MINOR_II_V;
			}
		}
		// ii-V-I: 단7 → 속7 → 장7
		for (int i = 0; i < chords.size() - 2; i++) {
			if (isMin7(chords.get(i)) && isDom7(chords.get(i + 1)) && isMaj7(chords.get(i + 2))) {
				return HarmonicContext.II_V_I;
			}
		}
		// blues: 속7 코드가 2개 이상
		long dom7Count = chords.stream().filter(this::isDom7).count();
		if (dom7Count >= 2) {
			return HarmonicContext.BLUES;
		}
		return HarmonicContext.OTHER;
	}

	private boolean isHalfDim(String chord) {
		return chord.contains("-7b5") || chord.contains("m7b5") || chord.contains("ø");
	}

	private boolean isDom7(String chord) {
		return chord.endsWith("7")
			&& !chord.endsWith("Maj7")
			&& !chord.endsWith("maj7");
	}

	private boolean isMin7(String chord) {
		return (chord.endsWith("-7") || chord.endsWith("m7")) && !isHalfDim(chord);
	}

	private boolean isMaj7(String chord) {
		return chord.endsWith("Maj7")
			|| chord.endsWith("maj7")
			|| chord.endsWith("△7");
	}

	// ─── 헬퍼 ──────────────────────────────────────────────────────────

	private LickFeatures fromOverrides(SimilarityFeaturesRequest o) {
		return new LickFeatures(
			o.nEvents(), o.pitches(), o.intervals(), o.parsons(),
			o.fuzzyIntervals(), o.durationClasses(),
			o.pitchMin(), o.pitchMax(), o.pitchRange(), o.pitchMean(),
			o.startPitch(), o.endPitch()
		);
	}

	private LickFeatures emptyFeatures() {
		return new LickFeatures(null, null, null, null, null, null,
			null, null, null, null, null, null);
	}
}

