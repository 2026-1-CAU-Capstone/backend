package com.jazzify.backend.domain.chordproject.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.session.entity.Session;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.domain.MusicKey;
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

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "session_id")
	private @Nullable Session session;

	@OneToMany(mappedBy = "chordProject", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ChordInfo> chordInfos = new ArrayList<>();

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

