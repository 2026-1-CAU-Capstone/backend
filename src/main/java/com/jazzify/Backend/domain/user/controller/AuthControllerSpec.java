package com.jazzify.backend.domain.user.controller;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.user.dto.LoginRequest;
import com.jazzify.backend.domain.user.dto.SignUpRequest;
import com.jazzify.backend.domain.user.dto.SignUpResponse;
import com.jazzify.backend.domain.user.dto.TokenResponse;
import com.jazzify.backend.shared.exception.ApiResponse;
import com.jazzify.backend.shared.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "인증 API")
public interface AuthControllerSpec {

    @Operation(summary = "회원가입")
    ApiResponse<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request);

    @Operation(summary = "로그인", description = "AccessToken은 응답 body, RefreshToken은 쿠키로 반환됩니다.")
    ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse servletResponse);

    @Operation(summary = "토큰 재발급 (RTR)", description = "RefreshToken 쿠키를 사용해 새 AccessToken과 RefreshToken을 발급합니다.")
    ApiResponse<TokenResponse> reissue(HttpServletRequest request, HttpServletResponse servletResponse);

    @Operation(summary = "로그아웃", description = "RefreshToken을 무효화하고 쿠키를 삭제합니다.")
    ApiResponse<Void> logout(HttpServletRequest request, HttpServletResponse servletResponse);

    @Operation(summary = "내 정보 조회", security = @SecurityRequirement(name = "BearerAuth"))
    ApiResponse<CustomPrincipal> me(@AuthenticationPrincipal CustomPrincipal principal);
}
