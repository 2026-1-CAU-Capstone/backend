package com.jazzify.backend.domain.lick.service.implementation;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.lick.entity.Lick;
import com.jazzify.backend.domain.lick.repository.LickRepository;

import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional
public class LickWriter {

	private final LickRepository lickRepository;

	public Lick create(String title, @Nullable String composer, String content) {
		Lick lick = Lick.builder()
			.title(title)
			.composer(composer)
			.content(content)
			.build();
		return lickRepository.save(lick);
	}

	public void delete(Lick lick) {
		lickRepository.delete(lick);
	}
}

