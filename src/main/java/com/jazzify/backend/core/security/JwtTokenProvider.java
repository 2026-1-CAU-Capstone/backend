package com.jazzify.backend.core.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

	private final JwtProperties jwtProperties;
	private @Nullable SecretKey secretKey;

	@PostConstruct
	protected void init() {
		this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
	}

	public String createAccessToken(UUID publicId, String username) {
		return createToken(publicId, username, jwtProperties.getAccessExpiration(), "access");
	}

	public String createRefreshToken(UUID publicId, String username) {
		return createToken(publicId, username, jwtProperties.getRefreshExpiration(), "refresh");
	}

	private String createToken(UUID publicId, String username, long expiration, String tokenType) {
		Date now = new Date();
		Date expiryDate = new Date(now.getTime() + expiration);

		return Jwts.builder()
			.subject(publicId.toString())
			.claim("username", username)
			.claim("type", tokenType)
			.issuedAt(now)
			.expiration(expiryDate)
			.signWith(secretKey)
			.compact();
	}

	public boolean validateToken(String token) {
		try {
			parseToken(token);
			return true;
		} catch (JwtException | IllegalArgumentException e) {
			log.warn("JWT 토큰 검증 실패: {}", e.getMessage());
			return false;
		}
	}

	public Claims parseToken(String token) {
		return Jwts.parser()
			.verifyWith(secretKey)
			.build()
			.parseSignedClaims(token)
			.getPayload();
	}

	public UUID getPublicId(String token) {
		return UUID.fromString(parseToken(token).getSubject());
	}

	public String getUsername(String token) {
		return parseToken(token).get("username", String.class);
	}

	public String getTokenType(String token) {
		return parseToken(token).get("type", String.class);
	}

	public long getRefreshExpiration() {
		return jwtProperties.getRefreshExpiration();
	}
}
