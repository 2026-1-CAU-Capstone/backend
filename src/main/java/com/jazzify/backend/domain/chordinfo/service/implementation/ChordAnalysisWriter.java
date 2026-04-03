package com.jazzify.backend.domain.chordinfo.service.implementation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.chordinfo.entity.ChordAnalysis;
import com.jazzify.backend.domain.chordinfo.entity.ChordGroup;
import com.jazzify.backend.domain.chordinfo.entity.ChordGroupMember;
import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.chordinfo.entity.ChordSection;
import com.jazzify.backend.domain.chordinfo.repository.ChordAnalysisRepository;
import com.jazzify.backend.domain.chordinfo.repository.ChordGroupRepository;
import com.jazzify.backend.domain.chordinfo.repository.ChordSectionRepository;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * 분석 결과를 DB 엔티티에 반영하는 Writer.
 * 분석 결과 → ChordAnalysis(1:1), groups → ChordGroup/ChordGroupMember, sections → ChordSection 저장.
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Transactional
public class ChordAnalysisWriter {

	private final ChordAnalysisRepository chordAnalysisRepository;
	private final ChordGroupRepository chordGroupRepository;
	private final ChordSectionRepository chordSectionRepository;
	private final ObjectMapper objectMapper;

	/**
	 * 기존 그룹/섹션 삭제 (재분석 시).
	 * ChordAnalysis는 ChordInfo cascade로 자동 관리되므로 별도 삭제 불필요.
	 */
	public void clearPreviousAnalysis(ChordProject project) {
		chordGroupRepository.deleteAllByChordProject(project);
		chordSectionRepository.deleteAllByChordProject(project);
	}

	/**
	 * 분석 결과 Map에서 각 ChordInfo에 대응하는 ChordAnalysis를 생성/갱신한다.
	 */
	@SuppressWarnings("unchecked")
	public void saveAnalysisResults(List<ChordInfo> chordInfos, List<Map<String, Object>> chordDicts) {
		for (int i = 0; i < chordInfos.size() && i < chordDicts.size(); i++) {
			ChordInfo info = chordInfos.get(i);
			Map<String, Object> dict = chordDicts.get(i);
			Map<String, Object> analysis = (Map<String, Object>) dict.get("analysis");

			if (analysis == null) {
				continue;
			}

			String degree = toStr(analysis.get("degree"));
			Boolean isDiatonic = analysis.get("is_diatonic") instanceof Boolean b ? b : null;
			String normalizedQuality = toStr(analysis.get("normalized_quality"));
			String functions = toJson(analysis.get("functions"));
			String secondaryDominant = toJson(analysis.get("secondary_dominant"));
			String diminishedFunction = toStr(analysis.get("diminished_function"));
			String chromaticApproach = toJson(analysis.get("chromatic_approach"));
			String deceptiveResolution = toJson(analysis.get("deceptive_resolution"));
			String pedalInfoJson = toJson(analysis.get("pedal_info"));
			String modalInterchange = toJson(analysis.get("modal_interchange"));
			String modeSegment = toStr(analysis.get("mode_segment"));
			String tonicization = toJson(analysis.get("tonicization"));
			double ambiguityScore = analysis.get("ambiguity_score") instanceof Number n ? n.doubleValue() : 0.0;
			String ambiguityFlags = toJson(analysis.get("ambiguity_flags"));

			ChordAnalysis existing = info.getAnalysis();
			if (existing != null) {
				existing.update(degree, isDiatonic, normalizedQuality, functions,
					secondaryDominant, diminishedFunction, chromaticApproach, deceptiveResolution,
					pedalInfoJson, modalInterchange, modeSegment, tonicization,
					ambiguityScore, ambiguityFlags);
			} else {
				ChordAnalysis newAnalysis = ChordAnalysis.builder()
					.degree(degree).isDiatonic(isDiatonic).normalizedQuality(normalizedQuality)
					.functions(functions).secondaryDominant(secondaryDominant)
					.diminishedFunction(diminishedFunction).chromaticApproach(chromaticApproach)
					.deceptiveResolution(deceptiveResolution).pedalInfo(pedalInfoJson)
					.modalInterchange(modalInterchange).modeSegment(modeSegment)
					.tonicization(tonicization).ambiguityScore(ambiguityScore)
					.ambiguityFlags(ambiguityFlags).chordInfo(info)
					.build();
				chordAnalysisRepository.save(newAnalysis);
				info.assignAnalysis(newAnalysis);
			}
		}
	}

	/**
	 * ii-V-I 등 그룹 정보를 저장한다.
	 */
	@SuppressWarnings("unchecked")
	public List<ChordGroup> saveGroups(ChordProject project, List<Map<String, Object>> groups,
		List<ChordInfo> chordInfos) {
		List<ChordGroup> savedGroups = new ArrayList<>();

		for (Map<String, Object> groupMap : groups) {
			ChordGroup group = ChordGroup.builder()
				.groupIndex(toInt(groupMap.get("group_id")))
				.groupType(String.valueOf(groupMap.get("group_type")))
				.variant(String.valueOf(groupMap.get("variant")))
				.targetKey(String.valueOf(groupMap.get("target_key")))
				.isDiatonicTarget(Boolean.TRUE.equals(groupMap.get("is_diatonic_target")))
				.notes(groupMap.get("notes") != null ? String.valueOf(groupMap.get("notes")) : "")
				.chordProject(project)
				.build();

			ChordGroup saved = chordGroupRepository.save(group);

			List<Map<String, Object>> members = (List<Map<String, Object>>) groupMap.get("members");
			if (members != null) {
				for (Map<String, Object> memberMap : members) {
					int bar = toInt(memberMap.get("bar"));
					double beat = toDouble(memberMap.get("beat"));
					String role = String.valueOf(memberMap.get("role"));

					ChordInfo matchedInfo = findChordInfoByBarAndBeat(chordInfos, bar, beat);
					if (matchedInfo != null) {
						ChordGroupMember member = ChordGroupMember.builder()
							.chordGroup(saved)
							.chordInfo(matchedInfo)
							.role(role)
							.build();
						saved.getMembers().add(member);
					}
				}
			}
			chordGroupRepository.save(saved);
			savedGroups.add(saved);
		}

		return savedGroups;
	}

	/**
	 * 섹션 경계 정보를 저장한다.
	 */
	public List<ChordSection> saveSections(ChordProject project, List<Map<String, Object>> sections) {
		List<ChordSection> savedSections = new ArrayList<>();

		for (Map<String, Object> sectionMap : sections) {
			String tonicizationsJson = toJson(sectionMap.get("tonicizations"));

			ChordSection section = ChordSection.builder()
				.startBar(toInt(sectionMap.get("start_bar")))
				.endBar(toInt(sectionMap.get("end_bar")))
				.sectionKey(String.valueOf(sectionMap.get("key")))
				.sectionType(String.valueOf(sectionMap.get("type")))
				.mode(sectionMap.get("mode") != null ? String.valueOf(sectionMap.get("mode")) : null)
				.tonicizations(tonicizationsJson)
				.chordProject(project)
				.build();
			savedSections.add(chordSectionRepository.save(section));
		}

		return savedSections;
	}

	/**
	 * 앰비규이티 통계를 ChordProject에 업데이트한다.
	 */
	public void updateProjectStats(ChordProject project, Map<String, Object> analysisResult) {
		@SuppressWarnings("unchecked")
		Map<String, Object> stats = (Map<String, Object>) analysisResult.get("ambiguity_stats");
		if (stats != null) {
			project.updateAnalysisStats(
				toInt(stats.get("total_chords")),
				toInt(stats.get("high_confidence_count")),
				toInt(stats.get("ambiguous_count")),
				toDouble(stats.get("mean_score")),
				toDouble(stats.get("max_score"))
			);
		}
	}

	// ── private helpers ──

	private @Nullable String toJson(@Nullable Object obj) {
		if (obj == null) {
			return null;
		}
		try {
			return objectMapper.writeValueAsString(obj);
		} catch (JacksonException e) {
			return null;
		}
	}

	private static @Nullable String toStr(@Nullable Object obj) {
		return obj != null ? String.valueOf(obj) : null;
	}

	private static @Nullable ChordInfo findChordInfoByBarAndBeat(List<ChordInfo> infos, int bar, double beat) {
		return infos.stream()
			.filter(ci -> ci.getBar() == bar && Math.abs(ci.getBeat() - beat) < 0.01)
			.findFirst()
			.orElse(null);
	}

	private static int toInt(@Nullable Object obj) {
		if (obj instanceof Number n) {
			return n.intValue();
		}
		return 0;
	}

	private static double toDouble(@Nullable Object obj) {
		if (obj instanceof Number n) {
			return n.doubleValue();
		}
		return 0.0;
	}
}



