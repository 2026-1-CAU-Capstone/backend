package com.jazzify.backend.domain.solo.service;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.domain.solo.dto.request.SheetDataRequest;
import com.jazzify.backend.domain.solo.dto.app.SoloMetadataValueCountResult;
import com.jazzify.backend.domain.solo.dto.request.SoloCreateRequest;
import com.jazzify.backend.domain.solo.dto.request.SoloOmrRequest;
import com.jazzify.backend.domain.solo.dto.request.SoloUpdateRequest;
import com.jazzify.backend.domain.solo.dto.request.SoloVideoRequest;
import com.jazzify.backend.domain.solo.dto.response.SoloMetadataValueCountResponse;
import com.jazzify.backend.domain.solo.dto.response.SoloResponse;
import com.jazzify.backend.domain.solo.entity.Solo;
import com.jazzify.backend.domain.solo.entity.SoloSource;
import com.jazzify.backend.domain.solo.model.SoloFeatures;
import com.jazzify.backend.domain.solo.model.SoloHarmonicData;
import com.jazzify.backend.domain.solo.service.implementation.SoloFeatureCalculator;
import com.jazzify.backend.domain.solo.service.implementation.SoloOmrProcessor;
import com.jazzify.backend.domain.solo.service.implementation.SoloReader;
import com.jazzify.backend.domain.solo.service.implementation.SoloWriter;
import com.jazzify.backend.domain.solo.util.SoloMapper;
import com.jazzify.backend.shared.domain.Instrument;
import com.jazzify.backend.shared.domain.JazzStyle;
import com.jazzify.backend.shared.domain.RhythmFeel;

import lombok.RequiredArgsConstructor;

@NullMarked
@Service
@RequiredArgsConstructor
public class SoloService {

	private final SoloReader soloReader;
	private final SoloWriter soloWriter;
	private final SoloFeatureCalculator soloFeatureCalculator;
	private final SoloOmrProcessor soloOmrProcessor;

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
		Solo solo = soloWriter.create(request, harmonic, features, false);
		return SoloMapper.toResponse(solo);
	}

	@Transactional(readOnly = true)
	public Page<SoloResponse> getAll(Pageable pageable, @Nullable String composer, @Nullable String performer) {
		return soloReader.getAll(pageable, composer, performer).map(SoloMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public List<SoloMetadataValueCountResponse> getComposerCounts(@Nullable String performer) {
		return soloReader.getComposerCounts(performer).stream()
			.map(SoloService::toMetadataValueCountResponse)
			.toList();
	}

	@Transactional(readOnly = true)
	public List<SoloMetadataValueCountResponse> getPerformerCounts(@Nullable String composer) {
		return soloReader.getPerformerCounts(composer).stream()
			.map(SoloService::toMetadataValueCountResponse)
			.toList();
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

	/**
	 * 악보 파일(PNG/JPG/JPEG)을 OMR 서버로 처리한 뒤 솔로로 저장한다.
	 * <p>
	 * OMR 서버 호출은 트랜잭션 외부에서 수행되어 DB 커넥션을 장시간 점유하지 않는다.
	 * MusicVision의 {@code /omr/process} → 결과 다운로드 → 안전하게 매핑 가능한 chord assignments 결합 흐름을 사용한다.
	 * DB 쓰기는 {@link SoloWriter}(별도 빈)의 {@code @Transactional} 메서드에서 처리된다.
	 *
	 * @param file     업로드된 악보 파일
	 * @param metadata 연주자·악기 등 옵션 메타데이터 (미입력 시 MusicXML 파싱 값 사용)
	 * @return 생성된 솔로 응답 DTO
	 */
	public SoloResponse createFromOmr(MultipartFile file, SoloOmrRequest metadata) {
		// 1. OMR 처리 (트랜잭션 외부 — HTTP 통신 + XML 파싱)
		SoloOmrProcessor.ProcessedSheetData processedSheetData = soloOmrProcessor.process(file);
		SoloCreateRequest request = buildOmrCreateRequest(metadata, processedSheetData);

		// 2. 화성 데이터 계산 (트랜잭션 불필요)
		SoloHarmonicData harmonic = soloFeatureCalculator.computeHarmonicData(
			request.sheetData(),
			request.chords(),
			request.chordsPerNote(),
			request.harmonicContext(),
			request.targetChord()
		);

		// 3. 유사도 피처 계산 (트랜잭션 불필요)
		SoloFeatures features = soloFeatureCalculator.computeFeatures(
			request.sheetData(),
			request.features()
		);

		// 4. DB 저장 — soloWriter 는 별도 빈(@Component)이므로 @Transactional 정상 작동
		Solo solo = soloWriter.create(request, harmonic, features, true);
		return SoloMapper.toResponse(solo);
	}

	// ─── Private ────────────────────────────────────────────────────────

	private SoloCreateRequest buildOmrCreateRequest(
		SoloOmrRequest metadata,
		SoloOmrProcessor.ProcessedSheetData processedSheetData
	) {
		SheetDataRequest sheetData = processedSheetData.sheetData();
		String title = metadata.title() != null ? metadata.title()
			: (sheetData.title() != null ? sheetData.title() : "Untitled");
		String composer = metadata.composer() != null ? metadata.composer() : processedSheetData.composer();

		return new SoloCreateRequest(
			SoloSource.from(metadata.source()),
			parseUuid(metadata.userId()),
			metadata.performer(),
			composer,
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

	private static SoloMetadataValueCountResponse toMetadataValueCountResponse(SoloMetadataValueCountResult result) {
		return new SoloMetadataValueCountResponse(result.name(), result.count());
	}
}
