package com.jazzify.backend.domain.chordproject.util;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.domain.chordproject.dto.response.ChordProjectOmrStatusResponse;
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
			project.getOmrStatus(),
			project.getOmrProgress(),
			project.getOmrFailureReason(),
			Objects.requireNonNull(project.getCreatedAt()),
			Objects.requireNonNull(project.getUpdatedAt())
		);
	}

	public static ChordProjectOmrStatusResponse toOmrStatusResponse(ChordProject project) {
		return new ChordProjectOmrStatusResponse(
			Objects.requireNonNull(project.getPublicId()),
			project.getOmrStatus(),
			project.getOmrProgress(),
			project.getOmrFailureReason()
		);
	}
}
