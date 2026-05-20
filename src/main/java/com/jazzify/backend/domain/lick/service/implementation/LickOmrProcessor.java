package com.jazzify.backend.domain.lick.service.implementation;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.shared.omr.OmrClient;
import com.jazzify.backend.shared.omr.OmrFileValidator;
import com.jazzify.backend.domain.lick.dto.request.MeasureRequest;
import com.jazzify.backend.domain.lick.dto.request.NoteInfoRequest;
import com.jazzify.backend.domain.lick.dto.request.SheetDataRequest;
import com.jazzify.backend.shared.exception.CustomException;
import com.jazzify.backend.shared.exception.code.OmrErrorCode;
import com.jazzify.backend.shared.musicxml.MusicXmlParser;
import com.jazzify.backend.shared.musicxml.ParsedSheetData;

import lombok.RequiredArgsConstructor;

/**
 * Lick 도메인 OMR 처리기.
 * <p>
	 * 업로드된 악보 파일(PNG/JPG/JPEG/PDF)을 검증하고 OMR 서버에 전송한 후,
 * 반환된 MusicXML과 chord assignments를 결합하여 {@link SheetDataRequest}(Lick 도메인 전용)로 변환한다.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class LickOmrProcessor {

	@NullMarked
	public record ProcessedSheetData(
		@Nullable String composer,
		SheetDataRequest sheetData
	) {
	}

	private final OmrClient omrClient;

	/**
	 * 파일을 검증 · OMR 인식 · MusicXML 파싱하여 {@link SheetDataRequest}를 반환한다.
	 *
	 * @param file 업로드된 악보 파일
	 * @return 파싱된 악보 데이터 (Lick 도메인 SheetDataRequest)
	 * @throws CustomException 파일 검증 실패, OMR 호출 실패, 파싱 실패 시
	 */
	public ProcessedSheetData process(MultipartFile file) {
		validateFile(file);

		OmrClient.OmrRecognitionResult omrResult = omrClient.recognize(file);

		ParsedSheetData parsed;
		try {
			parsed = MusicXmlParser.parse(omrResult.musicXml(), omrResult.chordsByMeasureNumber());
		} catch (Exception e) {
			throw OmrErrorCode.OMR_PARSE_FAILED.toException(e.getMessage());
		}

		return toSheetDataRequest(parsed);
	}

	// ─── Private ────────────────────────────────────────────────────────

	private void validateFile(MultipartFile file) {
		OmrFileValidator.validate(file);
	}

	private ProcessedSheetData toSheetDataRequest(ParsedSheetData parsed) {
		List<MeasureRequest> measures = parsed.measures().stream()
			.map(m -> new MeasureRequest(
				m.chord(),
				m.notes().stream()
					.map(n -> new NoteInfoRequest(
						n.keys(),
						n.duration(),
						n.accidentals(),
						null,  // tuplet — OMR 파싱에서 셋잇단 정보 미제공
						n.dotted() ? Boolean.TRUE : null,
						null,  // tie — OMR 파싱에서 타이 연결 정보 미제공
						null,  // gliss
						null   // beamBreak
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

