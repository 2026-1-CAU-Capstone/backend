package com.jazzify.backend.domain.sheetproject.service.implementation;

import com.jazzify.backend.domain.sheetproject.entity.FileType;
import com.jazzify.backend.domain.sheetproject.entity.SheetFile;
import com.jazzify.backend.domain.sheetproject.repository.SheetFileRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional
public class SheetFileWriter {

    private final SheetFileRepository sheetFileRepository;

    public SheetFile create(FileType fileType) {
        return sheetFileRepository.save(SheetFile.builder().fileType(fileType).build());
    }

    public void delete(SheetFile sheetFile) {
        sheetFileRepository.delete(sheetFile);
    }
}

