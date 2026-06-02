package com.jazzify.backend.domain.embedding.service;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

import com.jazzify.backend.domain.embedding.dto.request.EmbeddingProbeRequest;
import com.jazzify.backend.domain.embedding.dto.response.EmbeddingHealthResponse;
import com.jazzify.backend.domain.embedding.dto.response.EmbeddingProbeResponse;
import com.jazzify.backend.shared.embedding.EmbeddingClient;

import lombok.RequiredArgsConstructor;

/**
 * 임베딩 서버 테스트 및 헬스체크 서비스.
 * <p>
 * {@link EmbeddingClient}에 위임하여 텍스트 임베딩 결과를 반환하거나
 * 서버 연결 상태를 조회한다.
 */
@Service
@NullMarked
@RequiredArgsConstructor
public class EmbeddingService {

	private final EmbeddingClient embeddingClient;

	/**
	 * 요청의 텍스트 목록을 임베딩 서버로 전송하고 결과를 반환한다.
	 *
	 * @param request 임베딩 대상 텍스트 목록
	 * @return 텍스트-벡터 쌍 목록과 차원 정보
	 */
	public EmbeddingProbeResponse probe(EmbeddingProbeRequest request) {
		List<String> texts = request.texts();
		List<List<Double>> vectors = embeddingClient.embedBatch(texts);

		int dimension = vectors.isEmpty() ? 0 : vectors.getFirst().size();

		List<EmbeddingProbeResponse.EmbeddingResultItem> results = new ArrayList<>();
		for (int i = 0; i < texts.size(); i++) {
			results.add(new EmbeddingProbeResponse.EmbeddingResultItem(texts.get(i), vectors.get(i)));
		}

		return new EmbeddingProbeResponse(dimension, vectors.size(), List.copyOf(results));
	}

	/**
	 * 임베딩 서버 연결 상태를 확인한다.
	 *
	 * @return 설정 여부·URL·연결 가능 여부를 담은 응답
	 */
	public EmbeddingHealthResponse health() {
		EmbeddingClient.EmbeddingHealthStatus status = embeddingClient.checkHealth();
		return new EmbeddingHealthResponse(status.configured(), status.serverUrl(), status.reachable());
	}
}


