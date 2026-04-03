package com.jazzify.backend.domain.chordinfo.repository;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jazzify.backend.domain.chordinfo.entity.ChordGroup;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;

@NullMarked
public interface ChordGroupRepository extends JpaRepository<ChordGroup, Long> {

	List<ChordGroup> findAllByChordProjectOrderByGroupIndexAsc(ChordProject chordProject);

	void deleteAllByChordProject(ChordProject chordProject);
}

