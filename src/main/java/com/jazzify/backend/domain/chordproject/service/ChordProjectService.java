package com.jazzify.backend.domain.chordproject.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.domain.analysis.dto.response.AnalysisExplanationResponse;
import com.jazzify.backend.domain.analysis.service.HarmonicAnalysisService;
import com.jazzify.backend.domain.chordinfo.entity.ChordGroup;
import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.chordinfo.entity.ChordSection;
import com.jazzify.backend.domain.chordinfo.service.implementation.ChordAnalysisReader;
import com.jazzify.backend.domain.chordinfo.service.implementation.ChordAnalysisWriter;
import com.jazzify.backend.domain.chordinfo.service.implementation.ChordInfoReader;
import com.jazzify.backend.domain.chordinfo.service.implementation.ChordInfoWriter;
import com.jazzify.backend.domain.chordinfo.util.ChordInfoMapper;
import com.jazzify.backend.domain.sheetproject.dto.request.OmrCallbackRequest;
import com.jazzify.backend.domain.chordproject.dto.request.AddChordsRequest;
import com.jazzify.backend.domain.chordproject.dto.request.ChordProjectCreateRequest;
import com.jazzify.backend.domain.chordproject.dto.request.ChordProjectOmrCreateRequest;
import com.jazzify.backend.domain.chordproject.dto.request.ChordProjectUpdateRequest;
import com.jazzify.backend.domain.chordproject.dto.response.AnalysisResultResponse;
import com.jazzify.backend.domain.chordproject.dto.response.ChordInfoResponse;
import com.jazzify.backend.domain.chordproject.dto.response.ChordProjectOmrCreateResponse;
import com.jazzify.backend.domain.chordproject.dto.response.ChordProjectOmrStatusResponse;
import com.jazzify.backend.domain.chordproject.dto.response.ChordProjectResponse;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.chordproject.model.ChordProjectOmrSourceType;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectOmrProcessor;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectOmrWriter;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectReader;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectWriter;
import com.jazzify.backend.domain.chordproject.util.ChordProjectMapper;
import com.jazzify.backend.domain.chordproject.util.IRealProChordParser;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.domain.user.service.implementation.UserReader;
import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.exception.CustomException;
import com.jazzify.backend.shared.exception.code.ChordProjectErrorCode;
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
public class ChordProjectService {

	private static final String DEFAULT_PENDING_TITLE = "OMR Processing";
	private static final String DEFAULT_PENDING_TIME_SIGNATURE = "4/4";
	private static final MusicKey DEFAULT_PENDING_KEY = MusicKey.C_MAJOR;

	private final ChordProjectReader chordProjectReader;
	private final ChordProjectWriter chordProjectWriter;
	private final ChordProjectOmrWriter chordProjectOmrWriter;
	private final ChordProjectOmrProcessor chordProjectOmrProcessor;
	private final TransactionTemplate transactionTemplate;
	private final UserReader userReader;
	private final ChordInfoReader chordInfoReader;
	private final ChordInfoWriter chordInfoWriter;
	private final ChordAnalysisReader chordAnalysisReader;
	private final ChordAnalysisWriter chordAnalysisWriter;
	private final HarmonicAnalysisService harmonicAnalysisService;
	private final OmrClient omrClient;
	private final OmrProperties omrProperties;

	@Transactional
	public ChordProjectResponse create(UUID userPublicId, ChordProjectCreateRequest request) {
		User user = userReader.getByPublicId(userPublicId);
		ChordProject project = chordProjectWriter.create(request.title(), request.key(), request.timeSignature(), user);
		return ChordProjectMapper.toResponse(project);
	}

	public ChordProjectOmrCreateResponse createFromOmr(
		UUID userPublicId,
		MultipartFile file,
		ChordProjectOmrCreateRequest request
	) {
		OmrFileValidator.validate(file);
		byte[] fileData = readFileBytes(file);

		String originalFilename = defaultFileName(file.getOriginalFilename());
		String contentType = defaultContentType(file.getContentType());
		String pendingTitle = hasText(request.title())
			? request.title().trim()
			: extractBaseName(originalFilename);
		MusicKey pendingKey = request.key() != null ? request.key() : DEFAULT_PENDING_KEY;
		String pendingTimeSignature = hasText(request.timeSignature())
			? request.timeSignature().trim()
			: DEFAULT_PENDING_TIME_SIGNATURE;
		ChordProjectOmrSourceType sourceType = ChordProjectOmrSourceType.from(request.sourceType());

		// 1단계: User 조회와 PENDING 엔티티 생성을 같은 트랜잭션 안에서 실행하고 커밋한다.
		ChordProject project = Objects.requireNonNull(
			transactionTemplate.execute(status -> {
				User user = userReader.getByPublicId(userPublicId);
				return chordProjectOmrWriter.createPending(
					user, pendingTitle, pendingKey, pendingTimeSignature,
					hasText(request.title()) ? request.title().trim() : null,
					request.key(),
					hasText(request.timeSignature()) ? request.timeSignature().trim() : null,
					sourceType
				);
			})
		);

		UUID projectPublicId = Objects.requireNonNull(project.getPublicId());

		// 2단계: 커밋 후 OMR 서버에 파일을 제출하고 job_id 응답을 확인한다.
		try {
			OmrClient.OmrSubmitResult result = submitChordProjectOmrJob(sourceType, fileData, originalFilename, projectPublicId.toString());
			chordProjectOmrWriter.storeJobIdAndMarkProcessing(projectPublicId, Objects.requireNonNull(result.jobId()), 10);
		} catch (CustomException e) {
			chordProjectOmrWriter.fail(projectPublicId, e.getMessage(), 0);
			throw e;
		} catch (Exception e) {
			chordProjectOmrWriter.fail(projectPublicId, e.getMessage(), 0);
			throw OmrErrorCode.OMR_SUBMIT_FAILED.toException(e.getMessage());
		}

		// 3단계: 최신 상태(PROCESSING)를 DB에서 다시 읽어 반환한다.
		ChordProject fresh = chordProjectReader.findByPublicId(projectPublicId)
			.orElseThrow(() -> OmrErrorCode.OMR_JOB_NOT_FOUND.toException("projectPublicId=" + projectPublicId));
		return new ChordProjectOmrCreateResponse(ChordProjectMapper.toResponse(fresh), List.of());
	}

	@Transactional(readOnly = true)
	public Page<ChordProjectResponse> getAll(UUID userPublicId, Pageable pageable) {
		User user = userReader.getByPublicId(userPublicId);
		return chordProjectReader.getAllByUser(user, pageable)
			.map(ChordProjectMapper::toResponse);
	}

	@Transactional(readOnly = true)
	public ChordProjectResponse getByPublicId(UUID userPublicId, UUID projectPublicId) {
		User user = userReader.getByPublicId(userPublicId);
		ChordProject project = chordProjectReader.getByPublicIdAndUser(projectPublicId, user);
		List<ChordInfo> chordInfos = chordInfoReader.getAllByChordProject(project);
		return ChordProjectMapper.toResponse(project, chordInfos);
	}

	@Transactional(readOnly = true)
	public ChordProjectOmrStatusResponse getOmrStatus(UUID userPublicId, UUID projectPublicId) {
		User user = userReader.getByPublicId(userPublicId);
		ChordProject project = chordProjectReader.getByPublicIdAndUser(projectPublicId, user);
		int progress = resolveLatestOmrProgress(project.getOmrStatus(), project.getOmrJobId(), project.getOmrProgress());
		return new ChordProjectOmrStatusResponse(
			Objects.requireNonNull(project.getPublicId()),
			project.getOmrStatus(),
			progress,
			project.getOmrFailureReason()
		);
	}

	@Transactional
	public ChordProjectResponse update(UUID userPublicId, UUID projectPublicId, ChordProjectUpdateRequest request) {
		User user = userReader.getByPublicId(userPublicId);
		ChordProject project = chordProjectReader.getByPublicIdAndUser(projectPublicId, user);
		project.update(request.title(), request.key());
		return ChordProjectMapper.toResponse(project);
	}

	@Transactional
	public void delete(UUID userPublicId, UUID projectPublicId) {
		User user = userReader.getByPublicId(userPublicId);
		ChordProject project = chordProjectReader.getByPublicIdAndUser(projectPublicId, user);
		chordProjectWriter.delete(project);
	}

	// ── 코드 정보 저장 ──

	@Transactional
	public List<ChordInfoResponse> addChords(UUID userPublicId, UUID projectPublicId, AddChordsRequest request) {
		User user = userReader.getByPublicId(userPublicId);
		ChordProject project = chordProjectReader.getByPublicIdAndUser(projectPublicId, user);

		// 기존 코드 정보 삭제 (덮어쓰기)
		chordInfoWriter.deleteAllByChordProject(project);

		// iRealPro 형식 문자열을 파싱하여 ChordInfo 리스트로 변환
		List<ChordInfo> chordInfos = IRealProChordParser.parse(
			request.progression(), project.getTimeSignature(), project);

		List<ChordInfo> saved = chordInfoWriter.saveAll(chordInfos);
		return saved.stream()
			.map(ChordInfoMapper::toChordInfoResponse)
			.toList();
	}

	// ── 분석 결과 조회 ──

	@Transactional(readOnly = true)
	public AnalysisResultResponse getAnalysisResult(UUID userPublicId, UUID projectPublicId) {
		User user = userReader.getByPublicId(userPublicId);
		ChordProject project = chordProjectReader.getByPublicIdAndUser(projectPublicId, user);

		if (project.getLastAnalyzedAt() == null) {
			throw ChordProjectErrorCode.CHORD_PROJECT_NOT_ANALYZED.toException();
		}

		List<ChordInfo> chordInfos = chordInfoReader.getAllByChordProject(project);
		List<ChordGroup> groups = chordAnalysisReader.getGroupsByProject(project);
		List<ChordSection> sections = chordAnalysisReader.getSectionsByProject(project);

		return ChordInfoMapper.toAnalysisResultResponse(project, chordInfos, groups, sections);
	}

	// ── 분석 실행 ──

	@Transactional
	@SuppressWarnings("unchecked")
	public AnalysisResultResponse analyze(UUID userPublicId, UUID projectPublicId) {
		User user = userReader.getByPublicId(userPublicId);
		ChordProject project = chordProjectReader.getByPublicIdAndUser(projectPublicId, user);

		List<ChordInfo> chordInfos = chordInfoReader.getAllByChordProject(project);
		if (chordInfos.isEmpty()) {
			throw ChordProjectErrorCode.CHORD_PROJECT_NO_CHORDS.toException();
		}

		// ChordInfo → 분석기 입력 텍스트 생성 (bar 기준 그룹화 → 마디별 "|" 구분)
		String text = buildProgressionText(chordInfos);
		String key = project.getKeySignature().getAnalysisKey();
		String title = project.getTitle();
		String timeSignature = project.getTimeSignature();

		// 분석 실행
		Map<String, Object> analysisResult = harmonicAnalysisService.analyze(text, key, title, timeSignature);

		// 분석 결과에서 ParsedChord 리스트 복원을 위해 재실행 대신 결과 Map에서 매핑
		// analyze()는 내부적으로 ParsedChord를 생성하므로, 인덱스 기반 매핑을 위해 파싱된 코드 목록 활용
		List<Map<String, Object>> chordDicts = (List<Map<String, Object>>) analysisResult.get("chords");
		List<Map<String, Object>> groups = (List<Map<String, Object>>) analysisResult.get("groups");
		List<Map<String, Object>> sections = (List<Map<String, Object>>) analysisResult.get("sections");

		// 기존 그룹/섹션 삭제
		chordAnalysisWriter.clearPreviousAnalysis(project);

		// ChordAnalysis 생성/갱신 (ChordInfo와 1:1)
		chordAnalysisWriter.saveAnalysisResults(chordInfos, chordDicts);

		// 그룹 저장
		List<ChordGroup> savedGroups = chordAnalysisWriter.saveGroups(project, groups, chordInfos);

		// 섹션 저장
		List<ChordSection> savedSections = chordAnalysisWriter.saveSections(project, sections);

		// 프로젝트 통계 업데이트
		chordAnalysisWriter.updateProjectStats(project, analysisResult);

		return ChordInfoMapper.toAnalysisResultResponse(project, chordInfos, savedGroups, savedSections);
	}

	// ── 자연어 설명 ──

	@Transactional(readOnly = true)
	public AnalysisExplanationResponse explain(UUID userPublicId, UUID projectPublicId) {
		User user = userReader.getByPublicId(userPublicId);
		ChordProject project = chordProjectReader.getByPublicIdAndUser(projectPublicId, user);

		List<ChordInfo> chordInfos = chordInfoReader.getAllByChordProject(project);
		if (chordInfos.isEmpty()) {
			throw ChordProjectErrorCode.CHORD_PROJECT_NO_CHORDS.toException();
		}

		String text = buildProgressionText(chordInfos);
		String key = project.getKeySignature().getAnalysisKey();
		String title = project.getTitle();
		String timeSignature = project.getTimeSignature();

		return harmonicAnalysisService.explain(text, key, title, timeSignature);
	}

	// ── private helpers ──

	/**
	 * ChordInfo 목록을 분석기 입력 텍스트 형식으로 변환한다.
	 * bar 기준으로 그룹화하여 "Dm7 G7 | Cmaj7 | Am7 D7" 형태로 생성.
	 */
	private String buildProgressionText(List<ChordInfo> chordInfos) {
		// bar 순서로 그룹화
		Map<Integer, List<ChordInfo>> byBar = chordInfos.stream()
			.collect(Collectors.groupingBy(ChordInfo::getBar, TreeMap::new, Collectors.toList()));

		List<String> bars = new ArrayList<>();
		for (Map.Entry<Integer, List<ChordInfo>> entry : byBar.entrySet()) {
			List<String> chords = entry.getValue().stream()
				.map(ci -> ci.getChord() != null ? ci.getChord() : "N.C.")
				.toList();
			bars.add(String.join(" ", chords));
		}
		return String.join(" | ", bars);
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

	// ── OMR 콜백 처리 ──

	/**
	 * OMR 서버가 처리 완료 후 보내는 콜백을 처리한다.
	 */
	@Transactional
	public void handleOmrCallback(String callbackApiKey, OmrCallbackRequest callbackRequest) {
		validateCallbackApiKey(callbackApiKey);

		String jobId = callbackRequest.jobId();
		UUID projectPublicId;
		try {
			projectPublicId = UUID.fromString(jobId);
		} catch (IllegalArgumentException e) {
			throw OmrErrorCode.OMR_JOB_NOT_FOUND.toException("유효하지 않은 job_id 형식입니다: " + jobId);
		}

		ChordProject project = chordProjectReader.findByPublicId(projectPublicId)
			.orElseThrow(() -> OmrErrorCode.OMR_JOB_NOT_FOUND.toException("job_id=" + jobId));

		if (callbackRequest.isCompleted()) {
			UUID publicId = Objects.requireNonNull(project.getPublicId());
			chordProjectOmrWriter.markProcessing(publicId, 80);

			ChordProjectOmrSourceType sourceType = resolveCallbackSourceType(project);
			ChordProjectOmrProcessor.ChordProjectOmrData omrData = chordProjectOmrProcessor.processJobResult(jobId, sourceType);

			String title = project.getOmrRequestedTitle() != null ? project.getOmrRequestedTitle() : omrData.title();
			MusicKey key = project.getOmrRequestedKey() != null ? project.getOmrRequestedKey() : omrData.key();
			if (key == null) {
				key = project.getKeySignature();
			}
			if (key == null) {
				chordProjectOmrWriter.fail(publicId, ChordProjectErrorCode.CHORD_PROJECT_KEY_REQUIRED.getMessage(), 80);
				return;
			}
			String timeSignature = project.getOmrRequestedTimeSignature() != null
				? project.getOmrRequestedTimeSignature()
				: omrData.timeSignature();

			chordProjectOmrWriter.complete(
				publicId,
				title,
				key,
				timeSignature,
				omrData.beatsPerBar(),
				omrData.chords()
			);

		} else if (callbackRequest.isFailed()) {
			String errorMsg = hasText(callbackRequest.error()) ? callbackRequest.error()
				: (hasText(callbackRequest.message()) ? callbackRequest.message() : "OMR 처리 실패");
			chordProjectOmrWriter.fail(Objects.requireNonNull(project.getPublicId()), errorMsg, 0);
		}
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

	private static ChordProjectOmrSourceType resolveCallbackSourceType(ChordProject project) {
		if (project.getOmrSourceType() != null) {
			return project.getOmrSourceType();
		}
		return ChordProjectOmrSourceType.CHORD_CHART;
	}

	private OmrClient.OmrSubmitResult submitChordProjectOmrJob(
		ChordProjectOmrSourceType sourceType,
		byte[] fileData,
		String originalFilename,
		String jobId
	) {
		return switch (sourceType) {
			case SHEET_MUSIC -> omrClient.submitChordSheetMusicJob(
				fileData, originalFilename, jobId, OmrCallbackDomain.CHORD_PROJECT
			);
			case CHORD_CHART -> omrClient.submitChordChartJob(
				fileData, originalFilename, jobId, OmrCallbackDomain.CHORD_PROJECT
			);
		};
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
			log.warn("[OMR] ChordProject status progress 조회 실패: jobId={}", omrJobId, e);
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
