package com.jazzify.backend.domain.embedding.dto.request;

import java.util.List;

import org.jspecify.annotations.NullMarked;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 임베딩 프로브 요청 본문.
 * <p>
 * 텍스트 목록을 임베딩 서버로 전송하여 벡터 변환 결과를 확인한다.
 *
 * @param texts 임베딩할 텍스트 목록 (1~64개)
 */
@NullMarked
public record EmbeddingProbeRequest(
	@NotEmpty(message = "texts는 비어 있을 수 없습니다.")
	@Size(max = 64, message = "texts는 최대 64개까지 입력할 수 있습니다.")
	List<@NotBlank(message = "각 text는 비어 있을 수 없습니다.") String> texts
) {
}

