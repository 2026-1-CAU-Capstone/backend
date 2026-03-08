package com.jazzify.backend.domain.user.util;

import com.jazzify.backend.domain.user.dto.response.SignUpResponse;
import com.jazzify.backend.domain.user.dto.response.TokenResponse;
import com.jazzify.backend.domain.user.dto.result.TokenResult;
import com.jazzify.backend.domain.user.entity.User;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
public final class UserMapper {

    private UserMapper() {}

    public static SignUpResponse toSignUpResponse(User user) {
        return new SignUpResponse(
                Objects.requireNonNull(user.getPublicId(), "publicId must not be null after persist"),
                user.getUsername()
        );
    }

    public static TokenResponse toTokenResponse(TokenResult result) {
        return new TokenResponse(result.accessToken(), result.publicId(), result.username());
    }
}
