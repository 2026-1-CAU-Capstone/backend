package com.jazzify.backend.domain.solo.util;

import com.jazzify.backend.domain.solo.entity.SoloVideo;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jazzify.backend.domain.solo.dto.request.SoloCreateRequest;
import com.jazzify.backend.domain.solo.dto.request.MeasureRequest;
import com.jazzify.backend.domain.solo.dto.request.SheetDataRequest;
import com.jazzify.backend.domain.solo.dto.request.SoloUpdateRequest;
import com.jazzify.backend.domain.solo.dto.response.SoloResponse;
import com.jazzify.backend.domain.solo.dto.response.SoloVideoResponse;
import com.jazzify.backend.domain.solo.dto.response.MeasureResponse;
import com.jazzify.backend.domain.solo.dto.response.NoteInfoResponse;
import com.jazzify.backend.domain.solo.dto.response.SheetDataResponse;
import com.jazzify.backend.domain.solo.entity.Solo;
import com.jazzify.backend.domain.solo.entity.SoloMeasure;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SoloMapper {

	private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
	private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
	private static final TypeReference<List<Integer>> INT_LIST = new TypeReference<>() {};
	private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {};
	private static final TypeReference<SheetDataResponse> SHEET_DATA_RESPONSE = new TypeReference<>() {};

	// ─── Entity → Response ─────────────────────────────────────────────

	public static SoloResponse toResponse(Solo solo) {
		SoloVideo v = solo.getVideo();
		SoloVideoResponse videoResponse = v != null
			? new SoloVideoResponse(v.getVideoId(), v.getStartSec(), v.getEndSec(), v.getUrl())
			: null;

		return new SoloResponse(
			Objects.requireNonNull(solo.getPublicId()),
			solo.getSource(),
			solo.getUserId(),
			solo.isOMR(),
			Objects.requireNonNull(solo.getCreatedAt()),
			Objects.requireNonNull(solo.getUpdatedAt()),
			solo.getPerformer(),
			solo.getComposer(),
			solo.getTitle(),
			solo.getAlbum(),
			solo.getInstrument(),
			solo.getStyle(),
			solo.getTempo(),
			solo.getMusicalKey(),
			solo.getRhythmFeel(),
			solo.getTimeSignature(),
			parseStringList(solo.getChords()),
			parseStringList(solo.getChordsPerNote()),
			solo.getHarmonicContext(),
			solo.getTargetChord(),
			toSheetDataResponse(solo),
			solo.getNEvents(),
			parseIntList(solo.getPitches()),
			parseIntList(solo.getIntervals()),
			parseIntList(solo.getParsons()),
			parseIntList(solo.getFuzzyIntervals()),
			parseIntList(solo.getDurationClasses()),
			solo.getPitchMin(),
			solo.getPitchMax(),
			solo.getPitchRange(),
			solo.getPitchMean(),
			solo.getStartPitch(),
			solo.getEndPitch(),
			videoResponse
		);
	}

	/**
	 * 반정규화된 sheetData JSON을 우선 사용하고,
	 * 아직 마이그레이션되지 않은 레거시 데이터만 기존 measures/notes 컬렉션에서 fallback 변환한다.
	 */
	public static SheetDataResponse toSheetDataResponse(Solo solo) {
		SheetDataResponse parsed = parseSheetData(solo.getSheetDataJson());
		if (parsed != null) {
			return parsed;
		}
		return toLegacySheetDataResponse(solo, solo.getMeasures());
	}

	public static SheetDataResponse toSheetDataResponse(SoloCreateRequest request) {
		return toSheetDataResponse(request.sheetData());
	}

	public static SheetDataResponse toSheetDataResponse(SoloUpdateRequest request) {
		return toSheetDataResponse(request.sheetData());
	}

	public static SheetDataResponse toSheetDataResponse(SheetDataRequest request) {
		return toSheetDataResponse(
			request.title(),
			request.key(),
			request.timeSignature(),
			request.tempo(),
			request.measures()
		);
	}

	public static String serializeSheetData(SheetDataResponse sheetData) {
		try {
			return MAPPER.writeValueAsString(sheetData);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("sheetData 직렬화 실패", e);
		}
	}

	public static @Nullable SheetDataResponse parseSheetData(@Nullable String json) {
		if (json == null || json.isBlank()) {
			return null;
		}
		try {
			return MAPPER.readValue(json, SHEET_DATA_RESPONSE);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	public static SheetDataResponse toLegacySheetDataResponse(Solo solo, List<SoloMeasure> legacyMeasures) {
		List<MeasureResponse> measures = legacyMeasures.stream()
			.map(m -> new MeasureResponse(
				m.getChord(),
				m.getNotes().stream()
					.map(n -> new NoteInfoResponse(
						Objects.requireNonNull(parseStringList(n.getKeys())),
						n.getDuration(),
						parseStringMap(n.getAccidentals()),
						n.getTuplet(),
						n.isDotted() ? Boolean.TRUE : null,
						n.isTie() ? Boolean.TRUE : null,
						n.isGliss() ? Boolean.TRUE : null,
						n.isBeamBreak() ? Boolean.TRUE : null
					))
					.toList()
			))
			.toList();

		return new SheetDataResponse(
			solo.getTitle(),
			solo.getMusicalKey(),
			solo.getTimeSignature(),
			solo.getTempo(),
			measures
		);
	}

	private static SheetDataResponse toSheetDataResponse(
		@Nullable String title,
		@Nullable String key,
		@Nullable String timeSignature,
		@Nullable Integer tempo,
		List<MeasureRequest> measureRequests
	) {
		List<MeasureResponse> measures = measureRequests.stream()
			.map(m -> new MeasureResponse(
				m.chord(),
				m.notes().stream()
					.map(n -> new NoteInfoResponse(
						n.keys(),
						n.duration(),
						n.accidentals(),
						n.tuplet(),
						n.dotted(),
						n.tie(),
						n.gliss(),
						n.beamBreak()
					))
					.toList()
			))
			.toList();

		return new SheetDataResponse(title, key, timeSignature, tempo, measures);
	}

	// ─── Domain Model → JSON String ────────────────────────────────────

	public static @Nullable String serializeList(@Nullable List<?> list) {
		if (list == null) return null;
		try {
			return MAPPER.writeValueAsString(list);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("리스트 직렬화 실패", e);
		}
	}


	// ─── JSON → Java ────────────────────────────────────────────────────

	private static @Nullable List<String> parseStringList(@Nullable String json) {
		if (json == null) return null;
		try {
			return MAPPER.readValue(json, STRING_LIST);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	private static @Nullable List<Integer> parseIntList(@Nullable String json) {
		if (json == null) return null;
		try {
			return MAPPER.readValue(json, INT_LIST);
		} catch (JsonProcessingException e) {
			return null;
		}
	}

	private static @Nullable Map<String, String> parseStringMap(@Nullable String json) {
		if (json == null) return null;
		try {
			return MAPPER.readValue(json, STRING_MAP);
		} catch (JsonProcessingException e) {
			return null;
		}
	}
}
