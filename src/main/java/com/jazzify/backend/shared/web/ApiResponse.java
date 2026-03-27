package com.jazzify.backend.shared.web;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import lombok.Getter;

@Getter
@NullMarked
public class ApiResponse<T> {

	private final @Nullable T data;

	private ApiResponse(@Nullable T data) {
		this.data = data;
	}

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(data);
	}

	public static <T> ApiResponse<T> ok() {
		return new ApiResponse<>(null);
	}
}
