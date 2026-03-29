package com.jazzify.backend.domain.analysis.service.implementation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;

import com.jazzify.backend.domain.analysis.dto.response.AnalysisExplanationResponse;
import com.jazzify.backend.domain.analysis.dto.response.ChordExplanation;
import com.jazzify.backend.domain.analysis.model.FunctionEntry;
import com.jazzify.backend.domain.analysis.model.GroupMembership;
import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.util.NoteUtils;

/**
 * 화성 분석 결과를 자연어(한국어)로 설명하는 컴포넌트.
 */
@NullMarked
@Component
public class AnalysisExplainer {

	public AnalysisExplanationResponse explain(String title, String key, String timeSignature,
		List<ParsedChord> chords,
		List<Map<String, Object>> groups,
		List<Map<String, Object>> sections) {

		String overview = buildOverview(title, key, timeSignature, chords);
		List<ChordExplanation> chordExplanations = buildChordExplanations(chords, key);
		List<String> sectionExplanations = buildSectionExplanations(sections, chords, key);
		List<String> notablePatterns = buildNotablePatterns(chords, groups);
		String harmonicSummary = buildHarmonicSummary(chords);

		return new AnalysisExplanationResponse(
			title, key, timeSignature, overview, chordExplanations, sectionExplanations, notablePatterns, harmonicSummary
		);
	}

	// ── 개요 ──

	private String buildOverview(String title, String key, String timeSignature, List<ParsedChord> chords) {
		int totalChords = chords.size();
		int maxBar = chords.stream().mapToInt(ParsedChord::getBar).max().orElse(0);
		long diatonicCount = chords.stream().filter(c -> Boolean.TRUE.equals(c.getIsDiatonic())).count();
		long nonDiatonicCount = totalChords - diatonicCount;
		double diatonicPct = totalChords > 0 ? Math.round(diatonicCount * 1000.0 / totalChords) / 10.0 : 0;

		return String.format(
			"\"%s\" — 키: %s, 박자: %s, 총 %d마디, 코드 %d개. "
				+ "다이어토닉 코드 %d개(%.1f%%), 비다이어토닉 코드 %d개(%.1f%%).",
			title, key, timeSignature, maxBar, totalChords,
			diatonicCount, diatonicPct, nonDiatonicCount, 100 - diatonicPct
		);
	}

	// ── 코드별 상세 설명 ──

	private List<ChordExplanation> buildChordExplanations(List<ParsedChord> chords, String key) {
		List<ChordExplanation> explanations = new ArrayList<>();
		for (int i = 0; i < chords.size(); i++) {
			ParsedChord chord = chords.get(i);
			ParsedChord prev = i > 0 ? chords.get(i - 1) : null;
			ParsedChord next = i + 1 < chords.size() ? chords.get(i + 1) : null;
			explanations.add(buildSingleChordExplanation(chord, prev, next, key));
		}
		return explanations;
	}

	private ChordExplanation buildSingleChordExplanation(ParsedChord chord, ParsedChord prev,
		ParsedChord next, String key) {

		return new ChordExplanation(
			chord.getBar(),
			chord.getBeat(),
			chord.getOriginalSymbol(),
			chord.getDegree(),
			Boolean.TRUE.equals(chord.getIsDiatonic()),
			describeChordBasics(chord, key),
			describeFunctions(chord, key),
			describePatternRoles(chord),
			describeVoiceLeading(chord, prev, next),
			describeAmbiguity(chord),
			buildChordSummary(chord)
		);
	}

	/**
	 * 코드 기본 정보: 근음, 품질, 구성음 구조, 텐션, 슬래시 베이스, 다이어토닉 여부, 디그리
	 */
	private String describeChordBasics(ParsedChord chord, String key) {
		StringBuilder sb = new StringBuilder();
		String rootName = NoteUtils.pcToNoteName(chord.getRoot());
		String nq = chord.getNormalizedQuality() != null ? chord.getNormalizedQuality() : chord.getQuality();

		sb.append(String.format("%s: 근음 %s, %s",
			chord.getOriginalSymbol(), rootName, qualityToKorean(nq)));

		if (chord.getTensions() != null && !chord.getTensions().isEmpty()) {
			sb.append(String.format(". 텐션: %s", String.join(", ", chord.getTensions())));
		}

		if (chord.getBass() != null && chord.getBass() != chord.getRoot()) {
			String bassName = NoteUtils.pcToNoteName(chord.getBass());
			sb.append(String.format(". 슬래시 베이스: %s(근음 %s와 상이한 베이스음)", bassName, rootName));
		}

		sb.append(". ");

		if (Boolean.TRUE.equals(chord.getIsDiatonic())) {
			sb.append(String.format("%s 키의 다이어토닉 코드. 스케일 디그리: %s.",
				key, chord.getDegree() != null ? chord.getDegree() : "?"));
		} else {
			sb.append(String.format("%s 키의 비다이어토닉 코드. 스케일 디그리: %s.",
				key, chord.getDegree() != null ? chord.getDegree() : "?"));
		}

		return sb.toString();
	}

	/**
	 * 화성 기능(T/SD/D) 분석
	 */
	private String describeFunctions(ParsedChord chord, String key) {
		List<FunctionEntry> funcs = chord.getFunctions();
		if (funcs.isEmpty()) {
			return String.format("%s 키에서 화성 기능이 확정되지 않았습니다. 문맥 분석 필요.", key);
		}

		StringBuilder sb = new StringBuilder();
		if (funcs.size() == 1) {
			FunctionEntry f = funcs.getFirst();
			sb.append(String.format("화성 기능: %s (확신도 %.0f%%)",
				functionToKorean(f.getFunction()), f.getConfidence() * 100));
			if (f.getNote() != null && !f.getNote().isEmpty()) {
				sb.append(String.format(". %s", f.getNote()));
			}
		} else {
			sb.append("복수 화성 기능 후보: ");
			for (int i = 0; i < funcs.size(); i++) {
				FunctionEntry f = funcs.get(i);
				if (i > 0) sb.append(" / ");
				sb.append(String.format("%s(확신도 %.0f%%)",
					functionToKorean(f.getFunction()), f.getConfidence() * 100));
				if (f.getNote() != null && !f.getNote().isEmpty()) {
					sb.append(String.format("[%s]", f.getNote()));
				}
			}
		}
		return sb.toString();
	}

	/**
	 * 패턴/그룹 역할 설명
	 */
	private List<String> describePatternRoles(ParsedChord chord) {
		List<String> roles = new ArrayList<>();

		// ii-V-I 그룹
		for (GroupMembership gm : chord.getGroupMemberships()) {
			if ("ii-V-I".equals(gm.getGroupType())) {
				roles.add(String.format("ii-V-I 그룹 #%d — 역할: %s, 변형: %s. %s",
					gm.getGroupId(),
					iiViRoleToKorean(gm.getRole()),
					iiViVariantToKorean(gm.getVariant()),
					iiViRoleDescription(gm.getRole())));
			}
		}

		// 세컨더리 도미넌트
		if (chord.getSecondaryDominant() != null) {
			var sd = chord.getSecondaryDominant();
			String resolvedDesc = sd.isResolved()
				? String.format("다음 코드 %s로 해결됨", sd.getTargetChord())
				: "미해결";
			roles.add(String.format("세컨더리 도미넌트: %s. 임시 토닉 대상: %s. %s.",
				sd.getType(), sd.getTargetChord(), resolvedDesc));
			if (sd.getImpliedDominant() != null) {
				roles.add(String.format("dim7 도미넌트 해석: %s의 rootless 형태(근음 생략 V7b9).",
					sd.getImpliedDominant()));
			}
		}

		// 감화음 기능
		if (chord.getDiminishedFunction() != null) {
			String dimDesc = switch (chord.getDiminishedFunction()) {
				case "passing" -> "경과 감화음(passing diminished): 인접 코드 간 반음 진행으로 연결.";
				case "auxiliary" -> "보조 감화음(auxiliary diminished): 동일 근음 코드 사이에 위치하는 반음 이웃음.";
				case "dominant_function" -> "도미넌트 기능의 감화음: 근음 생략 V7b9로 기능. 다음 코드로 반음 상행 해결.";
				default -> "감화음 기능 미확정.";
			};
			roles.add(dimDesc);
		}

		// 반음계적 접근
		if (chord.getChromaticApproach() != null) {
			var ca = chord.getChromaticApproach();
			String dirDesc = "below".equals(ca.getDirection()) ? "반음 하방에서 상행 접근" : "반음 상방에서 하행 접근";
			String qualityNote = ca.isQualityMatch() ? "품질 동일" : "품질 상이";
			roles.add(String.format("반음계적 접근(chromatic approach): %s → %s. (%s)",
				dirDesc, ca.getTarget(), qualityNote));
		}

		// 기만 종지
		if (chord.getDeceptiveResolution() != null) {
			var dr = chord.getDeceptiveResolution();
			String commonNote = dr.isCommonPattern() ? "일반적 패턴" : "비일반적 패턴";
			roles.add(String.format("기만 종지(deceptive cadence): 기대 해결 %s 대신 %s(%s)로 진행. %s.",
				dr.getExpectedResolution(), dr.getActualResolution(), dr.getActualDegree(), commonNote));
		}

		// 페달 포인트
		if (chord.getPedalInfo() != null) {
			var pi = chord.getPedalInfo();
			roles.add(String.format("페달 포인트 구간(마디 %d~%d): 베이스음 %s(%s) 지속.",
				pi.getPedalStartBar(), pi.getPedalEndBar(),
				pi.getPedalNoteName(), pedalTypeToKorean(pi.getPedalType())));
		}

		// 모달 인터체인지
		if (chord.getModalInterchange() != null) {
			var mi = chord.getModalInterchange();
			String commonNote = mi.isCommonBorrow() ? "일반적 차용" : "비일반적 차용";
			roles.add(String.format("모달 인터체인지: %s 모드의 %s 디그리에서 차용. %s.",
				mi.getSourceMode(), mi.getBorrowedDegree(), commonNote));
			if (mi.getAllPossibleSources() != null && mi.getAllPossibleSources().size() > 1) {
				List<String> sources = mi.getAllPossibleSources().stream()
					.map(s -> s.getSourceMode() + "/" + s.getBorrowedDegree())
					.toList();
				roles.add(String.format("차용 모드 후보: %s", String.join(", ", sources)));
			}
		}

		// 조성화/전조
		if (chord.getTonicization() != null) {
			var tonic = chord.getTonicization();
			if ("modulation".equals(tonic.getType())) {
				roles.add(String.format("전조(modulation): 마디 %d~%d, 대상 키 %s. 확신도 %.0f%%.",
					tonic.getStartBar(), tonic.getEndBar(), tonic.getTemporaryKey(), tonic.getConfidence() * 100));
			} else {
				roles.add(String.format("조성화(tonicization): 마디 %d~%d, 임시 키 %s. 확신도 %.0f%%.",
					tonic.getStartBar(), tonic.getEndBar(), tonic.getTemporaryKey(), tonic.getConfidence() * 100));
			}
		}

		return roles;
	}

	/**
	 * 전후 코드와의 근음 음정 관계
	 */
	private String describeVoiceLeading(ParsedChord chord, ParsedChord prev, ParsedChord next) {
		StringBuilder sb = new StringBuilder();

		if (prev != null) {
			int intervalFromPrev = NoteUtils.interval(prev.getRoot(), chord.getRoot());
			sb.append(String.format("이전 코드 %s로부터 %s",
				prev.getOriginalSymbol(), intervalToKorean(intervalFromPrev)));
			if (intervalFromPrev == 5) {
				sb.append("(5도권 진행)");
			} else if (intervalFromPrev == 7) {
				sb.append("(역행 5도)");
			}
			sb.append(". ");
		} else {
			sb.append("첫 번째 코드. ");
		}

		if (next != null) {
			int intervalToNext = NoteUtils.interval(chord.getRoot(), next.getRoot());
			sb.append(String.format("다음 코드 %s로 %s",
				next.getOriginalSymbol(), intervalToKorean(intervalToNext)));
			if (intervalToNext == 5) {
				sb.append("(5도권 진행)");
			} else if (intervalToNext == 0) {
				sb.append("(동음 근음, 품질 변화만)");
			}
			sb.append(".");
		} else {
			sb.append("마지막 코드.");
		}

		return sb.toString();
	}

	/**
	 * 모호성 점수 및 플래그
	 */
	private String describeAmbiguity(ParsedChord chord) {
		double score = chord.getAmbiguityScore();
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("모호성 점수: %.3f", score));

		if (score <= 0.05) {
			sb.append(" (단일 해석, 확신도 매우 높음)");
		} else if (score <= 0.1) {
			sb.append(" (단일 해석, 확신도 높음)");
		} else if (score <= 0.2) {
			sb.append(" (주요 해석 1개, 소수 대안 존재)");
		} else if (score <= 0.3) {
			sb.append(" (복수 해석 가능, 문맥 의존)");
		} else if (score <= 0.5) {
			sb.append(" (복수 해석 경합)");
		} else {
			sb.append(" (해석 불확정, 광범위 문맥 필요)");
		}

		if (!chord.getAmbiguityFlags().isEmpty()) {
			for (var flag : chord.getAmbiguityFlags()) {
				sb.append(String.format(" | %s 후보: %s",
					flag.getAspect(), String.join(" / ", flag.getInterpretations())));
			}
		}

		return sb.toString();
	}

	/**
	 * 코드 한 줄 요약
	 */
	private String buildChordSummary(ParsedChord chord) {
		StringBuilder sb = new StringBuilder();

		sb.append(String.format("마디 %d 박 %.0f: %s",
			chord.getBar(), chord.getBeat(), chord.getOriginalSymbol()));

		if (chord.getDegree() != null) {
			sb.append(String.format("(%s)", chord.getDegree()));
		}

		sb.append(Boolean.TRUE.equals(chord.getIsDiatonic()) ? " [다이어토닉]" : " [비다이어토닉]");

		if (!chord.getFunctions().isEmpty()) {
			FunctionEntry primary = chord.getFunctions().getFirst();
			sb.append(String.format(" | %s(%.0f%%)",
				functionToKorean(primary.getFunction()), primary.getConfidence() * 100));
		}

		if (chord.getSecondaryDominant() != null) {
			sb.append(String.format(" | 세컨더리 도미넌트 %s", chord.getSecondaryDominant().getType()));
		}
		if (!chord.getGroupMemberships().isEmpty()) {
			GroupMembership gm = chord.getGroupMemberships().getFirst();
			sb.append(String.format(" | ii-V-I#%d %s", gm.getGroupId(), gm.getRole()));
		}
		if (chord.getModalInterchange() != null) {
			sb.append(String.format(" | 차용: %s/%s",
				chord.getModalInterchange().getSourceMode(), chord.getModalInterchange().getBorrowedDegree()));
		}
		if (chord.getDeceptiveResolution() != null) {
			sb.append(String.format(" | 기만종지→%s", chord.getDeceptiveResolution().getActualDegree()));
		}
		if (chord.getDiminishedFunction() != null) {
			sb.append(String.format(" | 감화음(%s)", chord.getDiminishedFunction()));
		}
		if (chord.getChromaticApproach() != null) {
			sb.append(String.format(" | 반음계적접근→%s", chord.getChromaticApproach().getTarget()));
		}

		return sb.toString();
	}

	// ── 섹션별 설명 ──

	private List<String> buildSectionExplanations(List<Map<String, Object>> sections,
		List<ParsedChord> chords, String key) {
		List<String> explanations = new ArrayList<>();

		for (int i = 0; i < sections.size(); i++) {
			Map<String, Object> section = sections.get(i);
			int startBar = ((Number)section.get("start_bar")).intValue();
			int endBar = ((Number)section.get("end_bar")).intValue();
			String sectionKey = (String)section.get("key");
			String mode = (String)section.getOrDefault("mode", "ionian");
			String type = (String)section.getOrDefault("type", "original_key");

			StringBuilder sb = new StringBuilder();
			sb.append(String.format("[섹션 %d] 마디 %d~%d — ", i + 1, startBar, endBar));

			if ("modulation".equals(type)) {
				sb.append(String.format("키: %s(전조). ", sectionKey));
			} else {
				sb.append(String.format("키: %s. ", sectionKey));
			}

			sb.append(String.format("모드: %s. ", modeToKorean(mode)));

			List<ParsedChord> sectionChords = chords.stream()
				.filter(c -> c.getBar() >= startBar && c.getBar() <= endBar)
				.toList();

			if (!sectionChords.isEmpty()) {
				String progression = sectionChords.stream()
					.map(c -> c.getOriginalSymbol() + "(" + (c.getDegree() != null ? c.getDegree() : "?") + ")")
					.collect(Collectors.joining(" → "));
				sb.append("코드 진행: ").append(progression);
			}

			Object tonicizations = section.get("tonicizations");
			if (tonicizations instanceof List<?> tonics && !tonics.isEmpty()) {
				sb.append(String.format(". 조성화 대상 키: %s", tonics));
			}

			explanations.add(sb.toString());
		}

		return explanations;
	}

	// ── 주목할 패턴 ──

	@SuppressWarnings("unchecked")
	private List<String> buildNotablePatterns(List<ParsedChord> chords, List<Map<String, Object>> groups) {
		List<String> patterns = new ArrayList<>();

		// ii-V-I 그룹
		for (Map<String, Object> group : groups) {
			String variant = (String)group.getOrDefault("variant", "standard");
			String targetKey = (String)group.getOrDefault("target_key", "?");
			List<Map<String, Object>> members = (List<Map<String, Object>>)group.get("members");
			String memberSymbols = members != null
				? members.stream().map(m -> (String)m.get("symbol")).collect(Collectors.joining(" → "))
				: "?";

			String description = switch (variant) {
				case "standard" -> String.format("ii-V-I (%s) → 대상 키: %s", memberSymbols, targetKey);
				case "minor" -> String.format("ii-V-i 단조형 (%s) → 대상 키: %s", memberSymbols, targetKey);
				case "tritone_sub_V" -> String.format("ii-V-I 트라이톤 대리 V (%s) → 대상 키: %s. V7 위치에 트라이톤(증4도) 관계 dom7 사용.", memberSymbols, targetKey);
				case "backdoor" -> String.format("백도어 진행 (%s) → 대상 키: %s. 패턴: iv-bVII7-I.", memberSymbols, targetKey);
				case "incomplete" -> String.format("불완전 ii-V (%s) → 대상 키: %s. I로의 해결 없음.", memberSymbols, targetKey);
				default -> String.format("ii-V-I 변형[%s] (%s) → 대상 키: %s", variant, memberSymbols, targetKey);
			};
			patterns.add(description);
		}

		// 세컨더리 도미넌트
		for (ParsedChord chord : chords) {
			if (chord.getSecondaryDominant() != null) {
				var sd = chord.getSecondaryDominant();
				String resolvedStr = sd.isResolved() ? "해결됨" : "미해결";
				patterns.add(String.format("세컨더리 도미넌트: %s → %s, 대상 코드: %s [%s]",
					chord.getOriginalSymbol(), sd.getType(), sd.getTargetChord(), resolvedStr));
			}
		}

		// 모달 인터체인지
		for (ParsedChord chord : chords) {
			if (chord.getModalInterchange() != null) {
				var mi = chord.getModalInterchange();
				String commonStr = mi.isCommonBorrow() ? "일반적 차용" : "비일반적 차용";
				patterns.add(String.format("모달 인터체인지: %s — 출처: %s 모드 %s (%s)",
					chord.getOriginalSymbol(), mi.getSourceMode(), mi.getBorrowedDegree(), commonStr));
			}
		}

		// 기만 종지
		for (ParsedChord chord : chords) {
			if (chord.getDeceptiveResolution() != null) {
				var dr = chord.getDeceptiveResolution();
				String commonNote = dr.isCommonPattern() ? "일반적 패턴" : "비일반적 패턴";
				patterns.add(String.format("기만 종지: %s — 기대 해결 %s → 실제 해결 %s(%s). %s.",
					dr.getDominantChord(), dr.getExpectedResolution(),
					dr.getActualResolution(), dr.getActualDegree(), commonNote));
			}
		}

		// 반음계적 접근
		for (ParsedChord chord : chords) {
			if (chord.getChromaticApproach() != null) {
				var ca = chord.getChromaticApproach();
				String dirStr = "below".equals(ca.getDirection()) ? "반음 하방 상행" : "반음 상방 하행";
				patterns.add(String.format("반음계적 접근: %s → %s (%s, 품질 %s)",
					chord.getOriginalSymbol(), ca.getTarget(), dirStr,
					ca.isQualityMatch() ? "동일" : "상이"));
			}
		}

		// 감화음
		for (ParsedChord chord : chords) {
			if (chord.getDiminishedFunction() != null) {
				String funcDesc = switch (chord.getDiminishedFunction()) {
					case "passing" -> "경과(passing)";
					case "auxiliary" -> "보조(auxiliary)";
					case "dominant_function" -> "도미넌트 기능(rootless V7b9)";
					default -> "미확정";
				};
				patterns.add(String.format("감화음: %s — 마디 %d, 기능: %s",
					chord.getOriginalSymbol(), chord.getBar(), funcDesc));
			}
		}

		// 페달 포인트 (구간별로 중복 없이)
		chords.stream()
			.filter(c -> c.getPedalInfo() != null)
			.map(c -> c.getPedalInfo())
			.distinct()
			.forEach(pi -> patterns.add(String.format(
				"페달 포인트: 베이스음 %s(%s), 마디 %d~%d",
				pi.getPedalNoteName(), pedalTypeToKorean(pi.getPedalType()),
				pi.getPedalStartBar(), pi.getPedalEndBar())));

		// 조성화/전조
		for (ParsedChord chord : chords) {
			if (chord.getTonicization() != null) {
				var tonic = chord.getTonicization();
				if ("modulation".equals(tonic.getType())) {
					patterns.add(String.format("전조: 마디 %d~%d → %s 키 (확신도 %.0f%%)",
						tonic.getStartBar(), tonic.getEndBar(),
						tonic.getTemporaryKey(), tonic.getConfidence() * 100));
				} else {
					patterns.add(String.format("조성화: 마디 %d~%d → 임시 키 %s (확신도 %.0f%%)",
						tonic.getStartBar(), tonic.getEndBar(),
						tonic.getTemporaryKey(), tonic.getConfidence() * 100));
				}
			}
		}

		return patterns.stream().distinct().toList();
	}

	// ── 화성 요약 ──

	private String buildHarmonicSummary(List<ParsedChord> chords) {
		if (chords.isEmpty()) {
			return "분석할 코드가 없습니다.";
		}

		int total = chords.size();

		long tonicCount = chords.stream()
			.filter(c -> c.getFunctions().stream().anyMatch(f -> "T".equals(f.getFunction())))
			.count();
		long subdominantCount = chords.stream()
			.filter(c -> c.getFunctions().stream().anyMatch(f -> "SD".equals(f.getFunction())))
			.count();
		long dominantCount = chords.stream()
			.filter(c -> c.getFunctions().stream().anyMatch(f ->
				"D".equals(f.getFunction()) || "D_substitute".equals(f.getFunction())))
			.count();

		long highConfCount = chords.stream().filter(c -> c.getAmbiguityScore() <= 0.1).count();
		long ambiguousCount = chords.stream().filter(c -> c.getAmbiguityScore() > 0.3).count();
		double meanAmbiguity = chords.stream().mapToDouble(ParsedChord::getAmbiguityScore).average().orElse(0);

		long minorCount = chords.stream().filter(c -> {
			String q = c.getNormalizedQuality() != null ? c.getNormalizedQuality() : c.getQuality();
			return q.startsWith("min") || q.startsWith("dim");
		}).count();
		long majorCount = total - minorCount;

		return String.format(
			"총 코드 %d개. "
				+ "기능 분포 — 토닉(T): %d개(%.0f%%), 서브도미넌트(SD): %d개(%.0f%%), 도미넌트(D): %d개(%.0f%%). "
				+ "품질 분포 — 메이저/도미넌트 계열: %d개, 마이너/디미니시드 계열: %d개. "
				+ "모호성 — 평균 점수 %.3f, 고확신도(≤0.1): %d개, 모호(>0.3): %d개.",
			total,
			tonicCount, tonicCount * 100.0 / total,
			subdominantCount, subdominantCount * 100.0 / total,
			dominantCount, dominantCount * 100.0 / total,
			majorCount, minorCount,
			meanAmbiguity, highConfCount, ambiguousCount
		);
	}

	// ── 헬퍼 ──

	private String modeToKorean(String mode) {
		return switch (mode) {
			case "ionian" -> "이오니안(장조 음계, W-W-H-W-W-W-H)";
			case "dorian" -> "도리안(자연단음계에서 장6도)";
			case "phrygian" -> "프리지안(자연단음계에서 단2도)";
			case "lydian" -> "리디안(장음계에서 증4도)";
			case "mixolydian" -> "믹솔리디안(장음계에서 단7도)";
			case "aeolian" -> "에올리안(자연단음계)";
			case "locrian" -> "로크리안(자연단음계에서 단2도·감5도)";
			default -> mode;
		};
	}

	private String pedalTypeToKorean(String type) {
		return switch (type) {
			case "tonic" -> "으뜸음";
			case "dominant" -> "도미넌트";
			case "subdominant" -> "서브도미넌트";
			default -> type;
		};
	}

	/** 코드 품질 → 구성음 구조 설명 */
	private String qualityToKorean(String quality) {
		return switch (quality) {
			case "maj7" -> "장3화음 + 장7도(maj7)";
			case "maj" -> "장3화음";
			case "min7" -> "단3화음 + 단7도(m7)";
			case "min" -> "단3화음";
			case "dom7" -> "장3화음 + 단7도(dominant 7)";
			case "min7b5" -> "단3화음 + 감5도 + 단7도(half-diminished, m7♭5)";
			case "dim7" -> "단3화음 + 감5도 + 감7도(diminished 7, 완전대칭)";
			case "dim" -> "단3화음 + 감5도(diminished triad)";
			case "aug" -> "장3화음 + 증5도(augmented triad)";
			case "aug7" -> "장3화음 + 증5도 + 단7도(augmented 7)";
			case "augmaj7" -> "장3화음 + 증5도 + 장7도(augmented maj7)";
			case "sus4" -> "완전4도 + 완전5도(sus4, 3도 없음)";
			case "sus2" -> "장2도 + 완전5도(sus2, 3도 없음)";
			case "dom7sus4" -> "완전4도 + 완전5도 + 단7도(7sus4)";
			case "min6" -> "단3화음 + 장6도(m6)";
			case "maj6" -> "장3화음 + 장6도(6)";
			case "minmaj7" -> "단3화음 + 장7도(minMaj7)";
			case "power" -> "근음 + 완전5도(power chord, 3도 없음)";
			default -> quality;
		};
	}

	/** 화성 기능 → 한국어 */
	private String functionToKorean(String function) {
		return switch (function) {
			case "T" -> "토닉(T)";
			case "SD" -> "서브도미넌트(SD)";
			case "D" -> "도미넌트(D)";
			case "D_substitute" -> "도미넌트 대리(D_sub)";
			case "D_mediant" -> "도미넌트 중음(D_med)";
			case "chromatic_approach" -> "반음계적 접근";
			case "modal_interchange" -> "모달 인터체인지";
			default -> function;
		};
	}

	/** 반음 간격 → 음정명 */
	private String intervalToKorean(int semitones) {
		return switch (semitones) {
			case 0 -> "동음";
			case 1 -> "단2도 상행";
			case 2 -> "장2도 상행";
			case 3 -> "단3도 상행";
			case 4 -> "장3도 상행";
			case 5 -> "완전4도 상행";
			case 6 -> "증4도/감5도(트라이톤)";
			case 7 -> "완전5도 상행";
			case 8 -> "단6도 상행";
			case 9 -> "장6도 상행";
			case 10 -> "단7도 상행";
			case 11 -> "장7도 상행(= 단2도 하행)";
			default -> semitones + "반음";
		};
	}

	/** ii-V-I 변형 → 한국어 */
	private String iiViVariantToKorean(String variant) {
		if (variant == null) return "표준";
		return switch (variant) {
			case "standard" -> "표준";
			case "minor" -> "단조형";
			case "tritone_sub_V" -> "트라이톤 대리 V";
			case "tritone_sub_ii_V" -> "트라이톤 대리 ii";
			case "backdoor" -> "백도어(iv-bVII7-I)";
			case "incomplete" -> "불완전(I 미해결)";
			default -> variant.contains("sus_delay") ? variant + "(sus4 딜레이)" : variant;
		};
	}

	/** ii-V-I 역할 → 한국어 */
	private String iiViRoleToKorean(String role) {
		return switch (role) {
			case "ii" -> "ii";
			case "V" -> "V";
			case "I" -> "I";
			case "ii (tritone sub)" -> "ii(트라이톤 대리)";
			case "iv (backdoor)" -> "iv(백도어)";
			case "V (tritone sub bII7)" -> "V(트라이톤 대리 bII7)";
			case "V (backdoor bVII7)" -> "V(백도어 bVII7)";
			case "V (resolved from sus4)" -> "V(sus4 해결)";
			case "I (iii substitute)" -> "I(iii 대리)";
			default -> role;
		};
	}

	/** ii-V-I 역할별 화성 이론 설명 */
	private String iiViRoleDescription(String role) {
		return switch (role) {
			case "ii" -> "서브도미넌트 기능. V7과 루트가 장2도 관계.";
			case "V" -> "도미넌트 기능. 토닉 루트로부터 완전5도 위.";
			case "I" -> "토닉 기능. 종지 해결점.";
			case "iv (backdoor)" -> "서브도미넌트 기능(단조 iv). bVII7과 반음 관계.";
			case "ii (tritone sub)" -> "서브도미넌트 기능. 원래 ii의 트라이톤 대리.";
			case "V (tritone sub bII7)" -> "도미넌트 기능. 원래 V7의 트라이톤(증4도) 대리. 토닉으로 반음 하행 해결.";
			case "V (backdoor bVII7)" -> "도미넌트 기능. 장조 bVII7(토닉으로부터 단7도). 토닉으로 장2도 하행 해결.";
			case "V (resolved from sus4)" -> "도미넌트 기능. sus4(4도)에서 3도로 해결 후 V7 기능 수행.";
			case "I (iii substitute)" -> "토닉 기능. iii는 I과 구성음 2개 공유(대리 관계).";
			default -> "";
		};
	}
}

