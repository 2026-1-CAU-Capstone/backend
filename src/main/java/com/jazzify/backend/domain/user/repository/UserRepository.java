package com.jazzify.backend.domain.user.repository;

import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jazzify.backend.domain.user.entity.User;

@NullMarked
public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByUsername(String username);

	Optional<User> findByPublicId(UUID publicId);

	boolean existsByUsername(String username);
}
