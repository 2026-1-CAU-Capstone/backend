package com.jazzify.backend.domain.sheetproject.dto.request;

import com.jazzify.backend.shared.domain.MusicKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record SheetProjectUpdateRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 255, message = "제목은 255자 이하여야 합니다.")
        String title,

        @Nullable MusicKey key
) {
}

