package com.jazzify.backend.core.scheduler;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.sheetproject.entity.SheetFile;
import com.jazzify.backend.domain.sheetproject.repository.SheetFileRepository;
import com.jazzify.backend.domain.storagefile.entity.StorageFile;
import com.jazzify.backend.domain.storagefile.repository.StorageFileRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NullMarked
@Service
@RequiredArgsConstructor
public class OrphanFileCleanupService {

	private final SheetFileRepository sheetFileRepository;
	private final StorageFileRepository storageFileRepository;

	@Transactional
	public List<String> deleteOrphanRecords() {
		LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
		List<String> filePathsToDelete = new ArrayList<>();

		// 1. SheetProject에 연결되지 않은 고아 SheetFile 삭제
		List<SheetFile> orphanSheetFiles = sheetFileRepository.findOrphans(threshold);
		for (SheetFile sheetFile : orphanSheetFiles) {
			for (StorageFile storageFile : sheetFile.getStorageFiles()) {
				filePathsToDelete.add(storageFile.getFilePath());
			}
			storageFileRepository.deleteAll(sheetFile.getStorageFiles());
			sheetFileRepository.delete(sheetFile);
		}
		if (!orphanSheetFiles.isEmpty()) {
			log.info("Deleted {} orphan SheetFile(s)", orphanSheetFiles.size());
		}

		// 2. SheetFile에 연결되지 않은 고아 StorageFile 삭제
		List<StorageFile> orphanStorageFiles = storageFileRepository.findOrphans(threshold);
		for (StorageFile storageFile : orphanStorageFiles) {
			filePathsToDelete.add(storageFile.getFilePath());
		}
		storageFileRepository.deleteAll(orphanStorageFiles);
		if (!orphanStorageFiles.isEmpty()) {
			log.info("Deleted {} orphan StorageFile(s)", orphanStorageFiles.size());
		}

		return filePathsToDelete;
	}
}

