package com.jazzify.backend.domain.user.service.implementation;

import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.exception.code.UserErrorCode;
import com.jazzify.backend.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserReader {

    private final UserRepository userRepository;

    public User getByPublicId(UUID publicId) {
        return userRepository.findByPublicId(publicId)
                .orElseThrow(UserErrorCode.USER_NOT_FOUND::toException);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(UserErrorCode.USER_NOT_FOUND::toException);
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }
}

