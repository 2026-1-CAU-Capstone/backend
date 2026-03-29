package com.jazzify.backend.domain.analysis.service;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

import com.jazzify.backend.domain.analysis.dto.response.AnalysisExplanationResponse;
import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.service.implementation.AmbiguityScorer;
import com.jazzify.backend.domain.analysis.service.implementation.AnalysisAggregator;
import com.jazzify.backend.domain.analysis.service.implementation.AnalysisExplainer;
import com.jazzify.backend.domain.analysis.service.implementation.ChordNormalizer;
import com.jazzify.backend.domain.analysis.service.implementation.ChordSymbolParser;
import com.jazzify.backend.domain.analysis.service.implementation.ChordSymbolParser.ParseResult;
import com.jazzify.backend.domain.analysis.service.implementation.ChromaticApproachDetector;
import com.jazzify.backend.domain.analysis.service.implementation.DeceptiveResolutionDetector;
import com.jazzify.backend.domain.analysis.service.implementation.DiatonicClassifier;
import com.jazzify.backend.domain.analysis.service.implementation.DiminishedClassifier;
import com.jazzify.backend.domain.analysis.service.implementation.FunctionLabeler;
import com.jazzify.backend.domain.analysis.service.implementation.IiViDetector;
import com.jazzify.backend.domain.analysis.service.implementation.ModalInterchangeDetector;
import com.jazzify.backend.domain.analysis.service.implementation.ModeSegmentDetector;
import com.jazzify.backend.domain.analysis.service.implementation.PedalPointDetector;
import com.jazzify.backend.domain.analysis.service.implementation.SecondaryDominantDetector;
import com.jazzify.backend.domain.analysis.service.implementation.SectionBoundaryDetector;
import com.jazzify.backend.domain.analysis.service.implementation.TonicizationModulationDetector;
import com.jazzify.backend.domain.analysis.service.implementation.TritoneSubDetector;
import com.jazzify.backend.shared.exception.code.AnalysisErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * 화성 분석 파이프라인의 메인 오케스트레이터.
 * 17개의 분석 컴포넌트를 정해진 순서(6단계 Phase)로 호출하여 전체 분석을 수행한다.
 * Python main.py → analyze()에서 포팅됨.
 */
@NullMarked
@Service
@RequiredArgsConstructor
public class HarmonicAnalysisService {

	private final ChordSymbolParser chordSymbolParser;
	private final ChordNormalizer chordNormalizer;
	private final DiatonicClassifier diatonicClassifier;
	private final FunctionLabeler functionLabeler;
	private final IiViDetector iiViDetector;
	private final TritoneSubDetector tritoneSubDetector;
	private final SecondaryDominantDetector secondaryDominantDetector;
	private final DiminishedClassifier diminishedClassifier;
	private final ChromaticApproachDetector chromaticApproachDetector;
	private final DeceptiveResolutionDetector deceptiveResolutionDetector;
	private final PedalPointDetector pedalPointDetector;
	private final ModalInterchangeDetector modalInterchangeDetector;
	private final ModeSegmentDetector modeSegmentDetector;
	private final TonicizationModulationDetector tonicizationDetector;
	private final SectionBoundaryDetector sectionBoundaryDetector;
	private final AmbiguityScorer ambiguityScorer;
	private final AnalysisAggregator aggregator;
	private final AnalysisExplainer explainer;

	/**
	 * 전체 분석 파이프라인을 실행한다.
	 * 텍스트 코드 진행을 입력받아 6단계를 거쳐 완전한 분석 결과 Map을 반환한다.
	 *
	 * @param text          코드 진행 텍스트 (마디 구분: |, 코드 구분: 공백)
	 * @param key           곡의 키 (예: "C", "Bb", "F#m")
	 * @param title         곡 제목
	 * @param timeSignature 박자 (예: "4/4")
	 * @return JSON 직렬화 가능한 분석 결과 Map
	 */
	public Map<String, Object> analyze(String text, String key, String title, String timeSignature) {
		PipelineResult pr = runPipeline(text, key, title, timeSignature);
		return aggregator.aggregate(title, key, timeSignature, pr.chords, pr.groups, pr.sections);
	}

	/**
	 * 분석 파이프라인을 실행한 뒤 결과를 자연어(한국어)로 설명한다.
	 *
	 * @param text          코드 진행 텍스트
	 * @param key           곡의 키
	 * @param title         곡 제목
	 * @param timeSignature 박자
	 * @return 자연어 설명 응답 DTO
	 */
	public AnalysisExplanationResponse explain(String text, String key, String title, String timeSignature) {
		PipelineResult pr = runPipeline(text, key, title, timeSignature);
		return explainer.explain(title, key, timeSignature, pr.chords, pr.groups, pr.sections);
	}

	// ── 내부 파이프라인 ──

	private record PipelineResult(
		List<ParsedChord> chords,
		List<Map<String, Object>> groups,
		List<Map<String, Object>> sections
	) {
	}

	/**
	 * 6단계 분석 파이프라인의 공통 로직.
	 */
	private PipelineResult runPipeline(String text, String key, String title, String timeSignature) {
		// Phase 1: 텍스트 파싱 → ParsedChord 리스트 생성
		ParseResult pr = chordSymbolParser.parseProgressionText(text, title, key, timeSignature);
		List<ParsedChord> chords = pr.chords();

		if (chords.isEmpty()) {
			throw AnalysisErrorCode.NO_CHORDS_PARSED.toException();
		}

		// Phase 2: Layer 1 – 개별 코드 분석 (품질 정규화 → 다이어토닉 분류 → 기능 라벨링)
		chordNormalizer.normalize(chords);
		diatonicClassifier.classify(chords, key);
		functionLabeler.label(chords, key);

		// Phase 3: Layer 2 – 문맥 패턴 감지 (앞뒤 코드 관계 분석)
		IiViDetector.IiViResult iiViResult = iiViDetector.detect(chords, key);
		chords = iiViResult.chords();
		List<Map<String, Object>> groups = iiViResult.groups();

		functionLabeler.labelFromGroups(chords);           // ii-V-I 역할 기반 기능 보완
		tritoneSubDetector.detect(chords, groups);         // 트라이톤 대리 감지
		secondaryDominantDetector.detect(chords, key);     // 세컨더리 도미넌트 감지
		diminishedClassifier.detect(chords, key);          // 감화음 기능 분류
		chromaticApproachDetector.detect(chords);          // 반음계적 접근 감지
		deceptiveResolutionDetector.detect(chords, key);   // 기만 종지 감지
		pedalPointDetector.detect(chords, key);            // 페달 포인트 감지

		// Phase 4: Layer 3 – 구조 분석 (곡 전체 차원)
		modalInterchangeDetector.detect(chords, key);      // 모달 인터체인지(차용 화음) 감지
		modeSegmentDetector.detect(chords, key);           // 모드 세그먼트 감지
		tonicizationDetector.detect(chords, key, groups);  // 조성화 vs 전조 판별
		List<Map<String, Object>> sections = sectionBoundaryDetector.detect(chords, key); // 섹션 경계

		// Phase 5: 모호성 채점 – 각 코드의 분석 확신도 0.0~1.0 계산
		ambiguityScorer.score(chords);

		return new PipelineResult(chords, groups, sections);
	}
}
