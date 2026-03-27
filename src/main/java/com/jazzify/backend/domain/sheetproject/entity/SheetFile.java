package com.jazzify.backend.domain.sheetproject.entity;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.domain.storagefile.entity.StorageFile;
import com.jazzify.backend.shared.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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
