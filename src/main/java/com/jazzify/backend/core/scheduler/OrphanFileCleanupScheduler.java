package com.jazzify.backend.core.scheduler;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.jazzify.backend.domain.storagefile.service.LocalFileStorageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
public class OrphanFileCleanupScheduler {

	private final OrphanFileCleanupService cleanupService;
	private final LocalFileStorageService localFileStorageService;

	@Scheduled(fixedRate = 3600000) // 1시간
	public void cleanup() {
		log.info("Orphan file cleanup started");

		List<String> filePathsToDelete = cleanupService.deleteOrphanRecords();

		for (String filePath : filePathsToDelete) {
			localFileStorageService.delete(filePath);
		}

		log.info("Orphan file cleanup finished. {} physical file(s) deleted", filePathsToDelete.size());
	}
}

