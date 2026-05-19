package com.jazzify.backend.domain.lick.service;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.domain.lick.dto.request.LickCreateRequest;
import com.jazzify.backend.domain.lick.dto.request.LickOmrRequest;
import com.jazzify.backend.domain.lick.dto.request.LickUpdateRequest;
import com.jazzify.backend.domain.lick.dto.request.LickVideoRequest;
import com.jazzify.backend.domain.lick.dto.app.LickMetadataValueCountResult;
import com.jazzify.backend.domain.lick.dto.response.LickMetadataValueCountResponse;
import com.jazzify.backend.domain.lick.dto.request.SheetDataRequest;
import com.jazzify.backend.domain.lick.dto.response.LickResponse;
import com.jazzify.backend.domain.lick.entity.Lick;
import com.jazzify.backend.domain.lick.entity.LickSource;
import com.jazzify.backend.domain.lick.model.LickFeatures;
import com.jazzify.backend.domain.lick.model.LickHarmonicData;
import com.jazzify.backend.domain.lick.service.implementation.LickFeatureCalculator;
import com.jazzify.backend.domain.lick.service.implementation.LickOmrProcessor;
import com.jazzify.backend.domain.lick.service.implementation.LickReader;
import com.jazzify.backend.domain.lick.service.implementation.LickWriter;
import com.jazzify.backend.domain.lick.util.LickMapper;
import com.jazzify.backend.shared.domain.Instrument;
import com.jazzify.backend.shared.domain.JazzStyle;
import com.jazzify.backend.shared.domain.RhythmFeel;

import lombok.RequiredArgsConstructor;

@NullMarked
@Service
@RequiredArgsConstructor
public class LickService {

	private final LickReader lickReader;
	private final LickWriter lickWriter;
	private final LickFeatureCalculator lickFeatureCalculator;
	private final LickOmrProcessor lickOmrProcessor;

	@Transactional
	public LickResponse create(LickCreateRequest request) {
		LickHarmonicData harmonic = lickFeatureCalculator.computeHarmonicData(
			request.sheetData(),
			request.chords(),
			request.chordsPerNote(),
			request.harmonicContext(),
			request.targetChord()
		);
		LickFeatures features = lickFeatureCalculator.computeFeatures(
			request.sheetData(),
			request.features()
		);
		Lick lick = lickWriter.create(request, harmonic, features, false);
		return LickMapper.toResponse(lick);
	}

	@Transactional(readOnly = true)
	public Page<LickResponse> getAll(Pageable pageable, @Nullable String composer, @Nullable String performer) {
		return lickReader.getAll(pageable, composer, performer).map(LickMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public List<LickMetadataValueCountResponse> getComposerCounts(@Nullable String performer) {
		return lickReader.getComposerCounts(performer).stream()
			.map(LickService::toMetadataValueCountResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<LickMetadataValueCountResponse> getPerformerCounts(@Nullable String composer) {
		return lickReader.getPerformerCounts(composer).stream()
			.map(LickService::toMetadataValueCountResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public LickResponse getByPublicId(UUID publicId) {
		return LickMapper.toResponse(lickReader.getByPublicId(publicId));
	}

	@Transactional
	public LickResponse update(UUID publicId, LickUpdateRequest request) {
		Lick lick = lickReader.getByPublicId(publicId);
		LickHarmonicData harmonic = lickFeatureCalculator.computeHarmonicData(
			request.sheetData(),
			request.chords(),
			request.chordsPerNote(),
			request.harmonicContext(),
			request.targetChord()
		);
		LickFeatures features = lickFeatureCalculator.computeFeatures(
			request.sheetData(),
			request.features()
		);
		lickWriter.update(lick, request, harmonic, features);
		return LickMapper.toResponse(lick);
	}

	@Transactional
	public void delete(UUID publicId) {
		Lick lick = lickReader.getByPublicId(publicId);
		lickWriter.delete(lick);
	}

	@Transactional
	public LickResponse updateVideo(UUID publicId, LickVideoRequest request) {
		Lick lick = lickReader.getByPublicId(publicId);
		lickWriter.upsertVideo(lick, request);
		return LickMapper.toResponse(lick);
	}

	@Transactional
	public void deleteVideo(UUID publicId) {
		Lick lick = lickReader.getByPublicId(publicId);
		lickWriter.deleteVideo(lick);
	}

	/**
	 * 악보 파일(PNG/JPG/JPEG/PDF)을 OMR 서버로 처리한 뒤 릭으로 저장한다.
	 * <p>
	 * OMR 서버 호출은 트랜잭션 외부에서 수행되어 DB 커넥션을 장시간 점유하지 않는다.
	 * MusicVision의 {@code /omr/process} → 결과 다운로드 → chord assignments 결합 흐름을 사용한다.
	 * DB 쓰기는 {@link LickWriter}(별도 빈)의 {@code @Transactional} 메서드에서 처리된다.
	 *
	 * @param file     업로드된 악보 파일
	 * @param metadata 연주자·악기 등 옵션 메타데이터 (미입력 시 MusicXML 파싱 값 사용)
	 * @return 생성된 릭 응답 DTO
	 */
	public LickResponse createFromOmr(MultipartFile file, LickOmrRequest metadata) {
		// 1. OMR 처리 (트랜잭션 외부 — HTTP 통신 + XML 파싱)
		SheetDataRequest sheetData = lickOmrProcessor.process(file);
		LickCreateRequest request = buildOmrCreateRequest(metadata, sheetData);

		// 2. 화성 데이터 계산 (트랜잭션 불필요)
		LickHarmonicData harmonic = lickFeatureCalculator.computeHarmonicData(
			request.sheetData(),
			request.chords(),
			request.chordsPerNote(),
			request.harmonicContext(),
			request.targetChord()
		);

		// 3. 유사도 피처 계산 (트랜잭션 불필요)
		LickFeatures features = lickFeatureCalculator.computeFeatures(
			request.sheetData(),
			request.features()
		);

		// 4. DB 저장 — lickWriter 는 별도 빈(@Component)이므로 @Transactional 정상 작동
		Lick lick = lickWriter.create(request, harmonic, features, true);
		return LickMapper.toResponse(lick);
	}

	// ─── Private ────────────────────────────────────────────────────────

	private LickCreateRequest buildOmrCreateRequest(LickOmrRequest metadata, SheetDataRequest sheetData) {
		String title = metadata.title() != null ? metadata.title()
			: (sheetData.title() != null ? sheetData.title() : "Untitled");

		return new LickCreateRequest(
			LickSource.from(metadata.source()),
			parseUuid(metadata.userId()),
			metadata.performer(),
			title,
			metadata.album(),
			Instrument.from(metadata.instrument()),
			JazzStyle.from(metadata.style()),
			metadata.tempo() != null ? metadata.tempo() : sheetData.tempo(),
			metadata.key() != null ? metadata.key() : sheetData.key(),
			RhythmFeel.from(metadata.rhythmFeel()),
			sheetData.timeSignature(),
			null,  // chords       — 자동 추출
			null,  // chordsPerNote — 자동 추출
			null,  // harmonicContext — 자동 감지
			null,  // targetChord  — 자동 설정
			sheetData,
			null   // features     — 자동 계산
		);
	}

	@org.jspecify.annotations.Nullable
	private static UUID parseUuid(@org.jspecify.annotations.Nullable String uuidStr) {
		if (uuidStr == null || uuidStr.isBlank()) return null;
		try {
			return UUID.fromString(uuidStr);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static LickMetadataValueCountResponse toMetadataValueCountResponse(LickMetadataValueCountResult result) {
		return new LickMetadataValueCountResponse(result.name(), result.count());
	}
}
