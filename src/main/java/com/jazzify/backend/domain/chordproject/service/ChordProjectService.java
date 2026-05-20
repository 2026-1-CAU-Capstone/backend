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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import com.jazzify.backend.domain.chordproject.event.ChordProjectOmrRequestedEvent;
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
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectOmrWriter;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectReader;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectWriter;
import com.jazzify.backend.domain.chordproject.util.ChordProjectMapper;
import com.jazzify.backend.domain.chordproject.util.IRealProChordParser;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.domain.user.service.implementation.UserReader;
import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.exception.code.ChordProjectErrorCode;
import com.jazzify.backend.shared.exception.code.OmrErrorCode;
import com.jazzify.backend.shared.omr.OmrFileValidator;

import lombok.RequiredArgsConstructor;

@NullMarked
@Service
@RequiredArgsConstructor
public class ChordProjectService {

	private static final String DEFAULT_PENDING_TITLE = "OMR Processing";
	private static final String DEFAULT_PENDING_TIME_SIGNATURE = "4/4";
	private static final MusicKey DEFAULT_PENDING_KEY = MusicKey.C_MAJOR;

	private final ChordProjectReader chordProjectReader;
	private final ChordProjectWriter chordProjectWriter;
	private final ChordProjectOmrWriter chordProjectOmrWriter;
	private final ApplicationEventPublisher eventPublisher;
	private final UserReader userReader;
	private final ChordInfoReader chordInfoReader;
	private final ChordInfoWriter chordInfoWriter;
	private final ChordAnalysisReader chordAnalysisReader;
	private final ChordAnalysisWriter chordAnalysisWriter;
	private final HarmonicAnalysisService harmonicAnalysisService;

	@Transactional
	public ChordProjectResponse create(UUID userPublicId, ChordProjectCreateRequest request) {
		User user = userReader.getByPublicId(userPublicId);
		ChordProject project = chordProjectWriter.create(request.title(), request.key(), request.timeSignature(), user);
		return ChordProjectMapper.toResponse(project);
	}

	@Transactional
	public ChordProjectOmrCreateResponse createFromOmr(
		UUID userPublicId,
		MultipartFile file,
		ChordProjectOmrCreateRequest request
	) {
		OmrFileValidator.validate(file);
		byte[] fileData = readFileBytes(file);
		User user = userReader.getByPublicId(userPublicId);

		String originalFilename = defaultFileName(file.getOriginalFilename());
		String contentType = defaultContentType(file.getContentType());
		String pendingTitle = hasText(request.title())
			? request.title().trim()
			: extractBaseName(originalFilename);
		MusicKey pendingKey = request.key() != null ? request.key() : DEFAULT_PENDING_KEY;
		String pendingTimeSignature = hasText(request.timeSignature())
			? request.timeSignature().trim()
			: DEFAULT_PENDING_TIME_SIGNATURE;

		ChordProject project = chordProjectOmrWriter.createPending(user, pendingTitle, pendingKey, pendingTimeSignature);
		UUID projectPublicId = Objects.requireNonNull(project.getPublicId());

		eventPublisher.publishEvent(new ChordProjectOmrRequestedEvent(
			projectPublicId,
			originalFilename,
			contentType,
			fileData,
			request.title(),
			request.key(),
			request.timeSignature()
		));

		return new ChordProjectOmrCreateResponse(ChordProjectMapper.toResponse(project), List.of());
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
		return ChordProjectMapper.toResponse(project);
	}

	@Transactional(readOnly = true)
	public ChordProjectOmrStatusResponse getOmrStatus(UUID userPublicId, UUID projectPublicId) {
		User user = userReader.getByPublicId(userPublicId);
		ChordProject project = chordProjectReader.getByPublicIdAndUser(projectPublicId, user);
		return ChordProjectMapper.toOmrStatusResponse(project);
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
}
