package com.jazzify.backend.domain.user.controller;

import java.util.Arrays;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jazzify.backend.core.security.CookieUtil;
import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.user.dto.request.LoginRequest;
import com.jazzify.backend.domain.user.dto.request.SignUpRequest;
import com.jazzify.backend.domain.user.dto.response.SignUpResponse;
import com.jazzify.backend.domain.user.dto.response.TokenResponse;
import com.jazzify.backend.domain.user.dto.result.TokenResult;
import com.jazzify.backend.domain.user.service.AuthService;
import com.jazzify.backend.domain.user.util.UserMapper;
import com.jazzify.backend.shared.exception.code.UserErrorCode;
import com.jazzify.backend.shared.web.ApiResponse;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@NullMarked
@RestController
@RequestMapping("/v1/auth")
@RequiredArgsConstructor
public class AuthController implements AuthControllerSpec {

	private final AuthService authService;
	private final CookieUtil cookieUtil;

	@PostMapping("/signup")
	@ResponseStatus(HttpStatus.CREATED)
	public ApiResponse<SignUpResponse> signUp(@Valid @RequestBody SignUpRequest request) {
		return ApiResponse.ok(authService.signUp(request));
	}

	@PostMapping("/login")
	public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request,
		HttpServletResponse servletResponse) {
		TokenResult result = authService.login(request);

		ResponseCookie cookie = cookieUtil.createRefreshTokenCookie(
			result.refreshToken(), authService.getRefreshTokenExpiration()
		);
		servletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

		return ApiResponse.ok(UserMapper.toTokenResponse(result));
	}

	@PostMapping("/refresh")
	public ApiResponse<TokenResponse> reissue(HttpServletRequest request,
		HttpServletResponse servletResponse) {
		String refreshToken = extractRefreshToken(request);
		TokenResult result = authService.reissue(refreshToken);

		ResponseCookie cookie = cookieUtil.createRefreshTokenCookie(
			result.refreshToken(), authService.getRefreshTokenExpiration()
		);
		servletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

		return ApiResponse.ok(UserMapper.toTokenResponse(result));
	}

	@PostMapping("/logout")
	public ApiResponse<Void> logout(HttpServletRequest request,
		HttpServletResponse servletResponse) {
		String refreshToken = extractRefreshTokenOrNull(request);
		if (refreshToken != null) {
			authService.logout(refreshToken);
		}

		ResponseCookie cookie = cookieUtil.deleteRefreshTokenCookie();
		servletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

		return ApiResponse.ok();
	}

	@GetMapping("/me")
	public ApiResponse<CustomPrincipal> me(@AuthenticationPrincipal CustomPrincipal principal) {
		return ApiResponse.ok(principal);
	}

	private String extractRefreshToken(HttpServletRequest request) {
		String token = extractRefreshTokenOrNull(request);
		if (token == null) {
			throw UserErrorCode.REFRESH_TOKEN_NOT_FOUND.toException();
		}
		return token;
	}

	private @Nullable String extractRefreshTokenOrNull(HttpServletRequest request) {
		Cookie[] cookies = request.getCookies();
		if (cookies == null) {
			return null;
		}
		return Arrays.stream(cookies)
			.filter(c -> cookieUtil.getRefreshTokenCookieName().equals(c.getName()))
			.map(Cookie::getValue)
			.findFirst()
			.orElse(null);
	}
}
