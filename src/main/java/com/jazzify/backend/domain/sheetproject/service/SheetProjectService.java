package com.jazzify.backend.domain.sheetproject.service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.domain.sheetproject.event.SheetProjectOmrRequestedEvent;
import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectCreateRequest;
import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectOmrCreateRequest;
import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectUpdateRequest;
import com.jazzify.backend.domain.sheetproject.dto.response.SheetProjectOmrCreateResponse;
import com.jazzify.backend.domain.sheetproject.dto.response.SheetProjectOmrStatusResponse;
import com.jazzify.backend.domain.sheetproject.dto.response.SheetProjectResponse;
import com.jazzify.backend.domain.sheetproject.entity.FileType;
import com.jazzify.backend.domain.sheetproject.entity.SheetFile;
import com.jazzify.backend.domain.sheetproject.entity.SheetProject;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetFileWriter;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetProjectOmrWriter;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetProjectReader;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetProjectWriter;
import com.jazzify.backend.domain.sheetproject.util.SheetProjectMapper;
import com.jazzify.backend.domain.storagefile.entity.StorageFile;
import com.jazzify.backend.domain.storagefile.service.implementation.StorageFileReader;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.domain.user.service.implementation.UserReader;
import com.jazzify.backend.shared.exception.code.OmrErrorCode;
import com.jazzify.backend.shared.omr.OmrFileValidator;

import lombok.RequiredArgsConstructor;

@NullMarked
@Service
@RequiredArgsConstructor
public class SheetProjectService {

	private static final String DEFAULT_PENDING_TITLE = "OMR Processing";

	private final SheetProjectReader sheetProjectReader;
	private final SheetProjectWriter sheetProjectWriter;
	private final SheetFileWriter sheetFileWriter;
	private final SheetProjectOmrWriter sheetProjectOmrWriter;
	private final ApplicationEventPublisher eventPublisher;
	private final StorageFileReader storageFileReader;
	private final UserReader userReader;

	@Transactional
	public SheetProjectResponse create(UUID userPublicId, SheetProjectCreateRequest request) {
		User user = userReader.getByPublicId(userPublicId);

		List<StorageFile> storageFiles = request.storageFileIds().stream()
			.map(storageFileReader::getByPublicId)
			.toList();

		FileType fileType = FileType.fromFileName(storageFiles.get(0).getOriginalFileName());
		SheetFile sheetFile = sheetFileWriter.create(fileType);

		storageFiles.forEach(sf -> sf.linkToSheetFile(sheetFile));

		SheetProject project = sheetProjectWriter.create(request.title(), request.key(), user, sheetFile);
		return SheetProjectMapper.toResponse(project);
	}

	@Transactional
	public SheetProjectOmrCreateResponse createFromOmr(
		UUID userPublicId,
		MultipartFile file,
		SheetProjectOmrCreateRequest request
	) {
		OmrFileValidator.validate(file);
		byte[] fileData = readFileBytes(file);
		User user = userReader.getByPublicId(userPublicId);

		String originalFilename = defaultFileName(file.getOriginalFilename());
		String contentType = defaultContentType(file.getContentType());
		String pendingTitle = hasText(request.title())
			? request.title().trim()
			: extractBaseName(originalFilename);

		SheetProject project = sheetProjectOmrWriter.createPending(
			user,
			fileData,
			originalFilename,
			contentType,
			pendingTitle,
			request.key()
		);

		UUID projectPublicId = Objects.requireNonNull(project.getPublicId());
		eventPublisher.publishEvent(new SheetProjectOmrRequestedEvent(
			projectPublicId,
			originalFilename,
			contentType,
			fileData,
			request.title(),
			request.key()
		));

		return new SheetProjectOmrCreateResponse(SheetProjectMapper.toResponse(project), List.of());
	}

	@Transactional(readOnly = true)
	public Page<SheetProjectResponse> getAll(UUID userPublicId, Pageable pageable) {
		User user = userReader.getByPublicId(userPublicId);
		return sheetProjectReader.getAllByUser(user, pageable)
			.map(SheetProjectMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public SheetProjectResponse getByPublicId(UUID userPublicId, UUID projectPublicId) {
		User user = userReader.getByPublicId(userPublicId);
		SheetProject project = sheetProjectReader.getByPublicIdAndUser(projectPublicId, user);
		return SheetProjectMapper.toResponse(project);
	}

	@Transactional(readOnly = true)
	public SheetProjectOmrStatusResponse getOmrStatus(UUID userPublicId, UUID projectPublicId) {
		User user = userReader.getByPublicId(userPublicId);
		SheetProject project = sheetProjectReader.getByPublicIdAndUser(projectPublicId, user);
		return SheetProjectMapper.toOmrStatusResponse(project);
	}

	@Transactional
	public SheetProjectResponse update(UUID userPublicId, UUID projectPublicId, SheetProjectUpdateRequest request) {
		User user = userReader.getByPublicId(userPublicId);
		SheetProject project = sheetProjectReader.getByPublicIdAndUser(projectPublicId, user);
		project.update(request.title(), request.key());
		return SheetProjectMapper.toResponse(project);
	}

	@Transactional
	public void delete(UUID userPublicId, UUID projectPublicId) {
		User user = userReader.getByPublicId(userPublicId);
		SheetProject project = sheetProjectReader.getByPublicIdAndUser(projectPublicId, user);
		sheetProjectWriter.delete(project);
	}

	private static boolean hasText(@Nullable String value) {
		return value != null && !value.isBlank();
	}

	private static String defaultFileName(@Nullable String originalFilename) {
		return hasText(originalFilename) ? Objects.requireNonNull(originalFilename).trim() : "upload.pdf";
	}

	private static String defaultContentType(@Nullable String contentType) {
		return hasText(contentType) ? Objects.requireNonNull(contentType).trim() : "application/octet-stream";
	}

	private static String extractBaseName(String originalFilename) {
		int lastDotIndex = originalFilename.lastIndexOf('.');
		if (lastDotIndex <= 0) {
			return hasText(originalFilename) ? originalFilename : DEFAULT_PENDING_TITLE;
		}
		String baseName = originalFilename.substring(0, lastDotIndex).trim();
		return hasText(baseName) ? baseName : DEFAULT_PENDING_TITLE;
	}

	private static byte[] readFileBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException e) {
			throw OmrErrorCode.OMR_FILE_READ_FAILED.toException(e.getMessage());
		}
	}
}
