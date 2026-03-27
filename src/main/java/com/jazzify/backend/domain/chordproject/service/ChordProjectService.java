package com.jazzify.backend.domain.chordproject.service;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.chordproject.dto.request.ChordProjectCreateRequest;
import com.jazzify.backend.domain.chordproject.dto.request.ChordProjectUpdateRequest;
import com.jazzify.backend.domain.chordproject.dto.response.ChordProjectResponse;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectReader;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectWriter;
import com.jazzify.backend.domain.chordproject.util.ChordProjectMapper;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.domain.user.service.implementation.UserReader;

import lombok.RequiredArgsConstructor;

@NullMarked
@Service
@RequiredArgsConstructor
public class ChordProjectService {

	private final ChordProjectReader chordProjectReader;
	private final ChordProjectWriter chordProjectWriter;
	private final UserReader userReader;

	@Transactional
	public ChordProjectResponse create(UUID userPublicId, ChordProjectCreateRequest request) {
		User user = userReader.getByPublicId(userPublicId);
		ChordProject project = chordProjectWriter.create(request.title(), request.key(), user);
		return ChordProjectMapper.toResponse(project);
	}

	@Transactional(readOnly = true)
	public Page<ChordProjectResponse> getAll(UUID userPublicId, Pageable pageable) {
		User user = userReader.getByPublicId(userPublicId);
		return chordProjectReader.getAllByUser(user, pageable)
			.map(ChordProjectMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public ChordProjectResponse getByPublicId(UUID userPublicId, UUID projectPublicId) {
		User user = userReader.getByPublicId(userPublicId);
		ChordProject project = chordProjectReader.getByPublicIdAndUser(projectPublicId, user);
		return ChordProjectMapper.toResponse(project);
	}

	@Transactional
	public ChordProjectResponse update(UUID userPublicId, UUID projectPublicId, ChordProjectUpdateRequest request) {
		User user = userReader.getByPublicId(userPublicId);
		ChordProject project = chordProjectReader.getByPublicIdAndUser(projectPublicId, user);
		project.update(request.title(), request.key());
		return ChordProjectMapper.toResponse(project);
	}

	@Transactional
	public void delete(UUID userPublicId, UUID projectPublicId) {
		User user = userReader.getByPublicId(userPublicId);
		ChordProject project = chordProjectReader.getByPublicIdAndUser(projectPublicId, user);
		chordProjectWriter.delete(project);
	}
}
