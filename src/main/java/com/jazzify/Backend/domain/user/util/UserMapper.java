package com.jazzify.backend.domain.user.util;

import com.jazzify.backend.domain.user.dto.SignUpResponse;
import com.jazzify.backend.domain.user.dto.TokenResponse;
import com.jazzify.backend.domain.user.dto.TokenResult;
import com.jazzify.backend.domain.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Objects;

@NullMarked
@Component
@RequiredArgsConstructor
public class UserMapper {

    private final PasswordEncoder passwordEncoder;

    public User toEntity(String username, String rawPassword) {
        return User.builder()
                .username(username)
                .password(Objects.requireNonNull(passwordEncoder.encode(rawPassword)))
                .build();
    }

    public SignUpResponse toSignUpResponse(User user) {
        return new SignUpResponse(
                Objects.requireNonNull(user.getPublicId(), "publicId must not be null after persist"),
                user.getUsername()
        );
    }

    public TokenResponse toTokenResponse(TokenResult result) {
        return new TokenResponse(result.accessToken(), result.publicId(), result.username());
    }
}


