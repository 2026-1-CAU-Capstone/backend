package com.jazzify.backend.domain.user.controller;

import org.jspecify.annotations.NullMarked;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.user.dto.response.UserProfileResponse;
import com.jazzify.backend.shared.web.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@NullMarked
@Tag(name = "User", description = "사용자 프로필 API")
public interface UserControllerSpec {

	@Operation(
		summary = "내 프로필 조회",
		description = """
			현재 로그인한 사용자의 프로필 정보를 DB에서 조회하여 반환합니다.
			
			### 응답 필드
			
			| 필드 | 타입 | 설명 |
			|------|------|------|
			| `publicId` | UUID | 사용자 외부 식별자 |
			| `name` | string | 이름 |
			| `username` | string | 로그인 아이디 |
			| `role` | string | 권한 역할: `ADMIN` · `MANAGE` · `MEMBER` |
			| `createdAt` | datetime | 가입 일시 |
			| `updatedAt` | datetime | 최근 수정 일시 |
			
			### 에러
			- `401`: 인증 토큰이 없거나 만료된 경우
			- `404 USER_001`: 사용자를 찾을 수 없는 경우 (토큰 유효하나 계정 삭제 등)
			""",
		security = @SecurityRequirement(name = "BearerAuth")
	)
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "프로필 조회 성공",
			content = @Content(schema = @Schema(implementation = UserProfileResponse.class))
		)
	})
	ApiResponse<UserProfileResponse> getMe(@AuthenticationPrincipal CustomPrincipal principal);
}

