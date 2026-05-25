package com.jazzify.backend.domain.rag.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class RagAgentTest {

	@Test
	void decomposeQuery_buildsSongPatternAndFunctionQueries() {
		Map<String, Object> chordContext = Map.of(
			"chord", "F7",
			"key", "Eb",
			"function", "II7",
			"next_chord", "Fm7",
			"patterns_detected", List.of("tritone-sub", "dim-axis"),
			"secondary_dominant", Map.of("targetDegree", "ii")
		);

		List<com.jazzify.backend.domain.rag.model.RagDecomposedQuery> queries = RagAgent.decomposeQuery(
			chordContext,
			"이 F7 위에서 어떻게 솔로해?",
			"It Could Happen to You"
		);

		assertThat(queries)
			.extracting(com.jazzify.backend.domain.rag.model.RagDecomposedQuery::query)
			.contains(
				"It Could Happen to You 코드 분석 화성",
				"It Could Happen to You 솔로 연주 판단",
				"II7 코드 특수성 얼터드 안 어울림 샵11 스케일",
				"세컨더리 도미넌트 V/ii 얼터드 스케일 F7",
				"트라이톤 서브스티튜션 F7 가이드톤 스케일",
				"디미니쉬드 코드 F7 모체 도미넌트 얼터드",
				"이 F7 위에서 어떻게 솔로해? F7 Eb 키"
			);

		assertThat(queries.getFirst().query()).isEqualTo("It Could Happen to You 코드 분석 화성");
	}
}


