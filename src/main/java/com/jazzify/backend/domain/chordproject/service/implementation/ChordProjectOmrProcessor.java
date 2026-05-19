package com.jazzify.backend.domain.chordproject.service.implementation;

import java.util.stream.Collectors;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.exception.code.OmrErrorCode;
import com.jazzify.backend.shared.omr.OmrFileValidator;
import com.jazzify.backend.shared.musicxml.MusicXmlParser;
import com.jazzify.backend.shared.musicxml.ParsedMeasure;
import com.jazzify.backend.shared.musicxml.ParsedSheetData;
import com.jazzify.backend.shared.omr.OmrClient;

import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
public class ChordProjectOmrProcessor {

	private static final String DEFAULT_TITLE = "Untitled";
	private static final String DEFAULT_TIME_SIGNATURE = "4/4";

	private final OmrClient omrClient;

	public ChordProjectOmrData process(MultipartFile file) {
		validateFile(file);

		OmrClient.OmrRecognitionResult omrResult = omrClient.recognize(file);
		ParsedSheetData parsed;
		try {
			parsed = MusicXmlParser.parse(omrResult.musicXml(), omrResult.chordsByMeasureNumber());
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

	private void validateFile(MultipartFile file) {
		OmrFileValidator.validate(file);
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


