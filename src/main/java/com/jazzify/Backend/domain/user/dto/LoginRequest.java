package com.jazzify.backend.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record LoginRequest(
        @NotBlank(message = "사용자명은 필수입니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        String password
) {
}
