package com.jazzify.backend.domain.storagefile.entity;

import com.jazzify.backend.domain.sheetproject.entity.SheetFile;
import com.jazzify.backend.shared.persistence.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "tb_storage_file")
@Getter
@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StorageFile extends BaseEntity {

    @Column(nullable = false)
    private String originalFileName;

    @Column(nullable = false, unique = true)
    private String savedFileName;

    @Column(nullable = false)
    private String filePath;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false)
    private String contentType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sheet_file_id")
    private @Nullable SheetFile sheetFile;

    @Builder
    public StorageFile(String originalFileName, String savedFileName,
                       String filePath, long fileSize, String contentType) {
        this.originalFileName = originalFileName;
        this.savedFileName = savedFileName;
        this.filePath = filePath;
        this.fileSize = fileSize;
        this.contentType = contentType;
    }

    public void linkToSheetFile(SheetFile sheetFile) {
        this.sheetFile = sheetFile;
    }
}

