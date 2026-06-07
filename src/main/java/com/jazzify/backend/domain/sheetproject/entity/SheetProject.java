package com.jazzify.backend.domain.sheetproject.entity;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.session.entity.Session;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.omr.OmrProcessingStatus;
import com.jazzify.backend.shared.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_sheet_project")
@Getter
@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SheetProject extends BaseEntity {

	@Column(nullable = false)
	private String title;

	@Enumerated(EnumType.STRING)
	@Column(length = 30)
	private @Nullable MusicKey keySignature;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private OmrProcessingStatus omrStatus = OmrProcessingStatus.COMPLETED;

	@Column(nullable = false)
	private int omrProgress = 100;

	@Column(length = 500)
	private @Nullable String omrFailureReason;

	/** OMR 서버에서 발급된 job ID. 비동기 콜백 수신 시 역조회에 사용된다. */
	@Column(length = 128)
	private @Nullable String omrJobId;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "file_id", nullable = false, unique = true)
	private SheetFile sheetFile;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "session_id")
	private @Nullable Session session;

	@OneToMany(mappedBy = "sheetProject", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ChordInfo> chordInfos = new ArrayList<>();

	@Builder
	public SheetProject(String title, @Nullable MusicKey keySignature, User user,
		SheetFile sheetFile, @Nullable Session session) {
		this.title = title;
		this.keySignature = keySignature;
		this.user = user;
		this.sheetFile = sheetFile;
		this.session = session;
	}

	public void update(String title, @Nullable MusicKey key) {
		this.title = title;
		this.keySignature = key;
	}

	public void updateOmrResolvedFields(String title, @Nullable MusicKey key) {
		this.title = title;
		this.keySignature = key;
	}

	public void markOmrQueued() {
		this.omrStatus = OmrProcessingStatus.PENDING;
		this.omrProgress = 0;
		this.omrFailureReason = null;
		this.omrJobId = null;
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
}
