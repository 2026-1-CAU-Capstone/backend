package com.jazzify.backend.domain.chordproject.service.implementation;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.chordproject.repository.ChordProjectRepository;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.exception.code.ChordProjectErrorCode;

import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChordProjectReader {

	private final ChordProjectRepository chordProjectRepository;

	public ChordProject getByPublicIdAndUser(UUID publicId, User user) {
		return chordProjectRepository.findByPublicIdAndUser(publicId, user)
			.orElseThrow(ChordProjectErrorCode.CHORD_PROJECT_NOT_FOUND::toException);
	}

	public Page<ChordProject> getAllByUser(User user, Pageable pageable) {
		return chordProjectRepository.findAllByUser(user, pageable);
	}
}
