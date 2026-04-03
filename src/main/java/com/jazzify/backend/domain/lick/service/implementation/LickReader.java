package com.jazzify.backend.domain.lick.service.implementation;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.lick.entity.Lick;
import com.jazzify.backend.domain.lick.repository.LickRepository;
import com.jazzify.backend.shared.exception.code.LickErrorCode;

import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LickReader {

	private final LickRepository lickRepository;

	public Lick getByPublicId(UUID publicId) {
		return lickRepository.findByPublicId(publicId)
			.orElseThrow(LickErrorCode.LICK_NOT_FOUND::toException);
	}

	public Page<Lick> getAll(Pageable pageable) {
		return lickRepository.findAll(pageable);
	}
}

