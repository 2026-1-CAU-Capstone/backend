package com.jazzify.backend.domain.lick.service.implementation;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.lick.dto.request.LickCreateRequest;
import com.jazzify.backend.domain.lick.dto.request.LickUpdateRequest;
import com.jazzify.backend.domain.lick.dto.request.LickVideoRequest;
import com.jazzify.backend.shared.domain.Instrument;
import com.jazzify.backend.shared.domain.JazzStyle;
import com.jazzify.backend.shared.domain.RhythmFeel;
import com.jazzify.backend.domain.lick.entity.LickSource;
import com.jazzify.backend.domain.lick.entity.Lick;
import com.jazzify.backend.domain.lick.entity.LickVideo;
import com.jazzify.backend.domain.lick.model.LickFeatures;
import com.jazzify.backend.domain.lick.model.LickHarmonicData;
import com.jazzify.backend.domain.lick.repository.LickRepository;
import com.jazzify.backend.domain.lick.util.LickMapper;

import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional
public class LickWriter {

	private static final String UNKNOWN_METADATA = "Unknown";
	private static final int FAILED_MESSAGE_MAX_LENGTH = 500;

	private final LickRepository lickRepository;

	public Lick createPending(
		LickSource source,
		@Nullable UUID userId,
		@Nullable String performer,
		@Nullable String composer,
		String title,
		@Nullable String requestedTitle,
		@Nullable String album,
		Instrument instrument,
		@Nullable JazzStyle style,
		@Nullable Integer tempo,
		@Nullable String musicalKey,
		@Nullable String timeSignature,
		@Nullable RhythmFeel rhythmFeel
	) {
		Lick lick = Lick.builder()
			.source(source)
			.userId(userId)
			.isOMR(true)
			.performer(unknownIfBlank(performer))
			.composer(unknownIfBlank(composer))
			.title(unknownIfBlank(title))
			.album(album)
			.instrument(instrument)
			.style(style)
			.tempo(tempo)
			.musicalKey(musicalKey)
			.timeSignature(timeSignature)
			.rhythmFeel(rhythmFeel)
			.build();
		lick.markOmrQueued(trimToNull(requestedTitle), trimToNull(composer));
		return lickRepository.save(lick);
	}

	public Lick create(LickCreateRequest request, LickHarmonicData harmonic, LickFeatures features, boolean isOMR) {
		Lick lick = Lick.builder()
			// 1. Identity
			.source(request.source() != null ? request.source() : LickSource.UNKNOWN)
			.userId(request.userId())
			.isOMR(isOMR)
			// 2. Performance
			.performer(unknownIfBlank(request.performer()))
			.composer(unknownIfBlank(request.composer()))
			.title(unknownIfBlank(request.title()))
			.album(request.album())
			.instrument(request.instrument() != null ? request.instrument() : Instrument.UNKNOWN)
			.style(request.style())
			.tempo(request.tempo())
			.musicalKey(request.key())
			.rhythmFeel(request.rhythmFeel())
			.timeSignature(request.timeSignature())
			// 3. Harmonic Context
			.chords(LickMapper.serializeList(harmonic.chords()))
			.chordsPerNote(LickMapper.serializeList(harmonic.chordsPerNote()))
			.harmonicContext(harmonic.harmonicContext())
			.targetChord(harmonic.targetChord())
			// 4. Sheet Data
			.sheetDataJson(LickMapper.serializeSheetData(LickMapper.toSheetDataResponse(request.sheetData())))
			// 5. Similarity Features
			.nEvents(features.nEvents())
			.pitches(LickMapper.serializeList(features.pitches()))
			.intervals(LickMapper.serializeList(features.intervals()))
			.parsons(LickMapper.serializeList(features.parsons()))
			.fuzzyIntervals(LickMapper.serializeList(features.fuzzyIntervals()))
			.durationClasses(LickMapper.serializeList(features.durationClasses()))
			.pitchMin(features.pitchMin())
			.pitchMax(features.pitchMax())
			.pitchRange(features.pitchRange())
			.pitchMean(features.pitchMean())
			.startPitch(features.startPitch())
			.endPitch(features.endPitch())
			.build();

		return lickRepository.save(lick);
	}

	public void update(Lick lick, LickUpdateRequest request, LickHarmonicData harmonic, LickFeatures features) {
		lick.update(
			// 2. Performance
			unknownIfBlank(request.performer()),
			unknownIfBlank(request.composer()),
			unknownIfBlank(request.title()),
			request.album(),
			request.instrument() != null ? request.instrument() : Instrument.UNKNOWN,
			request.style(),
			request.tempo(),
			request.key(),
			request.rhythmFeel(),
			request.timeSignature(),
			// 3. Harmonic Context
			LickMapper.serializeList(harmonic.chords()),
			LickMapper.serializeList(harmonic.chordsPerNote()),
			harmonic.harmonicContext(),
			harmonic.targetChord(),
			// 5. Similarity Features
			features.nEvents(),
			LickMapper.serializeList(features.pitches()),
			LickMapper.serializeList(features.intervals()),
			LickMapper.serializeList(features.parsons()),
			LickMapper.serializeList(features.fuzzyIntervals()),
			LickMapper.serializeList(features.durationClasses()),
			features.pitchMin(),
			features.pitchMax(),
			features.pitchRange(),
			features.pitchMean(),
			features.startPitch(),
			features.endPitch()
		);
		lick.replaceSheetDataJson(LickMapper.serializeSheetData(LickMapper.toSheetDataResponse(request.sheetData())));
	}

	public void completePending(UUID publicId, LickCreateRequest request, LickHarmonicData harmonic, LickFeatures features) {
		lickRepository.findByPublicId(publicId)
			.ifPresent(lick -> {
				lick.update(
					unknownIfBlank(request.performer()),
					unknownIfBlank(request.composer()),
					unknownIfBlank(request.title()),
					request.album(),
					request.instrument() != null ? request.instrument() : Instrument.UNKNOWN,
					request.style(),
					request.tempo(),
					request.key(),
					request.rhythmFeel(),
					request.timeSignature(),
					LickMapper.serializeList(harmonic.chords()),
					LickMapper.serializeList(harmonic.chordsPerNote()),
					harmonic.harmonicContext(),
					harmonic.targetChord(),
					features.nEvents(),
					LickMapper.serializeList(features.pitches()),
					LickMapper.serializeList(features.intervals()),
					LickMapper.serializeList(features.parsons()),
					LickMapper.serializeList(features.fuzzyIntervals()),
					LickMapper.serializeList(features.durationClasses()),
					features.pitchMin(),
					features.pitchMax(),
					features.pitchRange(),
					features.pitchMean(),
					features.startPitch(),
					features.endPitch()
				);
				lick.replaceSheetDataJson(LickMapper.serializeSheetData(LickMapper.toSheetDataResponse(request.sheetData())));
				lick.markOmrCompleted();
			});
	}

	public void storeJobIdAndMarkProcessing(UUID publicId, String omrJobId, int progress) {
		lickRepository.findByPublicId(publicId)
			.ifPresent(lick -> {
				lick.storeOmrJobId(omrJobId);
				lick.markOmrProcessing(normalizeProgress(progress));
			});
	}

	public void markProcessing(UUID publicId, int progress) {
		lickRepository.findByPublicId(publicId)
			.ifPresent(lick -> lick.markOmrProcessing(normalizeProgress(progress)));
	}

	public void fail(UUID publicId, @Nullable String failureReason, int progress) {
		lickRepository.findByPublicId(publicId)
			.ifPresent(lick -> lick.markOmrFailed(truncate(failureReason), normalizeProgress(progress)));
	}

	public void delete(Lick lick) {
		lickRepository.delete(lick);
	}

	/**
	 * 릭에 영상을 연결하거나 기존 영상을 갱신한다 (upsert).
	 */
	public void upsertVideo(Lick lick, LickVideoRequest request) {
		LickVideo video = LickVideo.builder()
			.lick(lick)
			.videoId(request.videoId())
			.startSec(request.startSec())
			.endSec(request.endSec())
			.url(request.url())
			.build();
		lick.replaceVideo(video);
	}

	/**
	 * 릭에 연결된 영상을 삭제한다.
	 */
	public void deleteVideo(Lick lick) {
		lick.replaceVideo(null);
	}

	private static int normalizeProgress(int progress) {
		return Math.max(0, Math.min(progress, 100));
	}

	private static String unknownIfBlank(@Nullable String value) {
		if (value == null || value.isBlank()) {
			return UNKNOWN_METADATA;
		}
		return value.trim();
	}

	private static @Nullable String trimToNull(@Nullable String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}

	private static String truncate(@Nullable String failureReason) {
		if (failureReason == null || failureReason.isBlank()) {
			return "OMR 처리 중 알 수 없는 오류가 발생했습니다.";
		}
		return failureReason.length() <= FAILED_MESSAGE_MAX_LENGTH
			? failureReason
			: failureReason.substring(0, FAILED_MESSAGE_MAX_LENGTH);
	}
}
