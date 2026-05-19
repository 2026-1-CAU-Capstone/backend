package com.jazzify.backend.domain.solo.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.shared.omr.OmrClient;
import com.jazzify.backend.shared.omr.OmrProperties;
import com.jazzify.backend.domain.solo.dto.request.SheetDataRequest;

@NullMarked
class SoloOmrProcessorTest {

	private static final String SIMPLE_MUSIC_XML = """
		<?xml version="1.0" encoding="UTF-8"?>
		<score-partwise version="3.1">
		  <work>
		    <work-title>Solo OMR</work-title>
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
		        <pitch><step>E</step><octave>4</octave></pitch>
		        <duration>1</duration>
		        <type>quarter</type>
		      </note>
		    </measure>
		    <measure number="2">
		      <note>
		        <pitch><step>F</step><octave>4</octave></pitch>
		        <duration>1</duration>
		        <type>quarter</type>
		      </note>
		    </measure>
		  </part>
		</score-partwise>
		""";

	@Test
	void process_acceptsPdfFile() {
		boolean[] called = {false};
		SoloOmrProcessor processor = new SoloOmrProcessor(new OmrClient(new OmrProperties("http://unused")) {
			@Override
			public OmrRecognitionResult recognize(MultipartFile file) {
				called[0] = true;
				return new OmrRecognitionResult(SIMPLE_MUSIC_XML, Map.of("1", "Gm7  C7", "2", "Fmaj7"));
			}
		});
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"score.pdf",
			"application/pdf",
			"dummy".getBytes(StandardCharsets.UTF_8)
		);

		SheetDataRequest result = processor.process(file);

		assertThat(called[0]).isTrue();
		assertThat(result.measures())
			.extracting(it -> it.chord())
			.containsExactly("Gm7  C7", "Fmaj7");
	}

	@Test
	void process_buildsSheetDataUsingChordAssignments() {
		SoloOmrProcessor processor = new SoloOmrProcessor(new OmrClient(new OmrProperties("http://unused")) {
			@Override
			public OmrRecognitionResult recognize(MultipartFile file) {
				return new OmrRecognitionResult(SIMPLE_MUSIC_XML, Map.of(
					"1", "Gm7  C7",
					"2", "Fmaj7"
				));
			}
		});
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"score.jpeg",
			"image/jpeg",
			"dummy".getBytes(StandardCharsets.UTF_8)
		);

		SheetDataRequest result = processor.process(file);

		assertThat(result.measures())
			.extracting(it -> it.chord())
			.containsExactly("Gm7  C7", "Fmaj7");
	}
}

