package com.jazzify.backend.domain.storagefile.service.implementation;

import com.jazzify.backend.domain.storagefile.entity.StorageFile;
import com.jazzify.backend.domain.storagefile.repository.StorageFileRepository;
import com.jazzify.backend.shared.exception.code.StorageFileErrorCode;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StorageFileReader {

    private final StorageFileRepository storageFileRepository;

    public StorageFile getByPublicId(UUID publicId) {
        return storageFileRepository.findByPublicId(publicId)
                .orElseThrow(StorageFileErrorCode.STORAGE_FILE_NOT_FOUND::toException);
    }

    public List<StorageFile> findOrphans(LocalDateTime threshold) {
        return storageFileRepository.findOrphans(threshold);
    }
}

