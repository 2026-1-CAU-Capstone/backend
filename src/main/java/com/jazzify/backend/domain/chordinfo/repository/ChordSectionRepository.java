package com.jazzify.backend.domain.chordinfo.repository;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jazzify.backend.domain.chordinfo.entity.ChordSection;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;

@NullMarked
public interface ChordSectionRepository extends JpaRepository<ChordSection, Long> {

	void deleteAllByChordProject(ChordProject chordProject);
}

