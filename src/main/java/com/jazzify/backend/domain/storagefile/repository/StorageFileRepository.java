package com.jazzify.backend.domain.storagefile.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.jazzify.backend.domain.storagefile.entity.StorageFile;

@NullMarked
public interface StorageFileRepository extends JpaRepository<StorageFile, Long> {

	Optional<StorageFile> findByPublicId(UUID publicId);

	@Query("SELECT sf FROM StorageFile sf WHERE sf.sheetFile IS NULL AND sf.createdAt < :threshold")
	List<StorageFile> findOrphans(@Param("threshold") LocalDateTime threshold);
}

