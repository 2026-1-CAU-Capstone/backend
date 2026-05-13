package com.jazzify.backend.domain.solo.util;

import com.jazzify.backend.domain.solo.entity.SoloVideo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jazzify.backend.domain.solo.dto.request.MeasureRequest;
import com.jazzify.backend.domain.solo.dto.request.NoteInfoRequest;
import com.jazzify.backend.domain.solo.dto.request.SheetDataRequest;
import com.jazzify.backend.domain.solo.dto.response.SoloResponse;
import com.jazzify.backend.domain.solo.dto.response.SoloVideoResponse;
import com.jazzify.backend.domain.solo.dto.response.MeasureResponse;
import com.jazzify.backend.domain.solo.dto.response.NoteInfoResponse;
import com.jazzify.backend.domain.solo.dto.response.SheetDataResponse;
import com.jazzify.backend.domain.solo.entity.Solo;
import com.jazzify.backend.domain.solo.entity.SoloMeasure;
import com.jazzify.backend.domain.solo.entity.SoloNote;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SoloMapper {

	private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
	private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
	private static final TypeReference<List<Integer>> INT_LIST = new TypeReference<>() {};
	private static final TypeReference<Map<String, String>> STRING_MAP = new TypeReference<>() {};

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
			Objects.requireNonNull(solo.getCreatedAt()),
			Objects.requireNonNull(solo.getUpdatedAt()),
			solo.getPerformer(),
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
	 * Solo 엔티티의 measures/notes 컬렉션을 SheetDataResponse로 변환한다.
	 * title, composer, key, timeSignature, tempo는 Solo 자신의 메타데이터 필드에서 채운다.
	 */
	public static SheetDataResponse toSheetDataResponse(Solo solo) {
		List<MeasureResponse> measures = solo.getMeasures().stream()
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
			solo.getPerformer(),
			solo.getMusicalKey(),
			solo.getTimeSignature(),
			solo.getTempo(),
			measures
		);
	}

	/**
	 * SheetDataRequest의 마디/음표 목록을 JPA 엔티티 계층으로 변환한다.
	 *
	 * @param solo    부모 Solo (이미 persist된 managed 엔티티여야 함)
	 * @param request 요청 sheetData
	 * @return 저장 준비된 SoloMeasure 목록 (각 마디에 SoloNote가 연결됨)
	 */
	public static List<SoloMeasure> toMeasureEntities(Solo solo, SheetDataRequest request) {
		List<MeasureRequest> measureRequests = request.measures();
		List<SoloMeasure> result = new ArrayList<>(measureRequests.size());

		for (int mi = 0; mi < measureRequests.size(); mi++) {
			MeasureRequest mr = measureRequests.get(mi);

			SoloMeasure measure = SoloMeasure.builder()
				.solo(solo)
				.measureIndex(mi)
				.chord(mr.chord())
				.build();

			List<NoteInfoRequest> noteRequests = mr.notes();
			for (int ni = 0; ni < noteRequests.size(); ni++) {
				NoteInfoRequest nr = noteRequests.get(ni);

				SoloNote note = SoloNote.builder()
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
