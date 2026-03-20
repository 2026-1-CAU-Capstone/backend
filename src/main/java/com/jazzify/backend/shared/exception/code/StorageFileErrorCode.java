package com.jazzify.backend.shared.exception.code;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

@Getter
@NullMarked
@AllArgsConstructor
public enum StorageFileErrorCode implements BaseErrorCode {

    STORAGE_FILE_NOT_FOUND(HttpStatus.NOT_FOUND, "STORAGE_FILE_001", "저장 파일을 찾을 수 없습니다."),
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_FILE_002", "파일 업로드에 실패했습니다."),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "STORAGE_FILE_003", "파일 삭제에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}

