package com.jazzify.backend.domain.chordinfo.entity;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.session.entity.Session;
import com.jazzify.backend.domain.sheetproject.entity.SheetProject;
import com.jazzify.backend.shared.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_chord_info")
@Getter
@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChordInfo extends BaseEntity {

	@Column
	private @Nullable String chord;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private ChordLength length;

	@Column(nullable = false)
	private int sortOrder;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sheet_project_id")
	private @Nullable SheetProject sheetProject;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chord_project_id")
	private @Nullable ChordProject chordProject;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "session_id", nullable = false)
	private Session session;

	@Builder
	public ChordInfo(@Nullable String chord, ChordLength length, int sortOrder,
		@Nullable SheetProject sheetProject, @Nullable ChordProject chordProject,
		Session session) {
		this.chord = chord;
		this.length = length;
		this.sortOrder = sortOrder;
		this.sheetProject = sheetProject;
		this.chordProject = chordProject;
		this.session = session;
	}
}

