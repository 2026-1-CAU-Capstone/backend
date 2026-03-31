package com.jazzify.backend.domain.chordinfo.repository;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jazzify.backend.domain.chordinfo.entity.ChordAnalysis;

@NullMarked
public interface ChordAnalysisRepository extends JpaRepository<ChordAnalysis, Long> {
}

