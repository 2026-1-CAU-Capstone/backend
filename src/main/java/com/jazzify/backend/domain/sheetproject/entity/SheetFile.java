package com.jazzify.backend.domain.sheetproject.entity;

import com.jazzify.backend.domain.storagefile.entity.StorageFile;
import com.jazzify.backend.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tb_sheet_file")
@Getter
@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SheetFile extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private FileType fileType;

    @OneToOne(mappedBy = "sheetFile", fetch = FetchType.LAZY)
    private @Nullable SheetProject sheetProject;

    @OneToMany(mappedBy = "sheetFile", fetch = FetchType.LAZY)
    private List<StorageFile> storageFiles = new ArrayList<>();

    @Builder
    public SheetFile(FileType fileType) {
        this.fileType = fileType;
    }
}
