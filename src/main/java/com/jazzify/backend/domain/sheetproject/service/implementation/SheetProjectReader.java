package com.jazzify.backend.domain.sheetproject.service.implementation;

import com.jazzify.backend.domain.sheetproject.entity.SheetProject;
import com.jazzify.backend.domain.sheetproject.repository.SheetProjectRepository;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.exception.code.SheetProjectErrorCode;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SheetProjectReader {

    private final SheetProjectRepository sheetProjectRepository;

    public SheetProject getByPublicIdAndUser(UUID publicId, User user) {
        return sheetProjectRepository.findByPublicIdAndUser(publicId, user)
                .orElseThrow(SheetProjectErrorCode.SHEET_PROJECT_NOT_FOUND::toException);
    }

    public Page<SheetProject> getAllByUser(User user, Pageable pageable) {
        return sheetProjectRepository.findAllByUser(user, pageable);
    }
}
