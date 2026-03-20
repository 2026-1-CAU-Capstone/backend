package com.jazzify.backend.domain.storagefile.service.implementation;

import com.jazzify.backend.domain.storagefile.entity.StorageFile;
import com.jazzify.backend.domain.storagefile.repository.StorageFileRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional
public class StorageFileWriter {

    private final StorageFileRepository storageFileRepository;

    public StorageFile create(String originalFileName, String savedFileName,
                              String filePath, long fileSize, String contentType) {
        StorageFile storageFile = StorageFile.builder()
                .originalFileName(originalFileName)
                .savedFileName(savedFileName)
                .filePath(filePath)
                .fileSize(fileSize)
                .contentType(contentType)
                .build();
        return storageFileRepository.save(storageFile);
    }

    public void delete(StorageFile storageFile) {
        storageFileRepository.delete(storageFile);
    }

    public void deleteById(Long id) {
        storageFileRepository.deleteById(id);
    }

    public void deleteAll(List<StorageFile> storageFiles) {
        storageFileRepository.deleteAll(storageFiles);
    }
}

