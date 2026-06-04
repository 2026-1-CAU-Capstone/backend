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
@Transactional
public class ChordInfoWriter {

	private final ChordInfoRepository chordInfoRepository;

	public List<ChordInfo> saveAll(List<ChordInfo> chordInfos) {
		ensureSortOrder(chordInfos);
		return chordInfoRepository.saveAll(chordInfos);
	}

	public void deleteAllByChordProject(ChordProject chordProject) {
		chordInfoRepository.deleteAllByChordProject(chordProject);
	}

	public void deleteAllBySheetProject(SheetProject sheetProject) {
		chordInfoRepository.deleteAllBySheetProject(sheetProject);
	}

	private static void ensureSortOrder(List<ChordInfo> chordInfos) {
		for (int i = 0; i < chordInfos.size(); i++) {
			ChordInfo chordInfo = chordInfos.get(i);
			if (chordInfo.getSortOrder() <= 0) {
				chordInfo.updateSortOrder(i + 1);
			}
		}
	}
}

