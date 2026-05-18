package com.jazzify.backend.domain.lick.service.implementation;

import java.util.Locale;
import java.util.List;
import java.util.Set;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.core.omr.OmrClient;
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
 * 업로드된 악보 이미지(PNG/JPG/JPEG)를 검증하고 OMR 서버에 전송한 후,
 * 반환된 MusicXML과 chord assignments를 결합하여 {@link SheetDataRequest}(Lick 도메인 전용)로 변환한다.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class LickOmrProcessor {

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg");

	private final OmrClient omrClient;

	/**
	 * 파일을 검증 · OMR 인식 · MusicXML 파싱하여 {@link SheetDataRequest}를 반환한다.
	 *
	 * @param file 업로드된 악보 이미지
	 * @return 파싱된 악보 데이터 (Lick 도메인 SheetDataRequest)
	 * @throws CustomException 파일 검증 실패, OMR 호출 실패, 파싱 실패 시
	 */
	public SheetDataRequest process(MultipartFile file) {
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
		if (file.isEmpty()) {
			throw OmrErrorCode.OMR_FILE_EMPTY.toException();
		}

		String originalFilename = file.getOriginalFilename();
		String extension = extractExtension(originalFilename);
		if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
			throw OmrErrorCode.OMR_INVALID_FILE_TYPE.toException(
				originalFilename != null ? originalFilename : "unknown"
			);
		}
	}

	private static @Nullable String extractExtension(@Nullable String originalFilename) {
		if (originalFilename == null) {
			return null;
		}

		int lastDotIndex = originalFilename.lastIndexOf('.');
		if (lastDotIndex < 0 || lastDotIndex == originalFilename.length() - 1) {
			return null;
		}

		return originalFilename.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
	}

	private SheetDataRequest toSheetDataRequest(ParsedSheetData parsed) {
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

		return new SheetDataRequest(
			parsed.title(),
			parsed.composer(),
			parsed.key(),
			parsed.timeSignature(),
			parsed.tempo(),
			measures
		);
	}
}

