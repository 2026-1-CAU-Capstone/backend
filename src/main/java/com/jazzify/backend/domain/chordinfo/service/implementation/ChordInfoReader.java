package com.jazzify.backend.domain.chordinfo.service.implementation;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.chordinfo.repository.ChordInfoRepository;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.sheetproject.entity.SheetProject;

import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChordInfoReader {

	private final ChordInfoRepository chordInfoRepository;

	public List<ChordInfo> getAllByChordProject(ChordProject chordProject) {
		return chordInfoRepository.findAllByChordProjectOrderByBarAscBeatAsc(chordProject);
	}

	public List<ChordInfo> getAllBySheetProject(SheetProject sheetProject) {
		return chordInfoRepository.findAllBySheetProjectOrderByBarAscBeatAsc(sheetProject);
	}
}

