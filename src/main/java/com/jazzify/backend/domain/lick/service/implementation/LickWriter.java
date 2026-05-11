package com.jazzify.backend.domain.lick.service.implementation;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.lick.dto.request.LickCreateRequest;
import com.jazzify.backend.domain.lick.dto.request.LickUpdateRequest;
import com.jazzify.backend.domain.lick.dto.request.LickVideoRequest;
import com.jazzify.backend.domain.lick.entity.Instrument;
import com.jazzify.backend.domain.lick.entity.LickSource;
import com.jazzify.backend.domain.lick.entity.Lick;
import com.jazzify.backend.domain.lick.entity.LickMeasure;
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

	private final LickRepository lickRepository;

	public Lick create(LickCreateRequest request, LickHarmonicData harmonic, LickFeatures features) {
		Lick lick = Lick.builder()
			// 1. Identity
			.source(request.source() != null ? request.source() : LickSource.UNKNOWN)
			.userId(request.userId())
			// 2. Performance
			.performer(request.performer())
			.title(request.title())
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

		Lick saved = lickRepository.save(lick);

		// 4. Sheet Data — 마디/음표를 개별 엔티티로 저장
		List<LickMeasure> measures = LickMapper.toMeasureEntities(saved, request.sheetData());
		saved.replaceMeasures(measures);

		return saved;
	}

	public void update(Lick lick, LickUpdateRequest request, LickHarmonicData harmonic, LickFeatures features) {
		lick.update(
			// 2. Performance
			request.performer(),
			request.title(),
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

		// 4. Sheet Data — 기존 마디/음표를 새 내용으로 교체 (orphanRemoval로 자동 삭제)
		List<LickMeasure> measures = LickMapper.toMeasureEntities(lick, request.sheetData());
		lick.replaceMeasures(measures);
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
}
