package com.jazzify.backend.domain.storagefile.event;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.jazzify.backend.domain.storagefile.service.LocalFileStorageService;
import com.jazzify.backend.domain.storagefile.service.implementation.StorageFileWriter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
public class StorageFileEventListener {

	private final LocalFileStorageService localFileStorageService;
	private final StorageFileWriter storageFileWriter;

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handleStorageFileSaved(StorageFileSavedEvent event) {
		try {
			localFileStorageService.store(event.filePath(), event.fileData());
		} catch (Exception e) {
			log.error("Physical file storage failed, executing compensating transaction: {}", event.filePath(), e);
			storageFileWriter.deleteById(event.storageFileId());
		}
	}
}

