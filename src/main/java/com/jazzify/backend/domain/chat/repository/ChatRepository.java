package com.jazzify.backend.domain.chat.repository;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jazzify.backend.domain.chat.entity.Chat;

@NullMarked
public interface ChatRepository extends JpaRepository<Chat, Long> {

	Optional<Chat> findByPublicId(UUID publicId);

	Optional<Chat> findByPublicIdAndUser_PublicId(UUID publicId, UUID userPublicId);

	Page<Chat> findAllByUser_PublicId(UUID userPublicId, Pageable pageable);
}

