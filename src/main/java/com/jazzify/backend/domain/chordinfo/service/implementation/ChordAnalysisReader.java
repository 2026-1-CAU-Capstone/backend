package com.jazzify.backend.domain.chordinfo.service.implementation;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.chordinfo.entity.ChordGroup;
import com.jazzify.backend.domain.chordinfo.entity.ChordSection;
import com.jazzify.backend.domain.chordinfo.repository.ChordGroupRepository;
import com.jazzify.backend.domain.chordinfo.repository.ChordSectionRepository;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;

import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChordAnalysisReader {

	private final ChordGroupRepository chordGroupRepository;
	private final ChordSectionRepository chordSectionRepository;

	public List<ChordGroup> getGroupsByProject(ChordProject project) {
		return chordGroupRepository.findAllByChordProjectOrderByGroupIndexAsc(project);
	}

	public List<ChordSection> getSectionsByProject(ChordProject project) {
		return chordSectionRepository.findAllByChordProjectOrderByStartBarAsc(project);
	}
}

