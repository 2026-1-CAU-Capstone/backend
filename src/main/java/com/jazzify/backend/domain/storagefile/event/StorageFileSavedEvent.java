package com.jazzify.backend.domain.storagefile.event;

import org.jspecify.annotations.NullMarked;

@NullMarked
public record StorageFileSavedEvent(
	Long storageFileId,
	byte[] fileData,
	String filePath
) {
}

