package com.jazzify.backend.domain.lick.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.shared.domain.HarmonicContext;
import com.jazzify.backend.shared.domain.Instrument;
import com.jazzify.backend.shared.domain.JazzStyle;
import com.jazzify.backend.domain.lick.entity.LickSource;
import com.jazzify.backend.shared.domain.RhythmFeel;
import com.jazzify.backend.shared.omr.OmrProcessingStatus;

@NullMarked
public record LickResponse(
	// ─── 1. IDENTITY ───────────────────────────────────────────────────
	UUID publicId,
	LickSource source,
	@Nullable UUID userId,
	boolean isOMR,
	LocalDateTime createdAt,
	LocalDateTime updatedAt,

	// ─── OMR 처리 상태 ─────────────────────────────────────────────────
	OmrProcessingStatus omrStatus,
	int omrProgress,
	@Nullable String omrFailureReason,

	// ─── 2. PERFORMANCE METADATA ───────────────────────────────────────
	@Nullable String performer,
	@Nullable String composer,
	String title,
	@Nullable String album,
	Instrument instrument,
	@Nullable JazzStyle style,
	@Nullable Integer tempo,
	@Nullable String key,
	@Nullable RhythmFeel rhythmFeel,
	@Nullable String timeSignature,

	// ─── 3. HARMONIC CONTEXT ───────────────────────────────────────────
	@Nullable List<String> chords,
	@Nullable List<String> chordsPerNote,
	@Nullable HarmonicContext harmonicContext,
	@Nullable String targetChord,

	// ─── 4. SHEET DATA (PENDING 상태에서는 null) ─────────────────────────
	@Nullable SheetDataResponse sheetData,

	// ─── 5. SIMILARITY FEATURES ────────────────────────────────────────
	@Nullable Integer nEvents,
	@Nullable List<Integer> pitches,
	@Nullable List<Integer> intervals,
	@Nullable List<Integer> parsons,
	@Nullable List<Integer> fuzzyIntervals,
	@Nullable List<Integer> durationClasses,
	@Nullable Integer pitchMin,
	@Nullable Integer pitchMax,
	@Nullable Integer pitchRange,
	@Nullable Double pitchMean,
	@Nullable Integer startPitch,
	@Nullable Integer endPitch,

	// ─── 6. VIDEOS ─────────────────────────────────────────────────────
	@Nullable LickVideoResponse video
) {
}
