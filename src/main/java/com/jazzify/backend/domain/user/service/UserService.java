package com.jazzify.backend.domain.user.service;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.user.dto.response.UserProfileResponse;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.domain.user.service.implementation.UserReader;
import com.jazzify.backend.domain.user.util.UserMapper;

import lombok.RequiredArgsConstructor;

@NullMarked
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserReader userReader;

	/**
	 * 현재 인증된 사용자의 프로필 정보를 조회한다.
	 *
	 * @param publicId JWT에서 추출한 사용자 publicId
	 * @return 사용자 프로필 응답 DTO
	 */
	@Transactional(readOnly = true)
	public UserProfileResponse getMe(UUID publicId) {
		User user = userReader.getByPublicId(publicId);
		return UserMapper.toUserProfileResponse(user);
	}
}

