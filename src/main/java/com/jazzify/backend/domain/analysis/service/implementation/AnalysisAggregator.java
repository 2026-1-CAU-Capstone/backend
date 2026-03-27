package com.jazzify.backend.domain.analysis.service.implementation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.util.NoteUtils;

/**
 * 분석 결과 집계기.
 * 파이프라인의 모든 분석 결과를 하나의 JSON 직렬화 가능한 Map으로 합친다.
 * Python aggregator.py에서 포팅됨.
 */
@NullMarked
@Component
public class AnalysisAggregator {

	private static final String ENGINE_VERSION = "0.1.0";

	/** 이 엔진이 수행하는 16가지 분석 항목 목록 */
	private static final List<String> COVERAGE = List.of(
		"diatonic_classification",
		"scale_degree_calculation",
		"T_SD_D_function_labeling",
		"chord_normalization",
		"ii-V-I_detection",
		"tritone_substitution_detection",
		"secondary_dominant_detection",
		"diminished_chord_classification",
		"chromatic_approach_detection",
		"deceptive_resolution_detection",
		"pedal_point_detection",
		"modal_interchange_detection",
		"mode_segment_detection",
		"tonicization_modulation_detection",
		"section_boundary_detection",
		"ambiguity_scoring"
	);

	/**
	 * 모든 분석 결과를 최종 출력 Map으로 집계한다.
	 *
	 * 출력 구조:
	 * - song: 곡 메타 정보 (title, key, time_signature)
	 * - chords: 코드별 위치 정보 + 상세 분석 결과
	 * - groups: ii-V-I 그룹 목록
	 * - sections: 섹션 경계 목록
	 * - ambiguity_stats: 모호성 통계 요약 (총 코드 수, 높은 확신도 비율, 모호한 코드 비율 등)
	 * - engine_version: 분석 엔진 버전
	 * - coverage: 수행된 분석 항목 목록
	 */
	public Map<String, Object> aggregate(String title, String key, String timeSignature,
		List<ParsedChord> chords,
		List<Map<String, Object>> groups,
		List<Map<String, Object>> sections) {

		// 각 ParsedChord를 JSON용 Map으로 변환
		List<Map<String, Object>> chordDicts = chords.stream()
			.map(this::chordToDict).toList();

		// 모호성 통계 계산
		List<Double> scores = chords.stream().map(ParsedChord::getAmbiguityScore).toList();
		int n = scores.size();
		long highConf = scores.stream().filter(s -> s <= 0.1).count();    // 모호성 ≤ 0.1 → 높은 확신도
		long ambiguous = scores.stream().filter(s -> s > 0.3).count();     // 모호성 > 0.3 → 모호
		double mean = n > 0 ? scores.stream().mapToDouble(Double::doubleValue).sum() / n : 0;
		double max = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0);

		// 최종 출력 Map 조립
		Map<String, Object> output = new LinkedHashMap<>();
		output.put("song", Map.of("title", title, "key", key, "time_signature", timeSignature));
		output.put("chords", chordDicts);
		output.put("groups", groups);
		output.put("sections", sections);
		output.put("ambiguity_stats", Map.of(
			"total_chords", n,
			"high_confidence_count", highConf,
			"high_confidence_pct", n > 0 ? Math.round(highConf * 1000.0 / n) / 10.0 : 0,
			"ambiguous_count", ambiguous,
			"ambiguous_pct", n > 0 ? Math.round(ambiguous * 1000.0 / n) / 10.0 : 0,
			"mean_score", Math.round(mean * 1000.0) / 1000.0,
			"max_score", Math.round(max * 1000.0) / 1000.0
		));
		output.put("engine_version", ENGINE_VERSION);
		output.put("coverage", COVERAGE);

		return output;
	}

	/**
	 * ParsedChord 객체를 JSON 출력용 Map으로 변환한다.
	 * 위치 정보(bar, beat, symbol, duration)와 분석 결과(analysis)를 분리하여 구조화한다.
	 */
	private Map<String, Object> chordToDict(ParsedChord c) {
		// analysis: Layer 1~3 분석 결과 + 모호성 정보
		Map<String, Object> analysis = new LinkedHashMap<>();
		analysis.put("root", c.getRoot());
		analysis.put("root_name", NoteUtils.pcToNoteName(c.getRoot()));
		analysis.put("quality", c.getQuality());
		analysis.put("normalized_quality", c.getNormalizedQuality() != null ? c.getNormalizedQuality() : c.getQuality());
		analysis.put("tensions", c.getTensions());
		analysis.put("bass", c.getBass());
		analysis.put("bass_name", c.getBass() != null ? NoteUtils.pcToNoteName(c.getBass()) : null);
		analysis.put("degree", c.getDegree());
		analysis.put("is_diatonic", c.getIsDiatonic());
		analysis.put("functions", c.getFunctions());
		analysis.put("secondary_dominant", c.getSecondaryDominant());
		analysis.put("group_memberships", c.getGroupMemberships());
		analysis.put("diminished_function", c.getDiminishedFunction());
		analysis.put("chromatic_approach", c.getChromaticApproach());
		analysis.put("deceptive_resolution", c.getDeceptiveResolution());
		analysis.put("pedal_info", c.getPedalInfo());
		analysis.put("modal_interchange", c.getModalInterchange());
		analysis.put("mode_segment", c.getModeSegment());
		analysis.put("tonicization", c.getTonicization());
		analysis.put("ambiguity_flags", c.getAmbiguityFlags());
		analysis.put("ambiguity_score", c.getAmbiguityScore());

		// 최상위: 위치 정보 + analysis 서브맵
		Map<String, Object> dict = new LinkedHashMap<>();
		dict.put("bar", c.getBar());
		dict.put("beat", c.getBeat());
		dict.put("symbol", c.getOriginalSymbol());
		dict.put("duration_beats", c.getDurationBeats());
		dict.put("analysis", analysis);
		return dict;
	}
}

