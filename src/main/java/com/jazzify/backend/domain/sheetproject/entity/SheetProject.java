package com.jazzify.backend.domain.sheetproject.entity;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.session.entity.Session;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_sheet_project")
@Getter
@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SheetProject extends BaseEntity {

	@Column(nullable = false)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private @Nullable MusicKey keySignature;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "file_id", nullable = false, unique = true)
	private SheetFile sheetFile;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "session_id")
	private @Nullable Session session;

	@OneToMany(mappedBy = "sheetProject", fetch = FetchType.LAZY)
	private List<ChordInfo> chordInfos = new ArrayList<>();

	@Builder
	public SheetProject(String title, @Nullable MusicKey keySignature, User user,
		SheetFile sheetFile, @Nullable Session session) {
		this.title = title;
		this.keySignature = keySignature;
		this.user = user;
		this.sheetFile = sheetFile;
		this.session = session;
	}

	public void update(String title, @Nullable MusicKey key) {
		this.title = title;
		this.keySignature = key;
	}
}

