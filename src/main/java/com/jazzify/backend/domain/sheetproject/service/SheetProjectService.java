package com.jazzify.backend.domain.sheetproject.service;

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
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.domain.sheetproject.dto.request.OmrCallbackRequest;
import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectCreateRequest;
import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectOmrCreateRequest;
import com.jazzify.backend.domain.sheetproject.dto.request.SheetProjectUpdateRequest;
import com.jazzify.backend.domain.sheetproject.dto.response.SheetProjectOmrCreateResponse;
import com.jazzify.backend.domain.sheetproject.dto.response.SheetProjectOmrStatusResponse;
import com.jazzify.backend.domain.sheetproject.dto.response.SheetProjectResponse;
import com.jazzify.backend.domain.sheetproject.entity.FileType;
import com.jazzify.backend.domain.sheetproject.entity.SheetFile;
import com.jazzify.backend.domain.sheetproject.entity.SheetProject;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetFileWriter;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetProjectOmrProcessor;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetProjectOmrWriter;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetProjectReader;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetProjectWriter;
import com.jazzify.backend.domain.sheetproject.util.SheetProjectMapper;
import com.jazzify.backend.domain.storagefile.entity.StorageFile;
import com.jazzify.backend.domain.storagefile.service.implementation.StorageFileReader;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.domain.user.service.implementation.UserReader;
import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.exception.CustomException;
import com.jazzify.backend.shared.exception.code.OmrErrorCode;
import com.jazzify.backend.shared.omr.OmrCallbackDomain;
import com.jazzify.backend.shared.omr.OmrClient;
import com.jazzify.backend.shared.omr.OmrFileValidator;
import com.jazzify.backend.shared.omr.OmrProperties;
import com.jazzify.backend.shared.omr.OmrProcessingStatus;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NullMarked
@Slf4j
@Service
@RequiredArgsConstructor
public class SheetProjectService {

	private static final String DEFAULT_PENDING_TITLE = "OMR Processing";

	private final SheetProjectReader sheetProjectReader;
	private final SheetProjectWriter sheetProjectWriter;
	private final SheetFileWriter sheetFileWriter;
	private final SheetProjectOmrWriter sheetProjectOmrWriter;
	private final SheetProjectOmrProcessor sheetProjectOmrProcessor;
	private final TransactionTemplate transactionTemplate;
	private final StorageFileReader storageFileReader;
	private final UserReader userReader;
	private final OmrClient omrClient;
	private final OmrProperties omrProperties;

	@Transactional
	public SheetProjectResponse create(UUID userPublicId, SheetProjectCreateRequest request) {
		User user = userReader.getByPublicId(userPublicId);

		List<StorageFile> storageFiles = request.storageFileIds().stream()
			.map(storageFileReader::getByPublicId)
			.toList();

		FileType fileType = FileType.fromFileName(storageFiles.get(0).getOriginalFileName());
		SheetFile sheetFile = sheetFileWriter.create(fileType);

		storageFiles.forEach(sf -> sf.linkToSheetFile(sheetFile));

		SheetProject project = sheetProjectWriter.create(request.title(), request.key(), user, sheetFile);
		return SheetProjectMapper.toResponse(project);
	}

	public SheetProjectOmrCreateResponse createFromOmr(
		UUID userPublicId,
		MultipartFile file,
		SheetProjectOmrCreateRequest request
	) {
		OmrFileValidator.validate(file);
		byte[] fileData = readFileBytes(file);

		String originalFilename = defaultFileName(file.getOriginalFilename());
		String contentType = defaultContentType(file.getContentType());
		String pendingTitle = hasText(request.title())
			? request.title().trim()
			: DEFAULT_PENDING_TITLE;

		// 1단계: User 조회와 PENDING 엔티티 생성을 같은 트랜잭션 안에서 실행하고 커밋한다.
		//        User 엔티티가 detached 되지 않도록 TransactionTemplate 으로 감싼다.
		SheetProject project = Objects.requireNonNull(
			transactionTemplate.execute(status -> {
				User user = userReader.getByPublicId(userPublicId);
				return sheetProjectOmrWriter.createPending(
					user,
					fileData,
					originalFilename,
					contentType,
					pendingTitle,
					request.key()
				);
			})
		);

		UUID projectPublicId = Objects.requireNonNull(project.getPublicId());

		// 2단계: 커밋 후 OMR 서버에 파일을 제출하고 job_id 응답을 확인한다.
		try {
			OmrClient.OmrSubmitResult result = omrClient.submitJob(fileData, originalFilename, projectPublicId.toString(), OmrCallbackDomain.SHEET_PROJECT);
			sheetProjectOmrWriter.storeJobIdAndMarkProcessing(projectPublicId, Objects.requireNonNull(result.jobId()), 10);
		} catch (CustomException e) {
			sheetProjectOmrWriter.fail(projectPublicId, e.getMessage(), 0);
			throw e;
		} catch (Exception e) {
			sheetProjectOmrWriter.fail(projectPublicId, e.getMessage(), 0);
			throw OmrErrorCode.OMR_SUBMIT_FAILED.toException(e.getMessage());
		}

		// 3단계: 최신 상태(PROCESSING)를 DB에서 다시 읽어 반환한다.
		SheetProject fresh = sheetProjectReader.findByPublicId(projectPublicId)
			.orElseThrow(() -> OmrErrorCode.OMR_JOB_NOT_FOUND.toException("projectPublicId=" + projectPublicId));
		return new SheetProjectOmrCreateResponse(SheetProjectMapper.toResponse(fresh), List.of());
	}

	@Transactional(readOnly = true)
	public Page<SheetProjectResponse> getAll(UUID userPublicId, Pageable pageable) {
		User user = userReader.getByPublicId(userPublicId);
		return sheetProjectReader.getAllByUser(user, pageable)
			.map(SheetProjectMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public SheetProjectResponse getByPublicId(UUID userPublicId, UUID projectPublicId) {
		User user = userReader.getByPublicId(userPublicId);
		SheetProject project = sheetProjectReader.getByPublicIdAndUser(projectPublicId, user);
		return SheetProjectMapper.toResponse(project);
	}

	@Transactional(readOnly = true)
	public SheetProjectOmrStatusResponse getOmrStatus(UUID userPublicId, UUID projectPublicId) {
		User user = userReader.getByPublicId(userPublicId);
		SheetProject project = sheetProjectReader.getByPublicIdAndUser(projectPublicId, user);
		int progress = resolveLatestOmrProgress(project.getOmrStatus(), project.getOmrJobId(), project.getOmrProgress());
		return new SheetProjectOmrStatusResponse(
			Objects.requireNonNull(project.getPublicId()),
			project.getOmrStatus(),
			progress,
			project.getOmrFailureReason()
		);
	}

	@Transactional
	public SheetProjectResponse update(UUID userPublicId, UUID projectPublicId, SheetProjectUpdateRequest request) {
		User user = userReader.getByPublicId(userPublicId);
		SheetProject project = sheetProjectReader.getByPublicIdAndUser(projectPublicId, user);
		project.update(request.title(), request.key());
		return SheetProjectMapper.toResponse(project);
	}

	@Transactional
	public void delete(UUID userPublicId, UUID projectPublicId) {
		User user = userReader.getByPublicId(userPublicId);
		SheetProject project = sheetProjectReader.getByPublicIdAndUser(projectPublicId, user);
		sheetProjectWriter.delete(project);
	}

	/**
	 * OMR 서버가 처리 완료 후 보내는 콜백을 처리한다.
	 * <p>
	 * {@code X-OMR-Callback-API-Key} 헤더를 검증하고, 완료 시 MusicXML과 chord assignments를
	 * 가져와 SheetProject를 업데이트한다. 실패 시 프로젝트를 실패 상태로 전환한다.
	 *
	 * @param callbackApiKey  OMR 서버가 헤더로 전달한 콜백 API 키
	 * @param callbackRequest OMR 서버가 전달한 콜백 페이로드
	 */
	@Transactional
	public void handleOmrCallback(String callbackApiKey, OmrCallbackRequest callbackRequest) {
		validateCallbackApiKey(callbackApiKey);

		String jobId = callbackRequest.jobId();

		// job_id = projectPublicId.toString() 이므로 UUID로 파싱해 역조회
		UUID projectPublicId;
		try {
			projectPublicId = UUID.fromString(jobId);
		} catch (IllegalArgumentException e) {
			throw OmrErrorCode.OMR_JOB_NOT_FOUND.toException("유효하지 않은 job_id 형식입니다: " + jobId);
		}

		SheetProject project = sheetProjectReader.findByPublicId(projectPublicId)
			.orElseThrow(() -> OmrErrorCode.OMR_JOB_NOT_FOUND.toException("job_id=" + jobId));

		if (callbackRequest.isCompleted()) {
			UUID publicId = Objects.requireNonNull(project.getPublicId());
			sheetProjectOmrWriter.markProcessing(publicId, 80);

			SheetProjectOmrProcessor.SheetProjectOmrData omrData = sheetProjectOmrProcessor.processJobResult(jobId);

			String requestedTitle = extractRequestedTitle(project);
			String title = hasText(requestedTitle) ? requestedTitle : omrData.title();
			MusicKey key = project.getKeySignature() != null ? project.getKeySignature() : omrData.key();

			sheetProjectOmrWriter.complete(
				publicId,
				title,
				key,
				omrData.timeSignature(),
				omrData.progression()
			);

		} else if (callbackRequest.isFailed()) {
			String errorMessage = hasText(callbackRequest.error()) ? callbackRequest.error()
				: (hasText(callbackRequest.message()) ? callbackRequest.message() : "OMR 처리 실패");
			sheetProjectOmrWriter.fail(Objects.requireNonNull(project.getPublicId()), errorMessage, 0);
		}
		// queued, processing 등 중간 상태는 무시 (완료/실패 시에만 콜백이 오는 것이 정상 use-case)
	}

	private void validateCallbackApiKey(String providedKey) {
		String expectedKey = omrProperties.callbackApiKey();
		if (expectedKey == null || expectedKey.isBlank()) {
			// callbackApiKey가 설정되지 않은 경우 검증 생략 (로컬 개발 편의)
			return;
		}
		if (!expectedKey.equals(providedKey)) {
			throw OmrErrorCode.OMR_CALLBACK_KEY_INVALID.toException();
		}
	}

	/** OMR pending 상태에서 임시 저장된 제목을 추출 (프로젝트 생성 시 사용자가 입력한 제목). */
	private static @Nullable String extractRequestedTitle(SheetProject project) {
		String title = project.getTitle();
		// 기본 pending 타이틀이면 null로 취급
		return "OMR Processing".equals(title) ? null : title;
	}

	private static boolean hasText(@Nullable String value) {
		return value != null && !value.isBlank();
	}

	private static String defaultFileName(@Nullable String originalFilename) {
		return hasText(originalFilename) ? Objects.requireNonNull(originalFilename).trim() : "upload.pdf";
	}

	private static String defaultContentType(@Nullable String contentType) {
		return hasText(contentType) ? Objects.requireNonNull(contentType).trim() : "application/octet-stream";
	}

	private static byte[] readFileBytes(MultipartFile file) {
		try {
			return file.getBytes();
		} catch (IOException e) {
			throw OmrErrorCode.OMR_FILE_READ_FAILED.toException(e.getMessage());
		}
	}

	private int resolveLatestOmrProgress(
		OmrProcessingStatus status,
		@Nullable String omrJobId,
		int fallbackProgress
	) {
		if (!isInProgress(status) || !hasText(omrJobId)) {
			return fallbackProgress;
		}

		try {
			Integer progress = omrClient.fetchJobStatus(Objects.requireNonNull(omrJobId)).progress();
			return progress != null ? normalizeProgress(progress) : fallbackProgress;
		} catch (Exception e) {
			log.warn("[OMR] SheetProject status progress 조회 실패: jobId={}", omrJobId, e);
			return fallbackProgress;
		}
	}

	private static boolean isInProgress(OmrProcessingStatus status) {
		return status == OmrProcessingStatus.PENDING || status == OmrProcessingStatus.PROCESSING;
	}

	private static int normalizeProgress(int progress) {
		return Math.max(0, Math.min(progress, 100));
	}
}
