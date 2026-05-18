package com.jazzify.backend.domain.lick.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.core.omr.OmrClient;
import com.jazzify.backend.core.omr.OmrProperties;
import com.jazzify.backend.domain.lick.dto.request.SheetDataRequest;
import com.jazzify.backend.shared.exception.CustomException;

@NullMarked
class LickOmrProcessorTest {

	private static final String SIMPLE_MUSIC_XML = """
		<?xml version="1.0" encoding="UTF-8"?>
		<score-partwise version="3.1">
		  <work>
		    <work-title>Lick OMR</work-title>
		  </work>
		  <part-list>
		    <score-part id="P1"><part-name>Music</part-name></score-part>
		  </part-list>
		  <part id="P1">
		    <measure number="1">
		      <attributes>
		        <divisions>1</divisions>
		        <key><fifths>0</fifths></key>
		        <time><beats>4</beats><beat-type>4</beat-type></time>
		      </attributes>
		      <note>
		        <pitch><step>C</step><octave>4</octave></pitch>
		        <duration>1</duration>
		        <type>quarter</type>
		      </note>
		    </measure>
		    <measure number="2">
		      <note>
		        <pitch><step>D</step><octave>4</octave></pitch>
		        <duration>1</duration>
		        <type>quarter</type>
		      </note>
		    </measure>
		  </part>
		</score-partwise>
		""";

	@Test
	void process_rejectsPdfFile() {
		LickOmrProcessor processor = new LickOmrProcessor(new OmrClient(new OmrProperties("http://unused")) {
			@Override
			public OmrRecognitionResult recognize(MultipartFile file) {
				throw new AssertionError("PDF 파일은 OMR 서버 호출 전에 차단되어야 합니다.");
			}
		});
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"score.pdf",
			"application/pdf",
			"dummy".getBytes(StandardCharsets.UTF_8)
		);

		CustomException exception = assertThrows(CustomException.class, () -> processor.process(file));
		assertThat(exception.getCode()).isEqualTo("OMR_004");
	}

	@Test
	void process_buildsSheetDataUsingChordAssignments() {
		LickOmrProcessor processor = new LickOmrProcessor(new OmrClient(new OmrProperties("http://unused")) {
			@Override
			public OmrRecognitionResult recognize(MultipartFile file) {
				return new OmrRecognitionResult(SIMPLE_MUSIC_XML, Map.of(
					"1", "Dm7  G7",
					"2", "Cmaj7"
				));
			}
		});
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"score.PNG",
			"image/png",
			"dummy".getBytes(StandardCharsets.UTF_8)
		);

		SheetDataRequest result = processor.process(file);

		assertThat(result.measures())
			.extracting(it -> it.chord())
			.containsExactly("Dm7  G7", "Cmaj7");
	}
}


