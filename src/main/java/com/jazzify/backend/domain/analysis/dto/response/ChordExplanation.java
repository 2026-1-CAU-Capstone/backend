package com.jazzify.backend.domain.analysis.dto.response;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 개별 코드의 상세 화성 분석 설명.
 *
 * @param bar              마디 번호
 * @param beat             박 위치
 * @param symbol           원래 코드 기호 (예: "Dm7")
 * @param degree           스케일 디그리 (예: "ii")
 * @param diatonic         다이어토닉 여부
 * @param basicDescription 코드 자체에 대한 기본 설명 (근음, 품질, 텐션 등)
 * @param functionAnalysis 화성 기능(T/SD/D) 분석 설명
 * @param patternRoles     이 코드가 속한 패턴/그룹에서의 역할 설명 목록
 * @param voiceLeading     전후 코드와의 연결(성부 진행) 설명
 * @param ambiguityNote    모호성 점수 및 해석
 * @param summary          이 코드에 대한 한 줄 종합 요약
 */
@NullMarked
public record ChordExplanation(
	int bar,
	double beat,
	String symbol,
	@Nullable String degree,
	boolean diatonic,
	String basicDescription,
	String functionAnalysis,
	List<String> patternRoles,
	@Nullable String voiceLeading,
	String ambiguityNote,
	String summary
) {
}

