package com.jazzify.backend.core.security;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@NullMarked
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private static final String RT_KEY_PREFIX = "RT:";

	private final StringRedisTemplate stringRedisTemplate;
	private final JwtTokenProvider jwtTokenProvider;

	public void save(UUID publicId, String refreshToken) {
		String key = RT_KEY_PREFIX + publicId.toString();
		stringRedisTemplate.opsForValue().set(
			key,
			refreshToken,
			jwtTokenProvider.getRefreshExpiration(),
			TimeUnit.MILLISECONDS
		);
	}

	public Optional<String> find(UUID publicId) {
		String key = RT_KEY_PREFIX + publicId.toString();
		String token = stringRedisTemplate.opsForValue().get(key);
		return Optional.ofNullable(token);
	}

	public void delete(UUID publicId) {
		String key = RT_KEY_PREFIX + publicId.toString();
		stringRedisTemplate.delete(key);
	}

	/**
	 * RTR (Refresh Token Rotation): 기존 토큰 삭제 후 새 토큰 저장
	 */
	public String rotate(UUID publicId, String username) {
		delete(publicId);
		String newRefreshToken = jwtTokenProvider.createRefreshToken(publicId, username);
		save(publicId, newRefreshToken);
		return newRefreshToken;
	}
}
