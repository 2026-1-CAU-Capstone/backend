package com.jazzify.backend.domain.user.entity;

import org.jspecify.annotations.NullMarked;

/**
 * 사용자 권한 역할 열거형.
 * <ul>
 *   <li>ADMIN  – 전체 관리 권한 (Lick/Solo CRUD 포함, 모든 도메인 접근 가능)</li>
 *   <li>MANAGE – 콘텐츠 관리 권한 (Lick/Solo CRUD 가능)</li>
 *   <li>MEMBER – 일반 회원 권한 (조회 전용)</li>
 * </ul>
 */
@NullMarked
public enum UserRole {
	ADMIN,
	MANAGE,
	MEMBER
}

