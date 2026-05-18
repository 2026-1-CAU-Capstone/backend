package com.jazzify.backend.domain.solo.service.implementation;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.solo.dto.request.SoloCreateRequest;
import com.jazzify.backend.domain.solo.dto.request.SoloUpdateRequest;
import com.jazzify.backend.domain.solo.dto.request.SoloVideoRequest;
import com.jazzify.backend.shared.domain.Instrument;
import com.jazzify.backend.domain.solo.entity.SoloSource;
import com.jazzify.backend.domain.solo.entity.Solo;
import com.jazzify.backend.domain.solo.entity.SoloMeasure;
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

	private final SoloRepository soloRepository;

	public Solo create(SoloCreateRequest request, SoloHarmonicData harmonic, SoloFeatures features) {
		Solo solo = Solo.builder()
			// 1. Identity
			.source(request.source() != null ? request.source() : SoloSource.UNKNOWN)
			.userId(request.userId())
			// 2. Performance
			.performer(request.performer())
			.composer(request.sheetData().composer())
			.title(request.title())
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

		Solo saved = soloRepository.save(solo);

		// 4. Sheet Data — 마디/음표를 개별 엔티티로 저장
		List<SoloMeasure> measures = SoloMapper.toMeasureEntities(saved, request.sheetData());
		saved.replaceMeasures(measures);

		return saved;
	}

	public void update(Solo solo, SoloUpdateRequest request, SoloHarmonicData harmonic, SoloFeatures features) {
		solo.update(
			// 2. Performance
			request.performer(),
			request.sheetData().composer(),
			request.title(),
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

		// 4. Sheet Data — 기존 마디/음표를 새 내용으로 교체 (orphanRemoval로 자동 삭제)
		List<SoloMeasure> measures = SoloMapper.toMeasureEntities(solo, request.sheetData());
		solo.replaceMeasures(measures);
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
}
