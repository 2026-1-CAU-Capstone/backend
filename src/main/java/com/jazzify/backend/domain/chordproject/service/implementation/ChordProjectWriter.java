package com.jazzify.backend.domain.chordproject.service.implementation;

import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.chordproject.repository.ChordProjectRepository;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.domain.MusicKey;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional
public class ChordProjectWriter {

    private final ChordProjectRepository chordProjectRepository;

    public ChordProject create(String title, MusicKey key, User user) {
        ChordProject project = ChordProject.builder()
                .title(title)
                .keySignature(key)
                .user(user)
                .build();
        return chordProjectRepository.save(project);
    }

    public void delete(ChordProject project) {
        chordProjectRepository.delete(project);
    }
}

