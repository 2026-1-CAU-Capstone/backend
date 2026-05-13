package com.jazzify.backend.domain.solo.entity;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.shared.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 솔로(Solo)에 연결된 영상 정보 엔티티.
 * <p>
 * 하나의 솔로에 여러 영상 구간(YouTube, Spotify, SoundCloud 등)을 연결할 수 있다.
 */
@Entity
@Table(name = "tb_solo_video")
@Getter
@NullMarked
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SoloVideo extends BaseEntity {

	/** 부모 솔로 */
	@OneToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "solo_id", nullable = false, unique = true)
	private Solo solo;

	/**
	 * 영상 플랫폼 내부 ID.
	 * YouTube의 경우 videoId (예: {@code dQw4w9WgXcQ}).
	 */
	@Column(name = "video_id", nullable = false, length = 255)
	private String videoId;

	/** 영상 재생 시작 시각 (초 단위). */
	@Column(name = "start_sec")
	private @Nullable Double startSec;

	/** 영상 재생 종료 시각 (초 단위). */
	@Column(name = "end_sec")
	private @Nullable Double endSec;

	/** 영상 전체 URL (YouTube, Spotify, SoundCloud 등). 최대 512자. */
	@Column(name = "url", nullable = false, length = 512)
	private String url;
}



