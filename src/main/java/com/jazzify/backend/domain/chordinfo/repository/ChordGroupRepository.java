package com.jazzify.backend.domain.chordinfo.repository;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jazzify.backend.domain.chordinfo.entity.ChordGroup;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;

@NullMarked
public interface ChordGroupRepository extends JpaRepository<ChordGroup, Long> {

	void deleteAllByChordProject(ChordProject chordProject);
}

