package com.jazzify.backend.domain.chordproject.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.chordinfo.entity.ChordGroup;
import com.jazzify.backend.domain.chordinfo.entity.ChordSection;
import com.jazzify.backend.domain.chordproject.model.ChordProjectOmrSourceType;
import com.jazzify.backend.domain.session.entity.Session;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.omr.OmrProcessingStatus;
import com.jazzify.backend.shared.persistence.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_chord_project")
@Getter
@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChordProject extends BaseEntity {

	@Column(nullable = false)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MusicKey keySignature;

	@Column(nullable = false, length = 10)
	private String timeSignature;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OmrProcessingStatus omrStatus = OmrProcessingStatus.COMPLETED;

	@Column(nullable = false)
	private int omrProgress = 100;

	@Column(length = 500)
	private @Nullable String omrFailureReason;

	/** OMR 서버에서 발급된 job ID. */
	@Column(length = 128)
	private @Nullable String omrJobId;

	/** 사용자가 직접 입력한 제목 (null이면 OMR 파싱값 사용). */
	@Column(length = 255)
	private @Nullable String omrRequestedTitle;

	/** 사용자가 직접 입력한 조성 (null이면 OMR 파싱값 사용). */
	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private @Nullable MusicKey omrRequestedKey;

	/** 사용자가 직접 입력한 박자 (null이면 OMR 파싱값 사용). */
	@Column(length = 10)
	private @Nullable String omrRequestedTimeSignature;

	/** ChordProject OMR 입력 유형. null이면 기존 데이터 호환을 위해 chord-chart로 해석한다. */
	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private @Nullable ChordProjectOmrSourceType omrSourceType;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "session_id")
	private @Nullable Session session;

	@OneToMany(mappedBy = "chordProject", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ChordInfo> chordInfos = new ArrayList<>();

	@OneToMany(mappedBy = "chordProject", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ChordGroup> chordGroups = new ArrayList<>();

	@OneToMany(mappedBy = "chordProject", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ChordSection> chordSections = new ArrayList<>();

	// ── 분석 메타데이터 ──
	@Column
	private @Nullable LocalDateTime lastAnalyzedAt;

	@Column
	private @Nullable Integer totalChords;

	@Column
	private @Nullable Integer highConfidenceCount;

	@Column
	private @Nullable Integer ambiguousCount;

	@Column
	private @Nullable Double meanAmbiguityScore;

	@Column
	private @Nullable Double maxAmbiguityScore;

	@Builder
	public ChordProject(String title, MusicKey keySignature, String timeSignature,
		User user, @Nullable Session session) {
		this.title = title;
		this.keySignature = keySignature;
		this.timeSignature = timeSignature;
		this.user = user;
		this.session = session;
	}

	public void update(String title, MusicKey key) {
		this.title = title;
		this.keySignature = key;
	}

	public void updateOmrResolvedFields(String title, MusicKey key, String timeSignature) {
		this.title = title;
		this.keySignature = key;
		this.timeSignature = timeSignature;
	}

	public void markOmrQueued(
		@Nullable String requestedTitle,
		@Nullable MusicKey requestedKey,
		@Nullable String requestedTimeSignature,
		ChordProjectOmrSourceType sourceType
	) {
		this.omrStatus = OmrProcessingStatus.PENDING;
		this.omrProgress = 0;
		this.omrFailureReason = null;
		this.omrJobId = null;
		this.omrRequestedTitle = requestedTitle;
		this.omrRequestedKey = requestedKey;
		this.omrRequestedTimeSignature = requestedTimeSignature;
		this.omrSourceType = sourceType;
	}

	public void storeOmrJobId(String jobId) {
		this.omrJobId = jobId;
	}

	public void markOmrProcessing(int progress) {
		this.omrStatus = OmrProcessingStatus.PROCESSING;
		this.omrProgress = progress;
		this.omrFailureReason = null;
	}

	public void markOmrCompleted() {
		this.omrStatus = OmrProcessingStatus.COMPLETED;
		this.omrProgress = 100;
		this.omrFailureReason = null;
	}

	public void markOmrFailed(String failureReason, int progress) {
		this.omrStatus = OmrProcessingStatus.FAILED;
		this.omrProgress = progress;
		this.omrFailureReason = failureReason;
	}

	public void updateAnalysisStats(int totalChords, int highConfidenceCount,
		int ambiguousCount, double meanAmbiguityScore, double maxAmbiguityScore) {
		this.lastAnalyzedAt = LocalDateTime.now();
		this.totalChords = totalChords;
		this.highConfidenceCount = highConfidenceCount;
		this.ambiguousCount = ambiguousCount;
		this.meanAmbiguityScore = meanAmbiguityScore;
		this.maxAmbiguityScore = maxAmbiguityScore;
	}
}
