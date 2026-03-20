package com.jazzify.backend.domain.chordproject.dto.request;

import com.jazzify.backend.shared.domain.MusicKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record ChordProjectCreateRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 255, message = "제목은 255자 이하여야 합니다.")
        String title,

        @NotNull(message = "키는 필수입니다.")
        MusicKey key
) {
}

