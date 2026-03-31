package com.jazzify.backend.domain.chordproject.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.analysis.service.HarmonicAnalysisService;
import com.jazzify.backend.domain.chordinfo.entity.ChordGroup;
import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.chordinfo.entity.ChordSection;
import com.jazzify.backend.domain.chordinfo.service.implementation.ChordAnalysisWriter;
import com.jazzify.backend.domain.chordinfo.service.implementation.ChordInfoReader;
import com.jazzify.backend.domain.chordinfo.service.implementation.ChordInfoWriter;
import com.jazzify.backend.domain.chordinfo.util.ChordInfoMapper;
import com.jazzify.backend.domain.chordproject.dto.request.AddChordsRequest;
import com.jazzify.backend.domain.chordproject.dto.request.ChordProjectCreateRequest;
import com.jazzify.backend.domain.chordproject.dto.request.ChordProjectUpdateRequest;
import com.jazzify.backend.domain.chordproject.dto.response.AnalysisResultResponse;
import com.jazzify.backend.domain.chordproject.dto.response.ChordInfoResponse;
import com.jazzify.backend.domain.chordproject.dto.response.ChordProjectResponse;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectReader;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectWriter;
import com.jazzify.backend.domain.chordproject.util.ChordProjectMapper;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.domain.user.service.implementation.UserReader;
import com.jazzify.backend.shared.exception.code.ChordProjectErrorCode;

import lombok.RequiredArgsConstructor;

@NullMarked
@Service
@RequiredArgsConstructor
public class ChordProjectService {

	private final ChordProjectReader chordProjectReader;
	private final ChordProjectWriter chordProjectWriter;
	private final UserReader userReader;
	private final ChordInfoReader chordInfoReader;
	private final ChordInfoWriter chordInfoWriter;
	private final ChordAnalysisWriter chordAnalysisWriter;
	private final HarmonicAnalysisService harmonicAnalysisService;

	@Transactional
	public ChordProjectResponse create(UUID userPublicId, ChordProjectCreateRequest request) {
		User user = userReader.getByPublicId(userPublicId);
		ChordProject project = chordProjectWriter.create(request.title(), request.key(), request.timeSignature(), user);
		return ChordProjectMapper.toResponse(project);
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

		List<ChordInfo> chordInfos = request.chords().stream()
			.map(entry -> ChordInfo.builder()
				.chord(entry.chord())
				.bar(entry.bar())
				.beat(entry.beat())
				.durationBeats(entry.durationBeats())
				.chordProject(project)
				.build())
			.toList();

		List<ChordInfo> saved = chordInfoWriter.saveAll(chordInfos);
		return saved.stream()
			.map(ChordInfoMapper::toChordInfoResponse)
			.toList();
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
}
