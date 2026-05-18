package com.jazzify.backend.domain.lick.repository;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jazzify.backend.domain.lick.entity.LickMeasure;

@NullMarked
public interface LickMeasureRepository extends JpaRepository<LickMeasure, Long> {

	@Query("""
		select distinct m
		from LickMeasure m
		left join fetch m.notes
		where m.lick.id = :lickId
		order by m.measureIndex asc
		""")
	List<LickMeasure> findAllByLickIdWithNotes(@Param("lickId") Long lickId);
}

