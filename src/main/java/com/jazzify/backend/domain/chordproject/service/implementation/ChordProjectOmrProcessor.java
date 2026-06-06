package com.jazzify.backend.domain.chordproject.service.implementation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.domain.chordproject.model.ChordProjectOmrChord;
import com.jazzify.backend.domain.chordproject.model.ChordProjectOmrSourceType;
import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.exception.code.OmrErrorCode;
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

	private final OmrClient omrClient;

	public ChordProjectOmrData process(MultipartFile file) {
		throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException(
			"ChordProject 도메인은 아직 비동기 OMR 콜백 방식으로 전환되지 않았습니다."
		);
	}

	/**
	 * ChordProject OMR job_id와 입력 유형으로 결과를 조회하여 ChordProjectOmrData로 변환한다.
	 */
	public ChordProjectOmrData processJobResult(String jobId) {
		return processJobResult(jobId, ChordProjectOmrSourceType.CHORD_CHART);
	}

	public ChordProjectOmrData processJobResult(String jobId, ChordProjectOmrSourceType sourceType) {
		return switch (sourceType) {
			case SHEET_MUSIC -> processSheetMusicJobResult(jobId);
			case CHORD_CHART -> processChordChartJobResult(jobId);
		};
	}

	private ChordProjectOmrData processChordChartJobResult(String jobId) {
		OmrClient.ChordChartResult chordChart = omrClient.fetchChordChart(jobId);
		return new ChordProjectOmrData(
			chordChart.title(),
			null,
			chordChart.timeSignature(),
			chordChart.beatsPerBar(),
			chordChart.chords().stream()
				.map(chord -> new ChordProjectOmrChord(
					chord.bar(),
					chord.chord(),
					chord.beat(),
					chord.durationBeats()
				))
				.toList()
		);
	}

	private ChordProjectOmrData processSheetMusicJobResult(String jobId) {
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

		String timeSignature = parsed.timeSignature().isBlank() ? "4/4" : parsed.timeSignature();
		int beatsPerBar = extractBeatsPerBar(timeSignature);
		return new ChordProjectOmrData(
			parsed.title().isBlank() ? DEFAULT_TITLE : parsed.title(),
			MusicKey.fromAnalysisKey(parsed.key()),
			timeSignature,
			beatsPerBar,
			toOmrChords(parsed, beatsPerBar)
		);
	}

	private List<ChordProjectOmrChord> toOmrChords(ParsedSheetData parsed, int beatsPerBar) {
		List<ChordProjectOmrChord> result = new ArrayList<>();
		for (int measureIndex = 0; measureIndex < parsed.measures().size(); measureIndex++) {
			ParsedMeasure measure = parsed.measures().get(measureIndex);
			int bar = measureIndex + 1;
			if (measure.chord() == null || measure.chord().isBlank()) {
				result.add(new ChordProjectOmrChord(bar, null, 1.0, beatsPerBar));
				continue;
			}

			String[] tokens = measure.chord().trim().split("\\s+");
			int baseDuration = beatsPerBar / tokens.length;
			int remainder = beatsPerBar % tokens.length;
			double currentBeat = 1.0;
			for (int i = 0; i < tokens.length; i++) {
				double duration = baseDuration + (i < remainder ? 1 : 0);
				String chord = "N.C.".equalsIgnoreCase(tokens[i]) ? null : tokens[i];
				result.add(new ChordProjectOmrChord(bar, chord, currentBeat, duration));
				currentBeat += duration;
			}
		}
		return List.copyOf(result);
	}

	private int extractBeatsPerBar(String timeSignature) {
		try {
			int beatsPerBar = Integer.parseInt(timeSignature.split("/")[0].trim());
			return beatsPerBar > 0 ? beatsPerBar : 4;
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			return 4;
		}
	}

	public record ChordProjectOmrData(
		String title,
		@Nullable MusicKey key,
		String timeSignature,
		int beatsPerBar,
		List<ChordProjectOmrChord> chords
	) {
	}
}
