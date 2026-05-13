package com.jazzify.backend.domain.solo.service;

import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.solo.dto.request.SoloCreateRequest;
import com.jazzify.backend.domain.solo.dto.request.SoloUpdateRequest;
import com.jazzify.backend.domain.solo.dto.request.SoloVideoRequest;
import com.jazzify.backend.domain.solo.dto.response.SoloResponse;
import com.jazzify.backend.domain.solo.entity.Solo;
import com.jazzify.backend.domain.solo.model.SoloFeatures;
import com.jazzify.backend.domain.solo.model.SoloHarmonicData;
import com.jazzify.backend.domain.solo.service.implementation.SoloFeatureCalculator;
import com.jazzify.backend.domain.solo.service.implementation.SoloReader;
import com.jazzify.backend.domain.solo.service.implementation.SoloWriter;
import com.jazzify.backend.domain.solo.util.SoloMapper;

import lombok.RequiredArgsConstructor;

@NullMarked
@Service
@RequiredArgsConstructor
public class SoloService {

	private final SoloReader soloReader;
	private final SoloWriter soloWriter;
	private final SoloFeatureCalculator soloFeatureCalculator;

	@Transactional
	public SoloResponse create(SoloCreateRequest request) {
		soloReader.validateNoDuplicate(request.title(), request.performer());
		SoloHarmonicData harmonic = soloFeatureCalculator.computeHarmonicData(
			request.sheetData(),
			request.chords(),
			request.chordsPerNote(),
			request.harmonicContext(),
			request.targetChord()
		);
		SoloFeatures features = soloFeatureCalculator.computeFeatures(
			request.sheetData(),
			request.features()
		);
		Solo solo = soloWriter.create(request, harmonic, features);
		return SoloMapper.toResponse(solo);
	}

	@Transactional(readOnly = true)
	public Page<SoloResponse> getAll(Pageable pageable) {
		return soloReader.getAll(pageable).map(SoloMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public SoloResponse getByPublicId(UUID publicId) {
		return SoloMapper.toResponse(soloReader.getByPublicId(publicId));
	}

	@Transactional
	public SoloResponse update(UUID publicId, SoloUpdateRequest request) {
		Solo solo = soloReader.getByPublicId(publicId);
		SoloHarmonicData harmonic = soloFeatureCalculator.computeHarmonicData(
			request.sheetData(),
			request.chords(),
			request.chordsPerNote(),
			request.harmonicContext(),
			request.targetChord()
		);
		SoloFeatures features = soloFeatureCalculator.computeFeatures(
			request.sheetData(),
			request.features()
		);
		soloWriter.update(solo, request, harmonic, features);
		return SoloMapper.toResponse(solo);
	}

	@Transactional
	public void delete(UUID publicId) {
		Solo solo = soloReader.getByPublicId(publicId);
		soloWriter.delete(solo);
	}

	@Transactional
	public SoloResponse updateVideo(UUID publicId, SoloVideoRequest request) {
		Solo solo = soloReader.getByPublicId(publicId);
		soloWriter.upsertVideo(solo, request);
		return SoloMapper.toResponse(solo);
	}

	@Transactional
	public void deleteVideo(UUID publicId) {
		Solo solo = soloReader.getByPublicId(publicId);
		soloWriter.deleteVideo(solo);
	}
}

