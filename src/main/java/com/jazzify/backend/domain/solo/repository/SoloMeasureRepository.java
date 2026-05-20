package com.jazzify.backend.domain.solo.repository;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jazzify.backend.domain.solo.entity.SoloMeasure;

@NullMarked
public interface SoloMeasureRepository extends JpaRepository<SoloMeasure, Long> {

	@Query("""
		select distinct m
		from SoloMeasure m
		left join fetch m.notes
		where m.solo.id = :soloId
		order by m.measureIndex asc
		""")
	List<SoloMeasure> findAllBySoloIdWithNotes(@Param("soloId") Long soloId);
}

