package com.jazzify.backend.domain.solo.entity;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.shared.domain.HarmonicContext;
import com.jazzify.backend.shared.domain.Instrument;
import com.jazzify.backend.shared.domain.JazzStyle;
import com.jazzify.backend.shared.domain.RhythmFeel;
import com.jazzify.backend.shared.persistence.BaseEntity;
import com.jazzify.backend.shared.persistence.converter.UuidBinaryConverter;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_solo")
@Getter
@NullMarked
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Solo extends BaseEntity {

	// ─── 1. IDENTITY ────────────────────────────────────────────────────
	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private SoloSource source;

	@Convert(converter = UuidBinaryConverter.class)
	@Column(columnDefinition = "BINARY(16)")
	private @Nullable UUID userId;

	@Column(name = "is_omr", nullable = false)
	private boolean isOMR;


	// ─── 2. PERFORMANCE METADATA ───────────────────────────────────────
	@Column(length = 255)
	private @Nullable String performer;

	@Column(length = 255)
	private @Nullable String composer;

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
	/** sheetData 응답 구조 전체를 JSON으로 반정규화 저장한다. */
	@Lob
	@Column(name = "sheet_data_json", columnDefinition = "LONGTEXT")
	private @Nullable String sheetDataJson;

	// ─── 6. VIDEO ──────────────────────────────────────────────────────
	/** 이 솔로에 연결된 영상 정보 (없을 수 있음). */
	@OneToOne(mappedBy = "solo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
	private @Nullable SoloVideo video;

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

	/**
	 * 메타데이터 및 유사도 특징값을 일괄 갱신한다.
	 */
	public void update(
		// 2. Performance
		@Nullable String performer,
		@Nullable String composer,
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
		this.composer = composer;
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


	/** 반정규화된 sheetData JSON을 교체한다. */
	public void replaceSheetDataJson(String newSheetDataJson) {
		this.sheetDataJson = newSheetDataJson;
	}

	/**
	 * 연결 영상 정보를 교체한다.
	 *
	 * @param newVideo 새로 연결할 영상 (null이면 기존 영상 삭제)
	 */
	public void replaceVideo(@Nullable SoloVideo newVideo) {
		this.video = newVideo;
	}
}
