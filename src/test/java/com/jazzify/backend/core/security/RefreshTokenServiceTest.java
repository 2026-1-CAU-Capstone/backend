package com.jazzify.backend.core.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import com.jazzify.backend.domain.user.entity.UserRole;

@NullMarked
@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

	@Mock
	private StringRedisTemplate stringRedisTemplate;

	@Mock
	private JwtTokenProvider jwtTokenProvider;

	private RefreshTokenService refreshTokenService;

	@BeforeEach
	void setUp() {
		refreshTokenService = new RefreshTokenService(stringRedisTemplate, jwtTokenProvider);
	}

	@Test
	void rotateIfCurrent_returnsNewTokenWhenStoredTokenMatches() {
		UUID publicId = UUID.randomUUID();
		String key = "RT:" + publicId;
		when(jwtTokenProvider.createRefreshToken(publicId, "tester", UserRole.MEMBER)).thenReturn("new-refresh-token");
		when(jwtTokenProvider.getRefreshExpiration()).thenReturn(604_800_000L);
		when(stringRedisTemplate.execute(
			anyRedisScript(),
			eq(List.of(key)),
			eq("current-refresh-token"),
			eq("new-refresh-token"),
			eq("604800000")
		)).thenReturn(1L);

		Optional<String> result = refreshTokenService.rotateIfCurrent(
			publicId,
			"current-refresh-token",
			"tester",
			UserRole.MEMBER
		);

		assertThat(result).contains("new-refresh-token");
		verify(stringRedisTemplate, never()).delete(anyString());
	}

	@Test
	void rotateIfCurrent_returnsEmptyWhenStoredTokenDoesNotMatch() {
		UUID publicId = UUID.randomUUID();
		String key = "RT:" + publicId;
		when(jwtTokenProvider.createRefreshToken(publicId, "tester", UserRole.MEMBER)).thenReturn("new-refresh-token");
		when(jwtTokenProvider.getRefreshExpiration()).thenReturn(604_800_000L);
		when(stringRedisTemplate.execute(
			anyRedisScript(),
			eq(List.of(key)),
			eq("stale-refresh-token"),
			eq("new-refresh-token"),
			eq("604800000")
		)).thenReturn(0L);

		Optional<String> result = refreshTokenService.rotateIfCurrent(
			publicId,
			"stale-refresh-token",
			"tester",
			UserRole.MEMBER
		);

		assertThat(result).isEmpty();
		verify(stringRedisTemplate, never()).delete(anyString());
	}

	private DefaultRedisScript<Long> anyRedisScript() {
		return any();
	}
}
