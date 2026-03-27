package com.jazzify.backend.domain.sheetproject.service;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectCreateRequest;
import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectUpdateRequest;
import com.jazzify.backend.domain.sheetproject.dto.response.SheetProjectResponse;
import com.jazzify.backend.domain.sheetproject.entity.FileType;
import com.jazzify.backend.domain.sheetproject.entity.SheetFile;
import com.jazzify.backend.domain.sheetproject.entity.SheetProject;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetFileWriter;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetProjectReader;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetProjectWriter;
import com.jazzify.backend.domain.sheetproject.util.SheetProjectMapper;
import com.jazzify.backend.domain.storagefile.entity.StorageFile;
import com.jazzify.backend.domain.storagefile.service.implementation.StorageFileReader;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.domain.user.service.implementation.UserReader;

import lombok.RequiredArgsConstructor;

@NullMarked
@Service
@RequiredArgsConstructor
public class SheetProjectService {

	private final SheetProjectReader sheetProjectReader;
	private final SheetProjectWriter sheetProjectWriter;
	private final SheetFileWriter sheetFileWriter;
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
}
