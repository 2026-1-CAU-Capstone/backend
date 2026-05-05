package com.jazzify.backend.domain.lick.entity;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.shared.persistence.BaseEntity;
import com.jazzify.backend.shared.persistence.converter.UuidBinaryConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_lick")
@Getter
@NullMarked
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Lick extends BaseEntity {

	// ─── 1. IDENTITY ────────────────────────────────────────────────────
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private LickSource source;

	@Convert(converter = UuidBinaryConverter.class)
	@Column(columnDefinition = "BINARY(16)")
	private @Nullable UUID userId;

	// ─── 2. PERFORMANCE METADATA ───────────────────────────────────────
	@Column(length = 255)
	private @Nullable String performer;

	@Column(nullable = false, length = 255)
	private String title;

	@Column(length = 255)
	private @Nullable String album;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Instrument instrument;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private @Nullable JazzStyle style;

	@Column
	private @Nullable Integer tempo;

	@Column(name = "musical_key", length = 20)
	private @Nullable String musicalKey;

	@Enumerated(EnumType.STRING)
	@Column(length = 20)
	private @Nullable RhythmFeel rhythmFeel;

	@Column(length = 10)
	private @Nullable String timeSignature;

	// ─── 3. HARMONIC CONTEXT ───────────────────────────────────────────
	// JSON arrays stored as TEXT (e.g., ["D-7","G7","CMaj7"])
	@Lob
	@Column(columnDefinition = "TEXT")
	private @Nullable String chords;

	@Lob
	@Column(columnDefinition = "TEXT")
	private @Nullable String chordsPerNote;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private @Nullable HarmonicContext harmonicContext;

	@Column(length = 50)
	private @Nullable String targetChord;

	// ─── 4. SHEET DATA ─────────────────────────────────────────────────
	// Full VexFlow-renderable JSON stored as TEXT
	@Lob
	@Column(nullable = false, columnDefinition = "TEXT")
	private String sheetData;

	// ─── 5. SIMILARITY FEATURES ────────────────────────────────────────
	@Column
	private @Nullable Integer nEvents;

	@Lob
	@Column(columnDefinition = "TEXT")
	private @Nullable String pitches;

	@Lob
	@Column(columnDefinition = "TEXT")
	private @Nullable String intervals;

	@Lob
	@Column(columnDefinition = "TEXT")
	private @Nullable String parsons;

	@Lob
	@Column(columnDefinition = "TEXT")
	private @Nullable String fuzzyIntervals;

	@Lob
	@Column(columnDefinition = "TEXT")
	private @Nullable String durationClasses;

	@Column
	private @Nullable Integer pitchMin;

	@Column
	private @Nullable Integer pitchMax;

	@Column
	private @Nullable Integer pitchRange;

	@Column
	private @Nullable Double pitchMean;

	@Column
	private @Nullable Integer startPitch;

	@Column
	private @Nullable Integer endPitch;

	// ─── Update ────────────────────────────────────────────────────────
	public void update(
		// 2. Performance
		@Nullable String performer,
		String title,
		@Nullable String album,
		Instrument instrument,
		@Nullable JazzStyle style,
		@Nullable Integer tempo,
		@Nullable String musicalKey,
		@Nullable RhythmFeel rhythmFeel,
		@Nullable String timeSignature,
		// 3. Harmonic Context
		@Nullable String chords,
		@Nullable String chordsPerNote,
		@Nullable HarmonicContext harmonicContext,
		@Nullable String targetChord,
		// 4. Sheet Data
		String sheetData,
		// 5. Similarity Features
		@Nullable Integer nEvents,
		@Nullable String pitches,
		@Nullable String intervals,
		@Nullable String parsons,
		@Nullable String fuzzyIntervals,
		@Nullable String durationClasses,
		@Nullable Integer pitchMin,
		@Nullable Integer pitchMax,
		@Nullable Integer pitchRange,
		@Nullable Double pitchMean,
		@Nullable Integer startPitch,
		@Nullable Integer endPitch
	) {
		this.performer = performer;
		this.title = title;
		this.album = album;
		this.instrument = instrument;
		this.style = style;
		this.tempo = tempo;
		this.musicalKey = musicalKey;
		this.rhythmFeel = rhythmFeel;
		this.timeSignature = timeSignature;
		this.chords = chords;
		this.chordsPerNote = chordsPerNote;
		this.harmonicContext = harmonicContext;
		this.targetChord = targetChord;
		this.sheetData = sheetData;
		this.nEvents = nEvents;
		this.pitches = pitches;
		this.intervals = intervals;
		this.parsons = parsons;
		this.fuzzyIntervals = fuzzyIntervals;
		this.durationClasses = durationClasses;
		this.pitchMin = pitchMin;
		this.pitchMax = pitchMax;
		this.pitchRange = pitchRange;
		this.pitchMean = pitchMean;
		this.startPitch = startPitch;
		this.endPitch = endPitch;
	}
}
