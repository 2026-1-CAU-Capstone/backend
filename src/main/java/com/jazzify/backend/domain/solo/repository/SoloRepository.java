package com.jazzify.backend.domain.solo.repository;

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

import com.jazzify.backend.domain.solo.dto.app.SoloMetadataValueCountResult;
import com.jazzify.backend.domain.solo.entity.Solo;

@NullMarked
public interface SoloRepository extends JpaRepository<Solo, Long> {

	Optional<Solo> findByPublicId(UUID publicId);

	@Query("""
		select s
		from Solo s
		where (:composer is null or lower(s.composer) = lower(:composer))
		  and (:performer is null or lower(s.performer) = lower(:performer))
		""")
	Page<Solo> findAllByFilters(
		@Param("composer") @Nullable String composer,
		@Param("performer") @Nullable String performer,
		Pageable pageable);

	@Query("""
		select s.id
		from Solo s
		where s.sheetDataJson is null
		order by s.id asc
		""")
	Page<Long> findIdsWithMissingSheetDataJson(Pageable pageable);

	@Query("""
		select new com.jazzify.backend.domain.solo.dto.app.SoloMetadataValueCountResult(s.composer, count(s))
		from Solo s
		where s.composer is not null
		  and length(trim(s.composer)) > 0
		  and (:performer is null or lower(s.performer) = lower(:performer))
		group by s.composer
		order by count(s) desc, s.composer asc
		""")
	List<SoloMetadataValueCountResult> findComposerCounts(@Param("performer") @Nullable String performer);

	@Query("""
		select new com.jazzify.backend.domain.solo.dto.app.SoloMetadataValueCountResult(s.performer, count(s))
		from Solo s
		where s.performer is not null
		  and length(trim(s.performer)) > 0
		  and (:composer is null or lower(s.composer) = lower(:composer))
		group by s.performer
		order by count(s) desc, s.performer asc
		""")
	List<SoloMetadataValueCountResult> findPerformerCounts(@Param("composer") @Nullable String composer);

	boolean existsByTitleAndPerformer(String title, @Nullable String performer);
}

