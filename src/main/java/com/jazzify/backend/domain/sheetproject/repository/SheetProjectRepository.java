package com.jazzify.backend.domain.sheetproject.repository;

import com.jazzify.backend.domain.sheetproject.entity.SheetProject;
import com.jazzify.backend.domain.user.entity.User;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

@NullMarked
public interface SheetProjectRepository extends JpaRepository<SheetProject, Long> {

    Optional<SheetProject> findByPublicIdAndUser(UUID publicId, User user);

    Page<SheetProject> findAllByUser(User user, Pageable pageable);
}
