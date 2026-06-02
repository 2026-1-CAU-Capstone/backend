package com.jazzify.backend.domain.embedding.dto.response;

import java.util.List;

import org.jspecify.annotations.NullMarked;

/**
 * 임베딩 프로브 응답.
 * <p>
 * 각 입력 텍스트에 대한 임베딩 벡터와 메타데이터를 반환한다.
 *
 * @param dimension 벡터 차원 수 (첫 번째 벡터 기준)
 * @param count     반환된 벡터 개수
 * @param results   텍스트-벡터 쌍 목록 (입력 순서 보장)
 */
@NullMarked
public record EmbeddingProbeResponse(
	int dimension,
	int count,
	List<EmbeddingResultItem> results
) {

	/**
	 * 단일 텍스트-벡터 쌍.
	 *
	 * @param text   입력 텍스트
	 * @param vector 임베딩 벡터
	 */
	public record EmbeddingResultItem(
		String text,
		List<Double> vector
	) {
	}
}

