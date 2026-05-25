package com.jazzify.backend.domain.solo.service.implementation;

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

import com.jazzify.backend.domain.solo.dto.app.SoloMetadataValueCountResult;
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

	public Optional<Solo> findByPublicId(UUID publicId) {
		return soloRepository.findByPublicId(publicId);
	}

	public Page<Solo> getAll(Pageable pageable, @Nullable String composer, @Nullable String performer) {
		return soloRepository.findAllByFilters(normalize(composer), normalize(performer), pageable);
	}

	public List<SoloMetadataValueCountResult> getComposerCounts(@Nullable String performer) {
		return soloRepository.findComposerCounts(normalize(performer));
	}

	public List<SoloMetadataValueCountResult> getPerformerCounts(@Nullable String composer) {
		return soloRepository.findPerformerCounts(normalize(composer));
	}

	public void validateNoDuplicate(String title, @Nullable String performer) {
		if (performer == null || performer.equalsIgnoreCase("unknown")) {
			return;
		}
		if (soloRepository.existsByTitleAndPerformer(title, performer)) {
			throw SoloErrorCode.SOLO_DUPLICATE.toException();
		}
	}

	private static @Nullable String normalize(@Nullable String value) {
		return StringUtils.hasText(value) ? value.trim() : null;
	}
}

