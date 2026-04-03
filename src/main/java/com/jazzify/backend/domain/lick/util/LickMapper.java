package com.jazzify.backend.domain.lick.util;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.domain.lick.dto.response.LickResponse;
import com.jazzify.backend.domain.lick.entity.Lick;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LickMapper {

	public static LickResponse toResponse(Lick lick) {
		return new LickResponse(
			Objects.requireNonNull(lick.getPublicId()),
			lick.getTitle(),
			lick.getContents(),
			Objects.requireNonNull(lick.getCreatedAt()),
			Objects.requireNonNull(lick.getUpdatedAt())
		);
	}
}

