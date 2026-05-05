package com.jazzify.backend.domain.lick.dto.request;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.domain.lick.entity.HarmonicContext;
import com.jazzify.backend.domain.lick.entity.Instrument;
import com.jazzify.backend.domain.lick.entity.JazzStyle;
import com.jazzify.backend.domain.lick.entity.RhythmFeel;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@NullMarked
public record LickUpdateRequest(
	// ─── 2. PERFORMANCE METADATA ───────────��───────────────────────────
	@Nullable @Size(max = 255) String performer,
	@NotBlank @Size(max = 255) String title,
	@Nullable @Size(max = 255) String album,
	@NotNull Instrument instrument,
	@Nullable JazzStyle style,
	@Nullable @Min(1) @Max(500) Integer tempo,
	@Nullable @Size(max = 20) String key,
	@Nullable RhythmFeel rhythmFeel,
	@Nullable @Size(max = 10) String timeSignature,

	// ─── 3. HARMONIC CONTEXT (선택 — 미입력 시 sheetData에서 자동 추출) ──
	@Nullable List<String> chords,
	@Nullable List<String> chordsPerNote,
	@Nullable HarmonicContext harmonicContext,
	@Nullable String targetChord,

	// ─── 4. SHEET DATA ─────────────────────────────────────────────────
	@NotNull @Valid SheetDataRequest sheetData,

	// ─── 5. SIMILARITY FEATURES (선택 — 미입력 시 sheetData에서 자동 계산) ──
	@Nullable SimilarityFeaturesRequest features
) {
}
