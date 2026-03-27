package com.jazzify.backend.domain.session.entity;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.sheetproject.entity.SheetProject;
import com.jazzify.backend.shared.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_session")
@Getter
@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Session extends BaseEntity {

	@Column
	private @Nullable String title;

	@OneToMany(mappedBy = "session", fetch = FetchType.LAZY)
	private List<SheetProject> sheetProjects = new ArrayList<>();

	@OneToMany(mappedBy = "session", fetch = FetchType.LAZY)
	private List<ChordProject> chordProjects = new ArrayList<>();

	@OneToMany(mappedBy = "session", fetch = FetchType.LAZY)
	private List<ChordInfo> chordInfos = new ArrayList<>();

	@OneToMany(mappedBy = "session", fetch = FetchType.LAZY)
	private List<SessionMessage> sessionMessages = new ArrayList<>();

	@Builder
	public Session(@Nullable String title) {
		this.title = title;
	}
}
