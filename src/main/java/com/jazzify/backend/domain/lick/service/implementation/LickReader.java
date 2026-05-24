package com.jazzify.backend.domain.lick.service.implementation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.jazzify.backend.domain.lick.dto.app.LickMetadataValueCountResult;
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

	public Optional<Lick> findByPublicId(UUID publicId) {
		return lickRepository.findByPublicId(publicId);
	}

	public Page<Lick> getAll(Pageable pageable, @Nullable String composer, @Nullable String performer) {
		return lickRepository.findAllByFilters(normalize(composer), normalize(performer), pageable);
	}

	public List<LickMetadataValueCountResult> getComposerCounts(@Nullable String performer) {
		return lickRepository.findComposerCounts(normalize(performer));
	}

	public List<LickMetadataValueCountResult> getPerformerCounts(@Nullable String composer) {
		return lickRepository.findPerformerCounts(normalize(composer));
	}

	private static @Nullable String normalize(@Nullable String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}

}

