package com.jazzify.backend.domain.lick.service.implementation;

import org.jspecify.annotations.NullMarked;
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

	public Lick create(String title, String contents) {
		Lick lick = Lick.builder()
			.title(title)
			.contents(contents)
			.build();
		return lickRepository.save(lick);
	}

	public void delete(Lick lick) {
		lickRepository.delete(lick);
	}
}

