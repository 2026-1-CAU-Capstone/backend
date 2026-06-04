package com.jazzify.backend.domain.chordproject.service.implementation;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.exception.code.OmrErrorCode;
import com.jazzify.backend.shared.omr.OmrClient;

import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
public class ChordProjectOmrProcessor {

	private static final String DEFAULT_TITLE = "Untitled";

	private final OmrClient omrClient;

	public ChordProjectOmrData process(MultipartFile file) {
		throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException(
			"ChordProject 도메인은 아직 비동기 OMR 콜백 방식으로 전환되지 않았습니다."
		);
	}

	/**
	 * Chord-chart OMR job_id로 결과를 조회하여 ChordProjectOmrData로 변환한다.
	 */
	public ChordProjectOmrData processJobResult(String jobId) {
		OmrClient.ChordChartResult chordChart = omrClient.fetchChordChart(jobId);
		return new ChordProjectOmrData(
			DEFAULT_TITLE,
			null,
			chordChart.timeSignature(),
			chordChart.progression()
		);
	}

	public record ChordProjectOmrData(
		String title,
		@Nullable MusicKey key,
		String timeSignature,
		String progression
	) {
	}
}

