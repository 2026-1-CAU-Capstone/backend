package com.jazzify.backend.domain.chordinfo.repository;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.sheetproject.entity.SheetProject;

@NullMarked
public interface ChordInfoRepository extends JpaRepository<ChordInfo, Long> {

	List<ChordInfo> findAllByChordProjectOrderByBarAscBeatAsc(ChordProject chordProject);

	List<ChordInfo> findAllBySheetProjectOrderByBarAscBeatAsc(SheetProject sheetProject);

	void deleteAllByChordProject(ChordProject chordProject);

	void deleteAllBySheetProject(SheetProject sheetProject);
}

