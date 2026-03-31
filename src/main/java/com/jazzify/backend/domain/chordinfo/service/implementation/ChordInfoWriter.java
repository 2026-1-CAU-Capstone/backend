package com.jazzify.backend.domain.chordinfo.service.implementation;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.chordinfo.repository.ChordInfoRepository;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;

import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional
public class ChordInfoWriter {

	private final ChordInfoRepository chordInfoRepository;

	public List<ChordInfo> saveAll(List<ChordInfo> chordInfos) {
		return chordInfoRepository.saveAll(chordInfos);
	}

	public void deleteAllByChordProject(ChordProject chordProject) {
		chordInfoRepository.deleteAllByChordProject(chordProject);
	}
}

