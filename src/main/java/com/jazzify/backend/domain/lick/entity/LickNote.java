package com.jazzify.backend.domain.lick.entity;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.shared.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 릭(Lick) 한 마디 안의 음표/쉼표 하나를 나타내는 엔티티.
 * <p>
 * 과거에는 {@code sheetData.measures[i].notes[j]} 각 요소를 DB 행으로 저장했으나,
 * 현재는 {@link Lick}의 {@code sheetDataJson} 반정규화 전환 이후
 * 기존 데이터 마이그레이션 및 레거시 호환 용도로만 유지된다.
 *
 * <h3>주요 필드</h3>
 * <ul>
 *   <li>{@code keys}        – VexFlow 음표 위치 (JSON 배열 텍스트). 예: {@code ["d/5"]}
 *                             단음은 요소 1개, 화음은 복수. 쉼표이면 {@code ["b/4"]} 고정.</li>
 *   <li>{@code duration}    – 음가 코드. 예: {@code "8"}, {@code "q"}, {@code "8r"}, {@code "16"}
 *                             {@code "r"} 접미사이면 쉼표.</li>
 *   <li>{@code dotted}      – 점음표 여부.</li>
 *   <li>{@code tuplet}      – 셋잇단음표 그룹 크기. 보통 {@code 3} 또는 {@code null}.</li>
 *   <li>{@code tie}         – 다음 음표로 타이 연결 여부.</li>
 *   <li>{@code gliss}       – 다음 음표까지 글리산도 여부.</li>
 *   <li>{@code beamBreak}   – 이 음표 이후 빔 강제 분리 여부.</li>
 *   <li>{@code accidentals} – 임시표 맵 (JSON 오브젝트 텍스트). 예: {@code {"0":"b"}}</li>
 * </ul>
 */
@Entity
@Table(name = "tb_lick_note")
@Getter
@NullMarked
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LickNote extends BaseEntity {

	/** 부모 마디 */
	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "measure_id", nullable = false)
	private LickMeasure measure;

	/** 마디 내 음표 순서 (0-based) */
	@Column(nullable = false)
	private int noteIndex;

	// ─── 음표 기본 정보 ──────────────────────────────────────────────────

	/**
	 * VexFlow keys 배열을 JSON TEXT로 저장한다.
	 * 예: {@code ["d/5"]} / {@code ["g/4","b/4","d/5"]}
	 * 쉼표(rest)이면 {@code ["b/4"]}.
	 */
	@Lob
	@Column(name = "note_keys", nullable = false, columnDefinition = "TEXT")
	private String keys;

	/**
	 * 음가 코드.
	 * 음표: {@code "w"}, {@code "h"}, {@code "q"}, {@code "8"}, {@code "16"}
	 * 쉼표: {@code "wr"}, {@code "hr"}, {@code "qr"}, {@code "8r"}, {@code "16r"}
	 */
	@Column(nullable = false, length = 10)
	private String duration;

	// ─── 수식 플래그 ─────────────────────────────────────────────────────

	/** 점음표 여부. {@code duration} + 0.5배 길이 추가. */
	@Builder.Default
	@Column(nullable = false)
	private boolean dotted = false;

	/**
	 * 셋잇단음표 그룹 크기.
	 * {@code 3} = 8분/16분 셋잇단. {@code null} = 일반 음표.
	 */
	@Column
	private @Nullable Integer tuplet;

	/** 다음 음표로 타이 연결 여부. */
	@Builder.Default
	@Column(nullable = false)
	private boolean tie = false;

	/** 다음 음표까지 글리산도 여부. */
	@Builder.Default
	@Column(nullable = false)
	private boolean gliss = false;

	/** 이 음표 이후 빔 강제 분리 여부. */
	@Builder.Default
	@Column(nullable = false)
	private boolean beamBreak = false;

	/**
	 * 임시표 맵을 JSON TEXT로 저장.
	 * 예: {@code {"0":"b"}} (♭), {@code {"0":"#"}} (♯), {@code {"0":"n"}} (제자리표)
	 * 화음이면 {@code {"0":"b","1":"#"}} 형태 가능.
	 * 임시표 없으면 {@code null}.
	 */
	@Column(columnDefinition = "TEXT")
	private @Nullable String accidentals;

	// ─── 편의 메서드 ─────────────────────────────────────────────────────

	/** 이 음표가 쉼표인지 여부. duration 끝이 "r"이면 쉼표. */
	public boolean isRest() {
		return duration.endsWith("r");
	}
}

