package com.jazzify.backend.domain.solo.service.implementation;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.shared.omr.OmrClient;
import com.jazzify.backend.domain.solo.dto.request.MeasureRequest;
import com.jazzify.backend.domain.solo.dto.request.NoteInfoRequest;
import com.jazzify.backend.domain.solo.dto.request.SheetDataRequest;
import com.jazzify.backend.shared.exception.CustomException;
import com.jazzify.backend.shared.exception.code.OmrErrorCode;
import com.jazzify.backend.shared.musicxml.MusicXmlParser;
import com.jazzify.backend.shared.musicxml.ParsedSheetData;

import lombok.RequiredArgsConstructor;

/**
 * Solo 도메인 OMR 처리기.
 * <p>
 * TODO: SheetProject 도메인처럼 콜백 기반 비동기 흐름으로 전환 필요.
 *       현재 OMR API는 동기 처리를 지원하지 않으므로 {@link #process(MultipartFile)}는 사용 불가 상태다.
 *       콜백 전환 시 {@link #processJobResult(String)}를 사용할 것.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class SoloOmrProcessor {

	@NullMarked
	public record ProcessedSheetData(
		@Nullable String composer,
		SheetDataRequest sheetData
	) {
	}

	private final OmrClient omrClient;

	/**
	 * @deprecated OMR API가 비동기로 전환되어 동기 호출이 불가능합니다.
	 *             콜백 기반 비동기 흐름으로 전환 후 {@link #processJobResult(String)}을 사용하세요.
	 */
	@Deprecated
	public ProcessedSheetData process(MultipartFile file) {
		throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException(
			"Solo 도메인은 아직 비동기 OMR 콜백 방식으로 전환되지 않았습니다."
		);
	}

	/**
	 * OMR job_id로 결과를 조회하여 {@link SheetDataRequest}를 반환한다.
	 * 콜백 기반 비동기 흐름 전환 시 이 메서드를 사용할 것.
	 *
	 * @param jobId OMR 서버 job ID
	 * @return 파싱된 악보 데이터
	 * @throws CustomException OMR 결과 조회 실패, 파싱 실패 시
	 */
	public ProcessedSheetData processJobResult(String jobId) {
		String musicXml = omrClient.fetchMusicXml(jobId);
		java.util.Map<String, String> chordsByMeasureNumber = omrClient.fetchChordAssignments(jobId);

		ParsedSheetData parsed;
		try {
			parsed = MusicXmlParser.parse(musicXml, chordsByMeasureNumber);
		} catch (Exception e) {
			throw OmrErrorCode.OMR_PARSE_FAILED.toException(e.getMessage());
		}

		return toSheetDataRequest(parsed);
	}

	// ─── Private ────────────────────────────────────────────────────────

	private ProcessedSheetData toSheetDataRequest(ParsedSheetData parsed) {
		List<MeasureRequest> measures = parsed.measures().stream()
			.map(m -> new MeasureRequest(
				m.chord(),
				m.notes().stream()
					.map(n -> new NoteInfoRequest(
						n.keys(),
						n.duration(),
						n.accidentals(),
						null,
						n.dotted() ? Boolean.TRUE : null,
						null,
						null,
						null
					))
					.toList()
			))
			.toList();

		return new ProcessedSheetData(
			parsed.composer(),
			new SheetDataRequest(
				parsed.title(),
				parsed.key(),
				parsed.timeSignature(),
				parsed.tempo(),
				measures
			)
		);
	}
}
