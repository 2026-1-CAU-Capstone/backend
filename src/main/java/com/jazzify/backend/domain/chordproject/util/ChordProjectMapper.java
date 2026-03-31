package com.jazzify.backend.domain.chordproject.util;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.domain.chordproject.dto.response.ChordProjectResponse;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChordProjectMapper {

	public static ChordProjectResponse toResponse(ChordProject project) {
		return new ChordProjectResponse(
			Objects.requireNonNull(project.getPublicId()),
			project.getTitle(),
			project.getKeySignature(),
			project.getTimeSignature(),
			Objects.requireNonNull(project.getCreatedAt()),
			Objects.requireNonNull(project.getUpdatedAt())
		);
	}
}
