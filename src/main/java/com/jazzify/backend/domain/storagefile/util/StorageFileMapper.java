package com.jazzify.backend.domain.storagefile.util;

import com.jazzify.backend.domain.storagefile.dto.response.StorageFileResponse;
import com.jazzify.backend.domain.storagefile.entity.StorageFile;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;

import java.util.Objects;

@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StorageFileMapper {

    public static StorageFileResponse toResponse(StorageFile storageFile) {
        return new StorageFileResponse(
                Objects.requireNonNull(storageFile.getPublicId()),
                storageFile.getOriginalFileName(),
                storageFile.getFileSize(),
                storageFile.getContentType(),
                Objects.requireNonNull(storageFile.getCreatedAt())
        );
    }
}

