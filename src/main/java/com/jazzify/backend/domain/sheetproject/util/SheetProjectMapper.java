package com.jazzify.backend.domain.sheetproject.util;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.domain.sheetproject.dto.response.SheetProjectResponse;
import com.jazzify.backend.domain.sheetproject.entity.SheetProject;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SheetProjectMapper {

	public static SheetProjectResponse toResponse(SheetProject project) {
		return new SheetProjectResponse(
			Objects.requireNonNull(project.getPublicId()),
			project.getTitle(),
			project.getKeySignature(),
			Objects.requireNonNull(project.getSheetFile().getPublicId()),
			Objects.requireNonNull(project.getCreatedAt()),
			Objects.requireNonNull(project.getUpdatedAt())
		);
	}
}
