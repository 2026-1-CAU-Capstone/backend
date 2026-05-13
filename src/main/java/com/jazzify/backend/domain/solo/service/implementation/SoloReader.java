package com.jazzify.backend.domain.solo.service.implementation;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.solo.entity.Solo;
import com.jazzify.backend.domain.solo.repository.SoloRepository;
import com.jazzify.backend.shared.exception.code.SoloErrorCode;

import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SoloReader {

	private final SoloRepository soloRepository;

	public Solo getByPublicId(UUID publicId) {
		return soloRepository.findByPublicId(publicId)
			.orElseThrow(SoloErrorCode.SOLO_NOT_FOUND::toException);
	}

	public Page<Solo> getAll(Pageable pageable) {
		return soloRepository.findAll(pageable);
	}

	public void validateNoDuplicate(String title, @Nullable String performer) {
		if (performer == null || performer.equalsIgnoreCase("unknown")) {
			return;
		}
		if (soloRepository.existsByTitleAndPerformer(title, performer)) {
			throw SoloErrorCode.SOLO_DUPLICATE.toException();
		}
	}
}

