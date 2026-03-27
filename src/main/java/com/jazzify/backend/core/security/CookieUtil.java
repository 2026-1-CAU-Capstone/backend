package com.jazzify.backend.core.security;

import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@NullMarked
@Component
public class CookieUtil {

	private static final String REFRESH_TOKEN_COOKIE_NAME = "refreshToken";

	@Value("${cookie.secure:false}")
	private boolean secure;

	public ResponseCookie createRefreshTokenCookie(String refreshToken, long maxAgeMillis) {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, refreshToken)
			.httpOnly(true)
			.secure(secure)
			.path("/api")
			.maxAge(maxAgeMillis / 1000)
			.sameSite(secure ? "None" : "Lax")
			.build();
	}

	public ResponseCookie deleteRefreshTokenCookie() {
		return ResponseCookie.from(REFRESH_TOKEN_COOKIE_NAME, "")
			.httpOnly(true)
			.secure(secure)
			.path("/api")
			.maxAge(0)
			.sameSite(secure ? "None" : "Lax")
			.build();
	}

	public String getRefreshTokenCookieName() {
		return REFRESH_TOKEN_COOKIE_NAME;
	}
}
