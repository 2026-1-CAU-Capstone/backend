package com.jazzify.backend.domain.lick.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jazzify.backend.domain.lick.dto.request.MeasureRequest;
import com.jazzify.backend.domain.lick.dto.request.NoteInfoRequest;
import com.jazzify.backend.domain.lick.dto.request.SheetDataRequest;
import com.jazzify.backend.domain.lick.dto.response.LickResponse;
import com.jazzify.backend.domain.lick.dto.response.MeasureResponse;
import com.jazzify.backend.domain.lick.dto.response.NoteInfoResponse;
import com.jazzify.backend.domain.lick.dto.response.SheetDataResponse;
import com.jazzify.backend.domain.lick.entity.Lick;
import com.jazzify.backend.domain.lick.entity.LickMeasure;
import com.jazzify.backend.domain.lick.entity.LickNote;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LickMapper {

	private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
	private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
	private static final TypeReference<List<Integer>> INT_LIST = new TypeReference<>() {};
	private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {};

	// ─── Entity → Response ─────────────────────────────────────────────

	public static LickResponse toResponse(Lick lick) {
		return new LickResponse(
			Objects.requireNonNull(lick.getPublicId()),
			lick.getSource(),
			lick.getUserId(),
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
			lick.getEndPitch()
		);
	}

	/**
	 * Lick 엔티티의 measures/notes 컬렉션을 SheetDataResponse로 변환한다.
	 * title, composer, key, timeSignature, tempo는 Lick 자신의 메타데이터 필드에서 채운다.
	 */
	public static SheetDataResponse toSheetDataResponse(Lick lick) {
		List<MeasureResponse> measures = lick.getMeasures().stream()
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
			lick.getPerformer(),
			lick.getMusicalKey(),
			lick.getTimeSignature(),
			lick.getTempo(),
			measures
		);
	}

	/**
	 * SheetDataRequest의 마디/음표 목록을 JPA 엔티티 계층으로 변환한다.
	 *
	 * @param lick    부모 Lick (이미 persist된 managed 엔티티여야 함)
	 * @param request 요청 sheetData
	 * @return 저장 준비된 LickMeasure 목록 (각 마디에 LickNote가 연결됨)
	 */
	public static List<LickMeasure> toMeasureEntities(Lick lick, SheetDataRequest request) {
		List<MeasureRequest> measureRequests = request.measures();
		List<LickMeasure> result = new ArrayList<>(measureRequests.size());

		for (int mi = 0; mi < measureRequests.size(); mi++) {
			MeasureRequest mr = measureRequests.get(mi);

			LickMeasure measure = LickMeasure.builder()
				.lick(lick)
				.measureIndex(mi)
				.chord(mr.chord())
				.build();

			List<NoteInfoRequest> noteRequests = mr.notes();
			for (int ni = 0; ni < noteRequests.size(); ni++) {
				NoteInfoRequest nr = noteRequests.get(ni);

				LickNote note = LickNote.builder()
					.measure(measure)
					.noteIndex(ni)
					.keys(Objects.requireNonNull(serializeList(nr.keys())))
					.duration(nr.duration())
					.dotted(Boolean.TRUE.equals(nr.dotted()))
					.tuplet(nr.tuplet())
					.tie(Boolean.TRUE.equals(nr.tie()))
					.gliss(Boolean.TRUE.equals(nr.gliss()))
					.beamBreak(Boolean.TRUE.equals(nr.beamBreak()))
					.accidentals(serializeMap(nr.accidentals()))
					.build();

				measure.addNote(note);
			}

			result.add(measure);
		}

		return result;
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

	private static @Nullable String serializeMap(@Nullable Map<String, String> map) {
		if (map == null) return null;
		try {
			return MAPPER.writeValueAsString(map);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("맵 직렬화 실패", e);
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
