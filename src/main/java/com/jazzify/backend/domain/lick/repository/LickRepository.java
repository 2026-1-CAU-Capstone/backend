package com.jazzify.backend.domain.lick.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jazzify.backend.domain.lick.dto.app.LickMetadataValueCountResult;
import com.jazzify.backend.domain.lick.entity.Lick;

@NullMarked
public interface LickRepository extends JpaRepository<Lick, Long> {

	Optional<Lick> findByPublicId(UUID publicId);

	@Query("""
		select l
		from Lick l
		where (:composer is null or lower(l.composer) = lower(:composer))
		  and (:performer is null or lower(l.performer) = lower(:performer))
		""")
	Page<Lick> findAllByFilters(
		@Param("composer") @Nullable String composer,
		@Param("performer") @Nullable String performer,
		Pageable pageable);

	@Query("""
		select l.id
		from Lick l
		where l.sheetDataJson is null
		order by l.id asc
		""")
	Page<Long> findIdsWithMissingSheetDataJson(Pageable pageable);

	@Query("""
		select new com.jazzify.backend.domain.lick.dto.app.LickMetadataValueCountResult(l.composer, count(l))
		from Lick l
		where l.composer is not null
		  and length(trim(l.composer)) > 0
		  and (:performer is null or lower(l.performer) = lower(:performer))
		group by l.composer
		order by count(l) desc, l.composer asc
		""")
	List<LickMetadataValueCountResult> findComposerCounts(@Param("performer") @Nullable String performer);

	@Query("""
		select new com.jazzify.backend.domain.lick.dto.app.LickMetadataValueCountResult(l.performer, count(l))
		from Lick l
		where l.performer is not null
		  and length(trim(l.performer)) > 0
		  and (:composer is null or lower(l.composer) = lower(:composer))
		group by l.performer
		order by count(l) desc, l.performer asc
		""")
	List<LickMetadataValueCountResult> findPerformerCounts(@Param("composer") @Nullable String composer);

	boolean existsByTitleAndPerformer(String title, @Nullable String performer);
}

