package com.jazzify.backend.domain.chordproject.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public record AnalysisResultResponse(
	UUID projectPublicId,
	String title,
	String keySignature,
	String timeSignature,
	@Nullable LocalDateTime lastAnalyzedAt,
	@Nullable AmbiguityStats ambiguityStats,
	List<ChordInfoResponse> chords,
	List<GroupResponse> groups,
	List<SectionResponse> sections
) {

	public record AmbiguityStats(
		int totalChords,
		int highConfidenceCount,
		int ambiguousCount,
		double meanAmbiguityScore,
		double maxAmbiguityScore
	) {
	}

	public record GroupResponse(
		UUID publicId,
		int groupIndex,
		String groupType,
		String variant,
		String targetKey,
		boolean isDiatonicTarget,
		@Nullable String notes,
		List<GroupMemberResponse> members
	) {
	}

	public record GroupMemberResponse(
		UUID chordInfoPublicId,
		String role
	) {
	}

	public record SectionResponse(
		UUID publicId,
		int startBar,
		int endBar,
		String sectionKey,
		String sectionType,
		@Nullable String mode,
		@Nullable Object tonicizations
	) {
	}
}

