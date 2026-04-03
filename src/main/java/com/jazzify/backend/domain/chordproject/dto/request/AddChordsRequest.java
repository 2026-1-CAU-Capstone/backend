package com.jazzify.backend.domain.chordproject.dto.request;

import org.jspecify.annotations.NullMarked;

import jakarta.validation.constraints.NotBlank;

/**
 * iRealPro 스타일의 코드 진행 입력 요청.
 * <p>
 * {@code |} 로 마디를 구분하고, 한 마디 안에 코드를 공백으로 나열한다.
 * 마디 내 코드 수에 따라 박자가 균등 분배된다.
 * <ul>
 *   <li>{@code "C | D"} → C 4박, D 4박 (4/4 기준)</li>
 *   <li>{@code "C C D E | D"} → C 2박, D 1박, E 1박 | D 4박</li>
 * </ul>
 * 연속되는 동일 코드는 하나로 병합하여 저장된다.
 */
@NullMarked
public record AddChordsRequest(
	@NotBlank(message = "코드 진행은 비어 있을 수 없습니다.")
	String progression
) {
}

