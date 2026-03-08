package com.jazzify.backend.shared.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

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
