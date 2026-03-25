package com.jazzify.backend.domain.sheetproject.dto.request;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@NullMarked
public record CreateSheetProjectRequest(
	@NotBlank(message = "제목은 필수입니다.")
	@Size(max = 255, message = "제목은 255자 이하여야 합니다.")
	String title,

	@Nullable
	@Size(max = 50, message = "키는 50자 이하여야 합니다.")
	String key,

	@NotNull(message = "악보 파일 ID는 필수입니다.")
	UUID fileId
) {
}

