package com.jazzify.backend.domain.chordproject.service.implementation;

import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.exception.code.OmrErrorCode;
import com.jazzify.backend.shared.musicxml.MusicXmlParser;
import com.jazzify.backend.shared.musicxml.ParsedMeasure;
import com.jazzify.backend.shared.musicxml.ParsedSheetData;
import com.jazzify.backend.shared.omr.OmrClient;

import lombok.RequiredArgsConstructor;

// TODO: SheetProject 도메인처럼 콜백 기반 비동기 흐름으로 전환 필요.
//       현재 OMR API는 동기 처리를 지원하지 않으므로 이 메서드는 사용 불가 상태다.
@NullMarked
@Component
@RequiredArgsConstructor
public class ChordProjectOmrProcessor {

	private static final String DEFAULT_TITLE = "Untitled";
	private static final String DEFAULT_TIME_SIGNATURE = "4/4";

	private final OmrClient omrClient;

	public ChordProjectOmrData process(MultipartFile file) {
		throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException(
			"ChordProject 도메인은 아직 비동기 OMR 콜백 방식으로 전환되지 않았습니다."
		);
	}

	/**
	 * OMR job_id로 결과를 조회하여 ChordProjectOmrData로 변환한다.
	 * 콜백 기반 비동기 흐름 전환 시 이 메서드를 사용할 것.
	 */
	public ChordProjectOmrData processJobResult(String jobId) {
		String musicXml = omrClient.fetchMusicXml(jobId);
		java.util.Map<String, String> chordsByMeasureNumber = omrClient.fetchChordAssignments(jobId);

		ParsedSheetData parsed;
		try {
			parsed = MusicXmlParser.parse(musicXml, chordsByMeasureNumber);
		} catch (Exception e) {
			throw OmrErrorCode.OMR_PARSE_FAILED.toException(e.getMessage());
		}

		if (parsed.measures().isEmpty()) {
			throw OmrErrorCode.OMR_PARSE_FAILED.toException("인식된 마디가 없습니다.");
		}

		return new ChordProjectOmrData(
			parsed.title().isBlank() ? DEFAULT_TITLE : parsed.title(),
			MusicKey.fromAnalysisKey(parsed.key()),
			parsed.timeSignature().isBlank() ? DEFAULT_TIME_SIGNATURE : parsed.timeSignature(),
			toProgression(parsed)
		);
	}

	private String toProgression(ParsedSheetData parsed) {
		return parsed.measures().stream()
			.map(this::toBarToken)
			.collect(Collectors.joining(" | "));
	}

	private String toBarToken(ParsedMeasure measure) {
		if (measure.chord() == null || measure.chord().isBlank()) {
			return "N.C.";
		}
		return measure.chord().trim().replaceAll("\\s+", " ");
	}

	public record ChordProjectOmrData(
		String title,
		@Nullable MusicKey key,
		String timeSignature,
		String progression
	) {
	}
}


