package com.jazzify.backend.domain.user.service.implementation;

import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.domain.user.repository.UserRepository;
import com.jazzify.backend.domain.user.util.UserMapper;
import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional
public class UserWriter {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public User create(String username, String rawPassword) {
        User user = userMapper.toEntity(username, rawPassword);
        return userRepository.save(user);
    }
}
