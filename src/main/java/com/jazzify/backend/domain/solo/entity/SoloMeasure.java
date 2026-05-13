package com.jazzify.backend.domain.solo.entity;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.shared.persistence.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 솔로(Solo) 한 마디를 나타내는 엔티티.
 * <p>
 * sheetData.measures 배열의 각 요소를 DB 행으로 저장한다.
 * <pre>
 * {
 *   "chord": "D-7",
 *   "notes": [ ... ]
 * }
 * </pre>
 */
@Entity
@Table(name = "tb_solo_measure")
@Getter
@NullMarked
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SoloMeasure extends BaseEntity {

	/** 부모 솔로 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "solo_id", nullable = false)
	private Solo solo;

	/** 마디 순서 (0-based) */
	@Column(nullable = false)
	private int measureIndex;

	/**
	 * 마디의 코드 심볼.
	 * 한 마디에 두 코드가 있으면 두 칸 이상 공백으로 구분한다 (예: {@code "D-7  G7"}).
	 * 코드가 없으면 null.
	 */
	@Column(length = 100)
	private @Nullable String chord;

	/** 마디에 속한 음표 목록 (noteIndex 오름차순) */
	@Builder.Default
	@OneToMany(mappedBy = "measure", cascade = CascadeType.ALL, orphanRemoval = true)
	@OrderBy("noteIndex ASC")
	private List<SoloNote> notes = new ArrayList<>();

	// ─── Helpers ────────────────────────────────────────────────────────

	/** 음표를 이 마디에 추가한다. 양방향 관계를 동기화한다. */
	public void addNote(SoloNote note) {
		notes.add(note);
	}
}

