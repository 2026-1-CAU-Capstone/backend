package com.jazzify.backend.domain.lick.repository;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jazzify.backend.domain.lick.entity.Lick;

@NullMarked
public interface LickRepository extends JpaRepository<Lick, Long> {

	Optional<Lick> findByPublicId(UUID publicId);
}

