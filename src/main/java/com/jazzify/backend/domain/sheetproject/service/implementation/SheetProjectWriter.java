package com.jazzify.backend.domain.sheetproject.service.implementation;

import com.jazzify.backend.domain.sheetproject.entity.SheetFile;
import com.jazzify.backend.domain.sheetproject.entity.SheetProject;
import com.jazzify.backend.domain.sheetproject.repository.SheetProjectRepository;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.domain.MusicKey;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional
public class SheetProjectWriter {

    private final SheetProjectRepository sheetProjectRepository;

    public SheetProject create(String title, @Nullable MusicKey key, User user, SheetFile sheetFile) {
        SheetProject project = SheetProject.builder()
                .title(title)
                .key(key)
                .user(user)
                .sheetFile(sheetFile)
                .build();
        return sheetProjectRepository.save(project);
    }

    public void delete(SheetProject project) {
        sheetProjectRepository.delete(project);
    }
}

