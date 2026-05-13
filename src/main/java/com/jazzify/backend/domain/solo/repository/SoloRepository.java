package com.jazzify.backend.domain.solo.repository;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jazzify.backend.domain.solo.entity.Solo;

@NullMarked
public interface SoloRepository extends JpaRepository<Solo, Long> {

	Optional<Solo> findByPublicId(UUID publicId);

	boolean existsByTitleAndPerformer(String title, @Nullable String performer);
}

