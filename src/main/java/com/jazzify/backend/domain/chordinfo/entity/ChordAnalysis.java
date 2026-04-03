package com.jazzify.backend.domain.chordinfo.entity;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.shared.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_chord_analysis")
@Getter
@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChordAnalysis extends BaseEntity {

	// ── Layer 1: 개별 코드 분석 ──
	@Column(length = 30)
	private @Nullable String degree;

	@Column
	private @Nullable Boolean isDiatonic;

	@Column(length = 30)
	private @Nullable String normalizedQuality;

	@Column(columnDefinition = "JSON")
	private @Nullable String functions;

	// ── Layer 2: 문맥 패턴 ──
	@Column(columnDefinition = "JSON")
	private @Nullable String secondaryDominant;

	@Column(length = 30)
	private @Nullable String diminishedFunction;

	@Column(columnDefinition = "JSON")
	private @Nullable String chromaticApproach;

	@Column(columnDefinition = "JSON")
	private @Nullable String deceptiveResolution;

	@Column(columnDefinition = "JSON")
	private @Nullable String pedalInfo;

	// ── Layer 3: 구조 분석 ──
	@Column(columnDefinition = "JSON")
	private @Nullable String modalInterchange;

	@Column(length = 30)
	private @Nullable String modeSegment;

	@Column(columnDefinition = "JSON")
	private @Nullable String tonicization;

	// ── Ambiguity ──
	@Column(nullable = false)
	private double ambiguityScore;

	@Column(columnDefinition = "JSON")
	private @Nullable String ambiguityFlags;

	// ── 1:1 관계 ──
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chord_info_id", nullable = false, unique = true)
	private ChordInfo chordInfo;

	@Builder
	public ChordAnalysis(@Nullable String degree, @Nullable Boolean isDiatonic,
		@Nullable String normalizedQuality, @Nullable String functions,
		@Nullable String secondaryDominant, @Nullable String diminishedFunction,
		@Nullable String chromaticApproach, @Nullable String deceptiveResolution,
		@Nullable String pedalInfo, @Nullable String modalInterchange,
		@Nullable String modeSegment, @Nullable String tonicization,
		double ambiguityScore, @Nullable String ambiguityFlags,
		ChordInfo chordInfo) {
		this.degree = degree;
		this.isDiatonic = isDiatonic;
		this.normalizedQuality = normalizedQuality;
		this.functions = functions;
		this.secondaryDominant = secondaryDominant;
		this.diminishedFunction = diminishedFunction;
		this.chromaticApproach = chromaticApproach;
		this.deceptiveResolution = deceptiveResolution;
		this.pedalInfo = pedalInfo;
		this.modalInterchange = modalInterchange;
		this.modeSegment = modeSegment;
		this.tonicization = tonicization;
		this.ambiguityScore = ambiguityScore;
		this.ambiguityFlags = ambiguityFlags;
		this.chordInfo = chordInfo;
	}

	public void update(@Nullable String degree, @Nullable Boolean isDiatonic,
		@Nullable String normalizedQuality, @Nullable String functions,
		@Nullable String secondaryDominant, @Nullable String diminishedFunction,
		@Nullable String chromaticApproach, @Nullable String deceptiveResolution,
		@Nullable String pedalInfo, @Nullable String modalInterchange,
		@Nullable String modeSegment, @Nullable String tonicization,
		double ambiguityScore, @Nullable String ambiguityFlags) {
		this.degree = degree;
		this.isDiatonic = isDiatonic;
		this.normalizedQuality = normalizedQuality;
		this.functions = functions;
		this.secondaryDominant = secondaryDominant;
		this.diminishedFunction = diminishedFunction;
		this.chromaticApproach = chromaticApproach;
		this.deceptiveResolution = deceptiveResolution;
		this.pedalInfo = pedalInfo;
		this.modalInterchange = modalInterchange;
		this.modeSegment = modeSegment;
		this.tonicization = tonicization;
		this.ambiguityScore = ambiguityScore;
		this.ambiguityFlags = ambiguityFlags;
	}
}

