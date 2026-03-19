package com.jazzify.backend.domain.sheetproject.entity;

import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.shared.persistence.BaseEntity;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_sheet_file")
@Getter
@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SheetFile extends BaseEntity {

    @Column
    private @Nullable String originalFileName;

    @Column
    private @Nullable String savedFileName;

    @Column
    private @Nullable String filePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileType fileType;

    @OneToMany(mappedBy = "sheetFile", fetch = FetchType.LAZY)
    private List<SheetProject> sheetProjects = new ArrayList<>();

    @Builder
    public SheetFile(@Nullable String originalFileName, @Nullable String savedFileName,
                     @Nullable String filePath, FileType fileType) {
        this.originalFileName = originalFileName;
        this.savedFileName = savedFileName;
        this.filePath = filePath;
        this.fileType = fileType;
    }
}

