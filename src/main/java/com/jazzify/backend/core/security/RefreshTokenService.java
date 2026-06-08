package com.jazzify.backend.core.security;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.jazzify.backend.domain.user.entity.UserRole;

import lombok.RequiredArgsConstructor;

@NullMarked
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

	private static final String RT_KEY_PREFIX = "RT:";
	private static final DefaultRedisScript<Long> ROTATE_IF_CURRENT_SCRIPT = new DefaultRedisScript<>("""
		local current = redis.call('GET', KEYS[1])
		if not current then
			return -1
		end
		if current ~= ARGV[1] then
			return 0
		end
		redis.call('SET', KEYS[1], ARGV[2], 'PX', ARGV[3])
		return 1
		""", Long.class);

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
	public String rotate(UUID publicId, String username, UserRole role) {
		delete(publicId);
		String newRefreshToken = jwtTokenProvider.createRefreshToken(publicId, username, role);
		save(publicId, newRefreshToken);
		return newRefreshToken;
	}

	/**
	 * RTR refresh 요청에서만 사용한다. 저장된 토큰이 요청 토큰과 일치할 때만 Redis에서 원자적으로 교체한다.
	 */
	public Optional<String> rotateIfCurrent(UUID publicId, String currentRefreshToken, String username, UserRole role) {
		String key = RT_KEY_PREFIX + publicId.toString();
		String newRefreshToken = jwtTokenProvider.createRefreshToken(publicId, username, role);
		Long result = stringRedisTemplate.execute(
			ROTATE_IF_CURRENT_SCRIPT,
			List.of(key),
			currentRefreshToken,
			newRefreshToken,
			String.valueOf(jwtTokenProvider.getRefreshExpiration())
		);

		if (result != null && result == 1L) {
			return Optional.of(newRefreshToken);
		}
		return Optional.empty();
	}
}
