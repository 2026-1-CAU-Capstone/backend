package com.jazzify.backend.domain.lick.service.implementation;

import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.lick.dto.request.LickCreateRequest;
import com.jazzify.backend.domain.lick.dto.request.LickUpdateRequest;
import com.jazzify.backend.domain.lick.entity.Lick;
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
			.source(request.source())
			.userId(request.userId())
			// 2. Performance
			.performer(request.performer())
			.title(request.title())
			.album(request.album())
			.instrument(request.instrument())
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
			.sheetData(LickMapper.serializeSheetData(request.sheetData()))
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
			request.performer(),
			request.title(),
			request.album(),
			request.instrument(),
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
			// 4. Sheet Data
			LickMapper.serializeSheetData(request.sheetData()),
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
	}

	public void delete(Lick lick) {
		lickRepository.delete(lick);
	}
}
