package com.jazzify.backend.domain.lick.util;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jazzify.backend.domain.lick.dto.response.LickResponse;
import com.jazzify.backend.domain.lick.dto.response.SheetDataResponse;
import com.jazzify.backend.domain.lick.entity.Lick;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LickMapper {

	private static final ObjectMapper MAPPER = new ObjectMapper().findAndRegisterModules();
	private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {};
	private static final TypeReference<List<Integer>> INT_LIST = new TypeReference<>() {};

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
			parseSheetData(lick.getSheetData()),
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

	// ─── Domain Model → JSON String ────────────────────────────────────

	public static String serializeSheetData(Object sheetDataRequest) {
		try {
			return MAPPER.writeValueAsString(sheetDataRequest);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("sheetData 직렬화 실패", e);
		}
	}

	public static @Nullable String serializeList(@Nullable List<?> list) {
		if (list == null) return null;
		try {
			return MAPPER.writeValueAsString(list);
		} catch (JsonProcessingException e) {
			throw new IllegalArgumentException("리스트 직렬화 실패", e);
		}
	}

	// ─── JSON → Java ────────────────────────────────────────────────────

	private static SheetDataResponse parseSheetData(String json) {
		try {
			return MAPPER.readValue(json, SheetDataResponse.class);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("sheetData 역직렬화 실패: " + e.getMessage(), e);
		}
	}

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
}
