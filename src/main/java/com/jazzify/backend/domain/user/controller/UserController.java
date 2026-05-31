package com.jazzify.backend.domain.user.controller;

import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.user.dto.response.UserProfileResponse;
import com.jazzify.backend.domain.user.service.UserService;
import com.jazzify.backend.shared.web.ApiResponse;

import lombok.RequiredArgsConstructor;

@NullMarked
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class UserController implements UserControllerSpec {

	private final UserService userService;

	@Override
	@GetMapping("/me")
	public ApiResponse<UserProfileResponse> getMe(@AuthenticationPrincipal CustomPrincipal principal) {
		return ApiResponse.ok(userService.getMe(principal.publicId()));
	}
}

