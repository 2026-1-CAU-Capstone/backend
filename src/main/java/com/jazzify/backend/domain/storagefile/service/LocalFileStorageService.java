package com.jazzify.backend.domain.storagefile.service;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@NullMarked
@Service
public class LocalFileStorageService {

    private final Path rootLocation;

    public LocalFileStorageService() {
        this.rootLocation = Path.of(System.getProperty("user.home"), "jazzify");
    }

    public void store(String filePath, byte[] data) throws IOException {
        Path targetPath = rootLocation.resolve(filePath);
        Files.createDirectories(targetPath.getParent());
        Files.write(targetPath, data);
        log.info("Physical file stored: {}", targetPath);
    }

    public void delete(String filePath) {
        try {
            Path targetPath = rootLocation.resolve(filePath);
            boolean deleted = Files.deleteIfExists(targetPath);
            if (deleted) {
                log.info("Physical file deleted: {}", targetPath);
            }
        } catch (IOException e) {
            log.warn("Failed to delete physical file: {}", filePath, e);
        }
    }
}

