package com.jazzify.backend.domain.chordproject.dto.request;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@NullMarked
public record AddChordsRequest(
	@NotEmpty(message = "코드 목록은 비어 있을 수 없습니다.")
	@Valid
	List<ChordEntry> chords
) {

	public record ChordEntry(
		@Nullable String chord,

		@NotNull(message = "마디 번호는 필수입니다.")
		@Positive(message = "마디 번호는 양수여야 합니다.")
		Integer bar,

		@NotNull(message = "박 위치는 필수입니다.")
		Double beat,

		@NotNull(message = "지속 박수는 필수입니다.")
		@Min(value = 0, message = "지속 박수는 0 이상이어야 합니다.")
		Double durationBeats
	) {
	}
}

