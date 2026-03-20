package com.jazzify.backend.domain.chordproject.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record CreateChordProjectRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 255, message = "제목은 255자 이하여야 합니다.")
        String title,

        @NotBlank(message = "키는 필수입니다.")
        @Size(max = 50, message = "키는 50자 이하여야 합니다.")
        String key
) {
}

