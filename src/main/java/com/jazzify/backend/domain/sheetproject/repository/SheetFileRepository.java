package com.jazzify.backend.domain.sheetproject.repository;

import com.jazzify.backend.domain.sheetproject.entity.SheetFile;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface SheetFileRepository extends JpaRepository<SheetFile, Long> {

    Optional<SheetFile> findByPublicId(UUID publicId);

    @Query("SELECT sf FROM SheetFile sf WHERE sf.createdAt < :threshold " +
           "AND NOT EXISTS (SELECT sp FROM SheetProject sp WHERE sp.sheetFile = sf)")
    List<SheetFile> findOrphans(@Param("threshold") LocalDateTime threshold);
}
