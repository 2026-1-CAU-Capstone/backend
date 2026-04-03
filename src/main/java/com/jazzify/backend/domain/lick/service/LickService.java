package com.jazzify.backend.domain.lick.service;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.lick.dto.request.LickCreateRequest;
import com.jazzify.backend.domain.lick.dto.request.LickUpdateRequest;
import com.jazzify.backend.domain.lick.dto.response.LickResponse;
import com.jazzify.backend.domain.lick.entity.Lick;
import com.jazzify.backend.domain.lick.service.implementation.LickReader;
import com.jazzify.backend.domain.lick.service.implementation.LickWriter;
import com.jazzify.backend.domain.lick.util.LickMapper;

import lombok.RequiredArgsConstructor;

@NullMarked
@Service
@RequiredArgsConstructor
public class LickService {

	private final LickReader lickReader;
	private final LickWriter lickWriter;

	@Transactional
	public LickResponse create(LickCreateRequest request) {
		Lick lick = lickWriter.create(request.title(), request.composer(), request.contents());
		return LickMapper.toResponse(lick);
	}

	@Transactional(readOnly = true)
	public Page<LickResponse> getAll(Pageable pageable) {
		return lickReader.getAll(pageable)
			.map(LickMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public LickResponse getByPublicId(UUID publicId) {
		Lick lick = lickReader.getByPublicId(publicId);
		return LickMapper.toResponse(lick);
	}

	@Transactional
	public LickResponse update(UUID publicId, LickUpdateRequest request) {
		Lick lick = lickReader.getByPublicId(publicId);
		lick.update(request.title(), request.composer(), request.contents());
		return LickMapper.toResponse(lick);
	}

	@Transactional
	public void delete(UUID publicId) {
		Lick lick = lickReader.getByPublicId(publicId);
		lickWriter.delete(lick);
	}
}

