package com.jazzify.backend.domain.lick.service;

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

import com.jazzify.backend.domain.lick.dto.request.LickCreateRequest;
import com.jazzify.backend.domain.lick.dto.request.LickOmrRequest;
import com.jazzify.backend.domain.lick.dto.request.LickUpdateRequest;
import com.jazzify.backend.domain.lick.dto.request.LickVideoRequest;
import com.jazzify.backend.domain.sheetproject.dto.request.OmrCallbackRequest;
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
public class LickService {

	private static final String DEFAULT_PENDING_TITLE = "OMR Processing";

	private final LickReader lickReader;
	private final LickWriter lickWriter;
	private final LickFeatureCalculator lickFeatureCalculator;
	private final LickOmrProcessor lickOmrProcessor;
	private final OmrClient omrClient;
	private final OmrProperties omrProperties;

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

	public LickResponse createFromOmr(MultipartFile file, LickOmrRequest metadata) {
		OmrFileValidator.validate(file);
		byte[] fileData = readFileBytes(file);

		String originalFilename = defaultFileName(file.getOriginalFilename());
		String pendingTitle = hasText(metadata.title()) ? Objects.requireNonNull(metadata.title()).trim() : extractBaseName(originalFilename);

		// 1단계: 트랜잭션 내에서 PENDING 엔티티를 생성하고 커밋한다.
		//        LickWriter 는 @Transactional 이므로 호출 즉시 자체 트랜잭션을 열고 커밋한다.
		Lick lick = lickWriter.createPending(
			LickSource.from(metadata.source()),
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

		UUID lickPublicId = Objects.requireNonNull(lick.getPublicId());

		// 2단계: 커밋이 완료된 후 OMR 서버에 파일을 제출하고 job_id 응답을 확인한다.
		try {
			OmrClient.OmrSubmitResult result = omrClient.submitJob(fileData, originalFilename, lickPublicId.toString(), OmrCallbackDomain.LICK);
			lickWriter.storeJobIdAndMarkProcessing(lickPublicId, Objects.requireNonNull(result.jobId()), 10);
		} catch (CustomException e) {
			lickWriter.fail(lickPublicId, e.getMessage(), 0);
			throw e;
		} catch (Exception e) {
			lickWriter.fail(lickPublicId, e.getMessage(), 0);
			throw OmrErrorCode.OMR_SUBMIT_FAILED.toException(e.getMessage());
		}

		// 3단계: 최신 상태(PROCESSING)를 DB에서 다시 읽어 반환한다.
		return LickMapper.toResponse(lickReader.getByPublicId(lickPublicId));
	}

	@Transactional
	public void handleOmrCallback(String callbackApiKey, OmrCallbackRequest callbackRequest) {
		validateCallbackApiKey(callbackApiKey);

		UUID lickPublicId;
		try {
			lickPublicId = UUID.fromString(callbackRequest.jobId());
		} catch (IllegalArgumentException e) {
			throw OmrErrorCode.OMR_JOB_NOT_FOUND.toException("유효하지 않은 job_id 형식입니다: " + callbackRequest.jobId());
		}

		Lick lick = lickReader.findByPublicId(lickPublicId)
			.orElseThrow(() -> OmrErrorCode.OMR_JOB_NOT_FOUND.toException("job_id=" + callbackRequest.jobId()));

		if (callbackRequest.isCompleted()) {
			UUID publicId = Objects.requireNonNull(lick.getPublicId());
			lickWriter.markProcessing(publicId, 80);

			LickOmrProcessor.ProcessedSheetData processedSheetData = lickOmrProcessor.processJobResult(callbackRequest.jobId());
			LickCreateRequest request = buildOmrCreateRequest(lick, processedSheetData);
			LickHarmonicData harmonic = lickFeatureCalculator.computeHarmonicData(
				request.sheetData(),
				request.chords(),
				request.chordsPerNote(),
				request.harmonicContext(),
				request.targetChord()
			);
			LickFeatures features = lickFeatureCalculator.computeFeatures(request.sheetData(), request.features());
			lickWriter.completePending(publicId, request, harmonic, features);
		} else if (callbackRequest.isFailed()) {
			String errorMessage = hasText(callbackRequest.error()) ? callbackRequest.error()
				: (hasText(callbackRequest.message()) ? callbackRequest.message() : "OMR 처리 실패");
			lickWriter.fail(Objects.requireNonNull(lick.getPublicId()), errorMessage, 0);
		}
	}

	// ─── Private ────────────────────────────────────────────────────────

	private LickCreateRequest buildOmrCreateRequest(
		Lick lick,
		LickOmrProcessor.ProcessedSheetData processedSheetData
	) {
		SheetDataRequest sheetData = processedSheetData.sheetData();
		String title = lick.getTitle() != null ? lick.getTitle()
			: (sheetData.title() != null ? sheetData.title() : "Untitled");
		String composer = lick.getComposer() != null ? lick.getComposer() : processedSheetData.composer();

		return new LickCreateRequest(
			lick.getSource(),
			lick.getUserId(),
			lick.getPerformer(),
			composer,
			title,
			lick.getAlbum(),
			lick.getInstrument(),
			lick.getStyle(),
			lick.getTempo() != null ? lick.getTempo() : sheetData.tempo(),
			lick.getMusicalKey() != null ? lick.getMusicalKey() : sheetData.key(),
			lick.getRhythmFeel(),
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
