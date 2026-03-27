package com.jazzify.backend.domain.storagefile.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.domain.storagefile.dto.response.StorageFileResponse;
import com.jazzify.backend.domain.storagefile.entity.StorageFile;
import com.jazzify.backend.domain.storagefile.event.StorageFileSavedEvent;
import com.jazzify.backend.domain.storagefile.service.implementation.StorageFileReader;
import com.jazzify.backend.domain.storagefile.service.implementation.StorageFileWriter;
import com.jazzify.backend.domain.storagefile.util.StorageFileMapper;
import com.jazzify.backend.shared.exception.code.StorageFileErrorCode;

import lombok.RequiredArgsConstructor;

@NullMarked
@Service
@RequiredArgsConstructor
public class StorageFileService {

	private final StorageFileReader storageFileReader;
	private final StorageFileWriter storageFileWriter;
	private final ApplicationEventPublisher eventPublisher;

	@Transactional
	public StorageFileResponse upload(MultipartFile file) {
		byte[] fileData;
		try {
			fileData = file.getBytes();
		} catch (IOException e) {
			throw StorageFileErrorCode.FILE_UPLOAD_FAILED.toException(e.getMessage());
		}

		String originalFileName = Objects.requireNonNullElse(file.getOriginalFilename(), "unknown");
		String contentType = Objects.requireNonNullElse(file.getContentType(), "application/octet-stream");
		long fileSize = file.getSize();

		String extension = extractExtension(originalFileName);
		String savedFileName = UUID.randomUUID() + extension;
		String datePath = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
		String filePath = datePath + "/" + savedFileName;

		StorageFile storageFile = storageFileWriter.create(
			originalFileName, savedFileName, filePath, fileSize, contentType
		);

		Long storageFileId = Objects.requireNonNull(storageFile.getId(), "id must not be null after persist");
		eventPublisher.publishEvent(new StorageFileSavedEvent(storageFileId, fileData, filePath));

		return StorageFileMapper.toResponse(storageFile);
	}

	@Transactional(readOnly = true)
	public StorageFileResponse getByPublicId(UUID publicId) {
		StorageFile storageFile = storageFileReader.getByPublicId(publicId);
		return StorageFileMapper.toResponse(storageFile);
	}

	private String extractExtension(String fileName) {
		int dotIndex = fileName.lastIndexOf('.');
		if (dotIndex == -1) {
			return "";
		}
		return fileName.substring(dotIndex);
	}
}

