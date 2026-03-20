package com.jazzify.backend.domain.chordproject.repository;

import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.user.entity.User;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface ChordProjectRepository extends JpaRepository<ChordProject, Long> {

    Optional<ChordProject> findByPublicIdAndUser(UUID publicId, User user);

    Page<ChordProject> findAllByUser(User user, Pageable pageable);
}
