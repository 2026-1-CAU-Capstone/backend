package com.jazzify.backend.domain.chordinfo.util;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jazzify.backend.domain.chordinfo.entity.ChordAnalysis;
import com.jazzify.backend.domain.chordinfo.entity.ChordGroup;
import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.chordinfo.entity.ChordSection;
import com.jazzify.backend.domain.chordproject.dto.response.AnalysisResultResponse;
import com.jazzify.backend.domain.chordproject.dto.response.AnalysisResultResponse.AmbiguityStats;
import com.jazzify.backend.domain.chordproject.dto.response.AnalysisResultResponse.GroupMemberResponse;
import com.jazzify.backend.domain.chordproject.dto.response.AnalysisResultResponse.GroupResponse;
import com.jazzify.backend.domain.chordproject.dto.response.AnalysisResultResponse.SectionResponse;
import com.jazzify.backend.domain.chordproject.dto.response.ChordInfoResponse;
import com.jazzify.backend.domain.chordproject.dto.response.ChordInfoResponse.ChordAnalysisResponse;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChordInfoMapper {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	public static ChordInfoResponse toChordInfoResponse(ChordInfo info) {
		@Nullable ChordAnalysisResponse analysisResponse = null;
		ChordAnalysis analysis = info.getAnalysis();

		if (analysis != null) {
			analysisResponse = new ChordAnalysisResponse(
				analysis.getDegree(),
				analysis.getIsDiatonic(),
				analysis.getNormalizedQuality(),
				parseJsonList(analysis.getFunctions()),
				parseJsonObject(analysis.getSecondaryDominant()),
				analysis.getDiminishedFunction(),
				parseJsonObject(analysis.getChromaticApproach()),
				parseJsonObject(analysis.getDeceptiveResolution()),
				parseJsonObject(analysis.getPedalInfo()),
				parseJsonObject(analysis.getModalInterchange()),
				analysis.getModeSegment(),
				parseJsonObject(analysis.getTonicization()),
				analysis.getAmbiguityScore(),
				parseJsonList(analysis.getAmbiguityFlags())
			);
		}

		return new ChordInfoResponse(
			Objects.requireNonNull(info.getPublicId()),
			info.getChord(),
			info.getBar(),
			info.getBeat(),
			info.getDurationBeats(),
			analysisResponse
		);
	}

	public static GroupResponse toGroupResponse(ChordGroup group) {
		List<GroupMemberResponse> members = group.getMembers().stream()
			.map(m -> new GroupMemberResponse(
				Objects.requireNonNull(m.getChordInfo().getPublicId()),
				m.getRole()
			))
			.toList();

		return new GroupResponse(
			Objects.requireNonNull(group.getPublicId()),
			group.getGroupIndex(),
			group.getGroupType(),
			group.getVariant(),
			group.getTargetKey(),
			group.isDiatonicTarget(),
			group.getNotes(),
			members
		);
	}

	public static SectionResponse toSectionResponse(ChordSection section) {
		return new SectionResponse(
			Objects.requireNonNull(section.getPublicId()),
			section.getStartBar(),
			section.getEndBar(),
			section.getSectionKey(),
			section.getSectionType(),
			section.getMode(),
			parseJsonObject(section.getTonicizations())
		);
	}

	public static AnalysisResultResponse toAnalysisResultResponse(
		ChordProject project,
		List<ChordInfo> chordInfos,
		List<ChordGroup> groups,
		List<ChordSection> sections) {

		@Nullable AmbiguityStats stats = null;
		if (project.getTotalChords() != null) {
			stats = new AmbiguityStats(
				project.getTotalChords(),
				Objects.requireNonNull(project.getHighConfidenceCount()),
				Objects.requireNonNull(project.getAmbiguousCount()),
				Objects.requireNonNull(project.getMeanAmbiguityScore()),
				Objects.requireNonNull(project.getMaxAmbiguityScore())
			);
		}

		return new AnalysisResultResponse(
			Objects.requireNonNull(project.getPublicId()),
			project.getTitle(),
			project.getKeySignature().getAnalysisKey(),
			project.getTimeSignature(),
			project.getLastAnalyzedAt(),
			stats,
			chordInfos.stream().map(ChordInfoMapper::toChordInfoResponse).toList(),
			groups.stream().map(ChordInfoMapper::toGroupResponse).toList(),
			sections.stream().map(ChordInfoMapper::toSectionResponse).toList()
		);
	}

	// ── JSON 파싱 헬퍼 ──

	private static @Nullable Object parseJsonObject(@Nullable String json) {
		if (json == null || json.isBlank()) {
			return null;
		}
		try {
			return OBJECT_MAPPER.readValue(json, Object.class);
		} catch (JsonProcessingException e) {
			return json;
		}
	}

	private static @Nullable List<Object> parseJsonList(@Nullable String json) {
		if (json == null || json.isBlank()) {
			return null;
		}
		try {
			return OBJECT_MAPPER.readValue(json, new TypeReference<>() {});
		} catch (JsonProcessingException e) {
			return null;
		}
	}
}


