package com.jazzify.backend.domain.sheetproject.service.implementation;

import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.exception.code.OmrErrorCode;
import com.jazzify.backend.shared.musicxml.MusicXmlParser;
import com.jazzify.backend.shared.musicxml.ParsedMeasure;
import com.jazzify.backend.shared.musicxml.ParsedSheetData;
import com.jazzify.backend.shared.omr.OmrClient;

import lombok.RequiredArgsConstructor;

/**
 * OMR 서버로부터 받은 결과를 파싱하여 SheetProject 도메인 데이터로 변환한다.
 * <p>
 * 콜백 수신 후 {@link #processJobResult(String)}을 호출하면,
 * OMR 서버에서 MusicXML과 chord assignments를 가져와 파싱한다.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class SheetProjectOmrProcessor {

	private static final String DEFAULT_TITLE = "Untitled";
	private static final String DEFAULT_TIME_SIGNATURE = "4/4";

	private final OmrClient omrClient;

	/**
	 * OMR 서버에서 완료된 작업의 결과를 가져와 SheetProject 도메인 데이터로 변환한다.
	 *
	 * @param jobId OMR 서버에서 발급된 job ID (우리 측에서 projectPublicId 문자열로 제출)
	 * @return 파싱된 악보 데이터
	 * @throws com.jazzify.backend.shared.exception.CustomException OMR 결과 조회 또는 파싱 실패 시
	 */
	public SheetProjectOmrData processJobResult(String jobId) {
		String musicXml = omrClient.fetchMusicXml(jobId);
		Map<String, String> chordsByMeasureNumber = omrClient.fetchChordAssignments(jobId);

		ParsedSheetData parsed;
		try {
			parsed = MusicXmlParser.parse(musicXml, chordsByMeasureNumber);
		} catch (Exception e) {
			throw OmrErrorCode.OMR_PARSE_FAILED.toException(e.getMessage());
		}

		if (parsed.measures().isEmpty()) {
			throw OmrErrorCode.OMR_PARSE_FAILED.toException("인식된 마디가 없습니다.");
		}

		return new SheetProjectOmrData(
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

	public record SheetProjectOmrData(
		String title,
		@Nullable MusicKey key,
		String timeSignature,
		String progression
	) {
	}
}
