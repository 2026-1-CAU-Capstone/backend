package com.jazzify.backend.domain.lick.util;

import com.jazzify.backend.domain.lick.entity.LickVideo;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jazzify.backend.domain.lick.dto.request.LickCreateRequest;
import com.jazzify.backend.domain.lick.dto.request.MeasureRequest;
import com.jazzify.backend.domain.lick.dto.request.LickUpdateRequest;
import com.jazzify.backend.domain.lick.dto.response.LickResponse;
import com.jazzify.backend.domain.lick.dto.response.LickVideoResponse;
import com.jazzify.backend.domain.lick.dto.response.MeasureResponse;
import com.jazzify.backend.domain.lick.dto.response.NoteInfoResponse;
import com.jazzify.backend.domain.lick.dto.response.SheetDataResponse;
import com.jazzify.backend.domain.lick.entity.Lick;
import com.jazzify.backend.domain.lick.entity.LickMeasure;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LickMapper {

	private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
	private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
	private static final TypeReference<List<Integer>> INT_LIST = new TypeReference<>() {};
	private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {};
	private static final TypeReference<SheetDataResponse> SHEET_DATA_RESPONSE = new TypeReference<>() {};

	// ─── Entity → Response ─────────────────────────────────────────────

	public static LickResponse toResponse(Lick lick) {
		LickVideo v = lick.getVideo();
		LickVideoResponse videoResponse = v != null
			? new LickVideoResponse(v.getVideoId(), v.getStartSec(), v.getEndSec(), v.getUrl())
			: null;

		return new LickResponse(
			Objects.requireNonNull(lick.getPublicId()),
			lick.getSource(),
			lick.getUserId(),
			lick.isOMR(),
			Objects.requireNonNull(lick.getCreatedAt()),
			Objects.requireNonNull(lick.getUpdatedAt()),
			lick.getPerformer(),
			lick.getTitle(),
			lick.getAlbum(),
			lick.getInstrument(),
			lick.getStyle(),
			lick.getTempo(),
			lick.getMusicalKey(),
			lick.getRhythmFeel(),
			lick.getTimeSignature(),
			parseStringList(lick.getChords()),
			parseStringList(lick.getChordsPerNote()),
			lick.getHarmonicContext(),
			lick.getTargetChord(),
			toSheetDataResponse(lick),
			lick.getNEvents(),
			parseIntList(lick.getPitches()),
			parseIntList(lick.getIntervals()),
			parseIntList(lick.getParsons()),
			parseIntList(lick.getFuzzyIntervals()),
			parseIntList(lick.getDurationClasses()),
			lick.getPitchMin(),
			lick.getPitchMax(),
			lick.getPitchRange(),
			lick.getPitchMean(),
			lick.getStartPitch(),
			lick.getEndPitch(),
			videoResponse
		);
	}

	/**
	 * 반정규화된 sheetData JSON을 우선 사용하고,
	 * 아직 마이그레이션되지 않은 레거시 데이터만 기존 measures/notes 컬렉션에서 fallback 변환한다.
	 */
	public static SheetDataResponse toSheetDataResponse(Lick lick) {
		SheetDataResponse parsed = parseSheetData(lick.getSheetDataJson());
		if (parsed != null) {
			return parsed;
		}
		return toLegacySheetDataResponse(lick, lick.getMeasures());
	}

	public static SheetDataResponse toSheetDataResponse(LickCreateRequest request) {
		return toSheetDataResponse(
			request.title(),
			request.sheetData().composer(),
			request.key(),
			request.timeSignature(),
			request.tempo(),
			request.sheetData().measures()
		);
	}

	public static SheetDataResponse toSheetDataResponse(LickUpdateRequest request) {
		return toSheetDataResponse(
			request.title(),
			request.sheetData().composer(),
			request.key(),
			request.timeSignature(),
			request.tempo(),
			request.sheetData().measures()
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

	public static SheetDataResponse toLegacySheetDataResponse(Lick lick, List<LickMeasure> legacyMeasures) {
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
			lick.getTitle(),
			lick.getComposer(),
			lick.getMusicalKey(),
			lick.getTimeSignature(),
			lick.getTempo(),
			measures
		);
	}

	private static SheetDataResponse toSheetDataResponse(
		@Nullable String title,
		@Nullable String composer,
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

		return new SheetDataResponse(title, composer, key, timeSignature, tempo, measures);
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
