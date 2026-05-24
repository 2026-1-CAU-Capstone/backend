package com.jazzify.backend.domain.solo.service;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.domain.sheetproject.dto.request.OmrCallbackRequest;
import com.jazzify.backend.domain.solo.dto.app.SoloMetadataValueCountResult;
import com.jazzify.backend.domain.solo.dto.request.SheetDataRequest;
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
import com.jazzify.backend.shared.exception.CustomException;
import com.jazzify.backend.shared.exception.code.OmrErrorCode;
import com.jazzify.backend.shared.omr.OmrCallbackDomain;
import com.jazzify.backend.shared.omr.OmrClient;
import com.jazzify.backend.shared.omr.OmrFileValidator;
import com.jazzify.backend.shared.omr.OmrProperties;

import lombok.RequiredArgsConstructor;

@NullMarked
@Service
@RequiredArgsConstructor
public class SoloService {

	private static final String DEFAULT_PENDING_TITLE = "OMR Processing";

	private final SoloReader soloReader;
	private final SoloWriter soloWriter;
	private final SoloFeatureCalculator soloFeatureCalculator;
	private final SoloOmrProcessor soloOmrProcessor;
	private final OmrClient omrClient;
	private final OmrProperties omrProperties;

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

	public SoloResponse createFromOmr(MultipartFile file, SoloOmrRequest metadata) {
		OmrFileValidator.validate(file);
		byte[] fileData = readFileBytes(file);

		String originalFilename = defaultFileName(file.getOriginalFilename());
		String pendingTitle = hasText(metadata.title()) ? Objects.requireNonNull(metadata.title()).trim() : extractBaseName(originalFilename);

		soloReader.validateNoDuplicate(pendingTitle, metadata.performer());

		// 1단계: PENDING 엔티티를 생성하고 커밋한다.
		Solo solo = soloWriter.createPending(
			SoloSource.from(metadata.source()),
			parseUuid(metadata.userId()),
			metadata.performer(),
			metadata.composer(),
			pendingTitle,
			metadata.album(),
			Instrument.from(metadata.instrument()),
			JazzStyle.from(metadata.style()),
			metadata.tempo(),
			metadata.key(),
			RhythmFeel.from(metadata.rhythmFeel())
		);

		UUID soloPublicId = Objects.requireNonNull(solo.getPublicId());

		// 2단계: 커밋 후 OMR 서버에 파일을 제출하고 job_id 응답을 확인한다.
		try {
			OmrClient.OmrSubmitResult result = omrClient.submitJob(fileData, originalFilename, soloPublicId.toString(), OmrCallbackDomain.SOLO);
			soloWriter.storeJobIdAndMarkProcessing(soloPublicId, Objects.requireNonNull(result.jobId()), 10);
		} catch (CustomException e) {
			soloWriter.fail(soloPublicId, e.getMessage(), 0);
			throw e;
		} catch (Exception e) {
			soloWriter.fail(soloPublicId, e.getMessage(), 0);
			throw OmrErrorCode.OMR_SUBMIT_FAILED.toException(e.getMessage());
		}

		// 3단계: 최신 상태(PROCESSING)를 DB에서 다시 읽어 반환한다.
		return SoloMapper.toResponse(soloReader.getByPublicId(soloPublicId));
	}

	@Transactional
	public void handleOmrCallback(String callbackApiKey, OmrCallbackRequest callbackRequest) {
		validateCallbackApiKey(callbackApiKey);

		UUID soloPublicId;
		try {
			soloPublicId = UUID.fromString(callbackRequest.jobId());
		} catch (IllegalArgumentException e) {
			throw OmrErrorCode.OMR_JOB_NOT_FOUND.toException("유효하지 않은 job_id 형식입니다: " + callbackRequest.jobId());
		}

		Solo solo = soloReader.findByPublicId(soloPublicId)
			.orElseThrow(() -> OmrErrorCode.OMR_JOB_NOT_FOUND.toException("job_id=" + callbackRequest.jobId()));

		if (callbackRequest.isCompleted()) {
			UUID publicId = Objects.requireNonNull(solo.getPublicId());
			soloWriter.markProcessing(publicId, 80);

			SoloOmrProcessor.ProcessedSheetData processedSheetData = soloOmrProcessor.processJobResult(callbackRequest.jobId());
			SoloCreateRequest request = buildOmrCreateRequest(solo, processedSheetData);
			SoloHarmonicData harmonic = soloFeatureCalculator.computeHarmonicData(
				request.sheetData(),
				request.chords(),
				request.chordsPerNote(),
				request.harmonicContext(),
				request.targetChord()
			);
			SoloFeatures features = soloFeatureCalculator.computeFeatures(request.sheetData(), request.features());
			soloWriter.completePending(publicId, request, harmonic, features);
		} else if (callbackRequest.isFailed()) {
			String errorMessage = hasText(callbackRequest.error()) ? callbackRequest.error()
				: (hasText(callbackRequest.message()) ? callbackRequest.message() : "OMR 처리 실패");
			soloWriter.fail(Objects.requireNonNull(solo.getPublicId()), errorMessage, 0);
		}
	}

	private SoloCreateRequest buildOmrCreateRequest(
		Solo solo,
		SoloOmrProcessor.ProcessedSheetData processedSheetData
	) {
		SheetDataRequest sheetData = processedSheetData.sheetData();
		String title = solo.getTitle();
		String composer = solo.getComposer() != null ? solo.getComposer() : processedSheetData.composer();

		return new SoloCreateRequest(
			solo.getSource(),
			solo.getUserId(),
			solo.getPerformer(),
			composer,
			title,
			solo.getAlbum(),
			solo.getInstrument(),
			solo.getStyle(),
			solo.getTempo() != null ? solo.getTempo() : sheetData.tempo(),
			solo.getMusicalKey() != null ? solo.getMusicalKey() : sheetData.key(),
			solo.getRhythmFeel(),
			sheetData.timeSignature(),
			null,
			null,
			null,
			null,
			sheetData,
			null
		);
	}

	private static @Nullable UUID parseUuid(@Nullable String uuidStr) {
		if (uuidStr == null || uuidStr.isBlank()) {
			return null;
		}
		try {
			return UUID.fromString(uuidStr);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private static SoloMetadataValueCountResponse toMetadataValueCountResponse(SoloMetadataValueCountResult result) {
		return new SoloMetadataValueCountResponse(result.name(), result.count());
	}

	private void validateCallbackApiKey(String providedKey) {
		String expectedKey = omrProperties.callbackApiKey();
		if (expectedKey == null || expectedKey.isBlank()) {
			return;
		}
		if (!expectedKey.equals(providedKey)) {
			throw OmrErrorCode.OMR_CALLBACK_KEY_INVALID.toException();
		}
	}

	private static boolean hasText(@Nullable String value) {
		return value != null && !value.isBlank();
	}

	private static String defaultFileName(@Nullable String originalFilename) {
		return hasText(originalFilename) ? Objects.requireNonNull(originalFilename).trim() : "upload.jpg";
	}

	private static String defaultContentType(@Nullable String contentType) {
		return hasText(contentType) ? Objects.requireNonNull(contentType).trim() : "application/octet-stream";
	}

	private static String extractBaseName(String originalFilename) {
		int lastDotIndex = originalFilename.lastIndexOf('.');
		if (lastDotIndex <= 0) {
			return hasText(originalFilename) ? originalFilename : DEFAULT_PENDING_TITLE;
		}
		String baseName = originalFilename.substring(0, lastDotIndex).trim();
		return hasText(baseName) ? baseName : DEFAULT_PENDING_TITLE;
	}

	private static byte[] readFileBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException e) {
			throw OmrErrorCode.OMR_FILE_READ_FAILED.toException(e.getMessage());
		}
	}
}
