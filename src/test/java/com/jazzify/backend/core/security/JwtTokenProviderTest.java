package com.jazzify.backend.core.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.jazzify.backend.domain.user.entity.UserRole;

@NullMarked
class JwtTokenProviderTest {

	private JwtTokenProvider jwtTokenProvider;

	@BeforeEach
	void setUp() {
		JwtProperties jwtProperties = new JwtProperties();
		jwtProperties.setSecret("test-secret-key-for-testing-purposes-must-be-at-least-32-characters-long");
		jwtProperties.setAccessExpiration(3_600_000);
		jwtProperties.setRefreshExpiration(604_800_000);

		jwtTokenProvider = new JwtTokenProvider(jwtProperties);
		jwtTokenProvider.init();
	}

	@Test
	void createRefreshToken_generatesUniqueTokenForSameUserAndRole() {
		UUID publicId = UUID.randomUUID();

		String firstToken = jwtTokenProvider.createRefreshToken(publicId, "tester", UserRole.MEMBER);
		String secondToken = jwtTokenProvider.createRefreshToken(publicId, "tester", UserRole.MEMBER);

		assertThat(secondToken).isNotEqualTo(firstToken);
		assertThat(jwtTokenProvider.parseToken(firstToken).getId()).isNotBlank();
		assertThat(jwtTokenProvider.parseToken(secondToken).getId()).isNotBlank();
	}
}
