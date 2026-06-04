package com.jazzify.backend.domain.solo.service.implementation;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.solo.dto.request.SoloCreateRequest;
import com.jazzify.backend.domain.solo.dto.request.SoloUpdateRequest;
import com.jazzify.backend.domain.solo.dto.request.SoloVideoRequest;
import com.jazzify.backend.shared.domain.Instrument;
import com.jazzify.backend.shared.domain.JazzStyle;
import com.jazzify.backend.shared.domain.RhythmFeel;
import com.jazzify.backend.domain.solo.entity.SoloSource;
import com.jazzify.backend.domain.solo.entity.Solo;
import com.jazzify.backend.domain.solo.entity.SoloVideo;
import com.jazzify.backend.domain.solo.model.SoloFeatures;
import com.jazzify.backend.domain.solo.model.SoloHarmonicData;
import com.jazzify.backend.domain.solo.repository.SoloRepository;
import com.jazzify.backend.domain.solo.util.SoloMapper;

import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
@Transactional
public class SoloWriter {

	private static final String UNKNOWN_METADATA = "Unknown";
	private static final int FAILED_MESSAGE_MAX_LENGTH = 500;

	private final SoloRepository soloRepository;

	public Solo createPending(
		SoloSource source,
		@Nullable UUID userId,
		@Nullable String performer,
		@Nullable String composer,
		String title,
		@Nullable String album,
		Instrument instrument,
		@Nullable JazzStyle style,
		@Nullable Integer tempo,
		@Nullable String musicalKey,
		@Nullable String timeSignature,
		@Nullable RhythmFeel rhythmFeel
	) {
		Solo solo = Solo.builder()
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
		solo.markOmrQueued();
		return soloRepository.save(solo);
	}

	public Solo create(SoloCreateRequest request, SoloHarmonicData harmonic, SoloFeatures features, boolean isOMR) {
		Solo solo = Solo.builder()
			// 1. Identity
			.source(request.source() != null ? request.source() : SoloSource.UNKNOWN)
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
			.chords(SoloMapper.serializeList(harmonic.chords()))
			.chordsPerNote(SoloMapper.serializeList(harmonic.chordsPerNote()))
			.harmonicContext(harmonic.harmonicContext())
			.targetChord(harmonic.targetChord())
			// 4. Sheet Data
			.sheetDataJson(SoloMapper.serializeSheetData(SoloMapper.toSheetDataResponse(request.sheetData())))
			// 5. Similarity Features
			.nEvents(features.nEvents())
			.pitches(SoloMapper.serializeList(features.pitches()))
			.intervals(SoloMapper.serializeList(features.intervals()))
			.parsons(SoloMapper.serializeList(features.parsons()))
			.fuzzyIntervals(SoloMapper.serializeList(features.fuzzyIntervals()))
			.durationClasses(SoloMapper.serializeList(features.durationClasses()))
			.pitchMin(features.pitchMin())
			.pitchMax(features.pitchMax())
			.pitchRange(features.pitchRange())
			.pitchMean(features.pitchMean())
			.startPitch(features.startPitch())
			.endPitch(features.endPitch())
			.build();

		return soloRepository.save(solo);
	}

	public void update(Solo solo, SoloUpdateRequest request, SoloHarmonicData harmonic, SoloFeatures features) {
		solo.update(
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
			SoloMapper.serializeList(harmonic.chords()),
			SoloMapper.serializeList(harmonic.chordsPerNote()),
			harmonic.harmonicContext(),
			harmonic.targetChord(),
			// 5. Similarity Features
			features.nEvents(),
			SoloMapper.serializeList(features.pitches()),
			SoloMapper.serializeList(features.intervals()),
			SoloMapper.serializeList(features.parsons()),
			SoloMapper.serializeList(features.fuzzyIntervals()),
			SoloMapper.serializeList(features.durationClasses()),
			features.pitchMin(),
			features.pitchMax(),
			features.pitchRange(),
			features.pitchMean(),
			features.startPitch(),
			features.endPitch()
		);
		solo.replaceSheetDataJson(SoloMapper.serializeSheetData(SoloMapper.toSheetDataResponse(request.sheetData())));
	}

	public void completePending(UUID publicId, SoloCreateRequest request, SoloHarmonicData harmonic, SoloFeatures features) {
		soloRepository.findByPublicId(publicId)
			.ifPresent(solo -> {
				solo.update(
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
					SoloMapper.serializeList(harmonic.chords()),
					SoloMapper.serializeList(harmonic.chordsPerNote()),
					harmonic.harmonicContext(),
					harmonic.targetChord(),
					features.nEvents(),
					SoloMapper.serializeList(features.pitches()),
					SoloMapper.serializeList(features.intervals()),
					SoloMapper.serializeList(features.parsons()),
					SoloMapper.serializeList(features.fuzzyIntervals()),
					SoloMapper.serializeList(features.durationClasses()),
					features.pitchMin(),
					features.pitchMax(),
					features.pitchRange(),
					features.pitchMean(),
					features.startPitch(),
					features.endPitch()
				);
				solo.replaceSheetDataJson(SoloMapper.serializeSheetData(SoloMapper.toSheetDataResponse(request.sheetData())));
				solo.markOmrCompleted();
			});
	}

	public void storeJobIdAndMarkProcessing(UUID publicId, String omrJobId, int progress) {
		soloRepository.findByPublicId(publicId)
			.ifPresent(solo -> {
				solo.storeOmrJobId(omrJobId);
				solo.markOmrProcessing(normalizeProgress(progress));
			});
	}

	public void markProcessing(UUID publicId, int progress) {
		soloRepository.findByPublicId(publicId)
			.ifPresent(solo -> solo.markOmrProcessing(normalizeProgress(progress)));
	}

	public void fail(UUID publicId, @Nullable String failureReason, int progress) {
		soloRepository.findByPublicId(publicId)
			.ifPresent(solo -> solo.markOmrFailed(truncate(failureReason), normalizeProgress(progress)));
	}

	public void delete(Solo solo) {
		soloRepository.delete(solo);
	}

	/**
	 * 솔로에 영상을 연결하거나 기존 영상을 갱신한다 (upsert).
	 */
	public void upsertVideo(Solo solo, SoloVideoRequest request) {
		SoloVideo video = SoloVideo.builder()
			.solo(solo)
			.videoId(request.videoId())
			.startSec(request.startSec())
			.endSec(request.endSec())
			.url(request.url())
			.build();
		solo.replaceVideo(video);
	}

	/**
	 * 솔로에 연결된 영상을 삭제한다.
	 */
	public void deleteVideo(Solo solo) {
		solo.replaceVideo(null);
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

	private static String truncate(@Nullable String failureReason) {
		if (failureReason == null || failureReason.isBlank()) {
			return "OMR 처리 중 알 수 없는 오류가 발생했습니다.";
		}
		return failureReason.length() <= FAILED_MESSAGE_MAX_LENGTH
			? failureReason
			: failureReason.substring(0, FAILED_MESSAGE_MAX_LENGTH);
	}
}
