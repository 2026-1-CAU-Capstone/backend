package com.jazzify.backend.domain.lick.dto.request;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@NullMarked
public record LickVideoRequest(
	/** 영상 플랫폼 내부 ID (예: YouTube videoId). 최대 255자. */
	@NotBlank @Size(max = 255) String videoId,

	/** 영상 재생 시작 시각 (초 단위). null이면 처음부터 재생. */
	@Nullable @Positive Double startSec,

	/** 영상 재생 종료 시각 (초 단위). null이면 끝까지 재생. */
	@Nullable @Positive Double endSec,

	/** 영상 전체 URL (YouTube, Spotify, SoundCloud 등). 최대 512자. */
	@NotBlank @Size(max = 512) String url
) {
}

