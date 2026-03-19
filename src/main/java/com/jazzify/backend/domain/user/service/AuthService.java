package com.jazzify.backend.domain.user.service;

import com.jazzify.backend.core.security.JwtTokenProvider;
import com.jazzify.backend.core.security.RefreshTokenService;
import com.jazzify.backend.domain.user.dto.request.LoginRequest;
import com.jazzify.backend.domain.user.dto.request.SignUpRequest;
import com.jazzify.backend.domain.user.dto.response.SignUpResponse;
import com.jazzify.backend.domain.user.dto.result.TokenResult;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.exception.code.UserErrorCode;
import com.jazzify.backend.domain.user.service.implementation.UserReader;
import com.jazzify.backend.domain.user.service.implementation.UserWriter;
import com.jazzify.backend.domain.user.util.UserMapper;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.UUID;

@NullMarked
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserReader userReader;
    private final UserWriter userWriter;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public SignUpResponse signUp(SignUpRequest request) {
        if (userReader.existsByUsername(request.username())) {
            throw UserErrorCode.DUPLICATE_USERNAME.toException();
        }

        User savedUser = userWriter.create(request.name(), request.username(), request.password());
        return UserMapper.toSignUpResponse(savedUser);
    }

    public TokenResult login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password())
        );

        User user = userReader.getByUsername(authentication.getName());

        UUID publicId = Objects.requireNonNull(user.getPublicId(), "publicId must not be null after persist");
        String accessToken = jwtTokenProvider.createAccessToken(publicId, user.getUsername());
        String refreshToken = refreshTokenService.rotate(publicId, user.getUsername());

        return new TokenResult(accessToken, refreshToken, publicId, user.getUsername());
    }

    public TokenResult reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw UserErrorCode.INVALID_REFRESH_TOKEN.toException();
        }

        String tokenType = jwtTokenProvider.getTokenType(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw UserErrorCode.INVALID_REFRESH_TOKEN.toException();
        }

        UUID publicId = jwtTokenProvider.getPublicId(refreshToken);
        String username = jwtTokenProvider.getUsername(refreshToken);

        String storedToken = refreshTokenService.find(publicId)
                .orElseThrow(UserErrorCode.REFRESH_TOKEN_NOT_FOUND::toException);

        if (!storedToken.equals(refreshToken)) {
            refreshTokenService.delete(publicId);
            throw UserErrorCode.INVALID_REFRESH_TOKEN.toException("토큰 재사용이 감지되었습니다.");
        }

        String newAccessToken = jwtTokenProvider.createAccessToken(publicId, username);
        String newRefreshToken = refreshTokenService.rotate(publicId, username);

        return new TokenResult(newAccessToken, newRefreshToken, publicId, username);
    }

    public void logout(String refreshToken) {
        if (jwtTokenProvider.validateToken(refreshToken)) {
            UUID publicId = jwtTokenProvider.getPublicId(refreshToken);
            refreshTokenService.delete(publicId);
        }
    }

    public long getRefreshTokenExpiration() {
        return jwtTokenProvider.getRefreshExpiration();
    }
}


