package com.jazzify.backend.domain.chordinfo.entity;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.session.entity.Session;
import com.jazzify.backend.domain.sheetproject.entity.SheetProject;
import com.jazzify.backend.shared.persistence.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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

	// ── 위치 및 원본 ──
	@Column
	private @Nullable String chord;

	@Column(nullable = false)
	private int bar;

	@Column(nullable = false)
	private double beat;

	@Column(nullable = false)
	private double durationBeats;

	// ── 1:1 분석 결과 ──
	@OneToOne(mappedBy = "chordInfo", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private @Nullable ChordAnalysis analysis;

	// ── 연관 관계 ──
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "sheet_project_id")
	private @Nullable SheetProject sheetProject;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chord_project_id")
	private @Nullable ChordProject chordProject;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "session_id")
	private @Nullable Session session;

	@Builder
	public ChordInfo(@Nullable String chord, int bar, double beat, double durationBeats,
		@Nullable SheetProject sheetProject, @Nullable ChordProject chordProject,
		@Nullable Session session) {
		this.chord = chord;
		this.bar = bar;
		this.beat = beat;
		this.durationBeats = durationBeats;
		this.sheetProject = sheetProject;
		this.chordProject = chordProject;
		this.session = session;
	}

	public void assignAnalysis(ChordAnalysis analysis) {
		this.analysis = analysis;
	}
}

