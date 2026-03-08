package com.jazzify.backend.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record SignUpRequest(
        @NotBlank(message = "사용자명은 필수입니다.")
        @Size(min = 2, max = 10, message = "사용자명은 2~10자여야 합니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8~20자여야 합니다.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]+$",
                message = "비밀번호는 영문자와 숫자를 모두 포함해야 합니다.")
        String password
) {
}
