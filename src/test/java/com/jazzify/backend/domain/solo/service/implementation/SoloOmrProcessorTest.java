package com.jazzify.backend.domain.solo.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.jazzify.backend.shared.exception.CustomException;
import com.jazzify.backend.shared.omr.OmrClient;
import com.jazzify.backend.shared.omr.OmrProperties;

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
	void processJobResult_buildsSheetDataUsingChordAssignments() {
		String[] capturedJobId = {null};
		SoloOmrProcessor processor = new SoloOmrProcessor(new OmrClient(new OmrProperties("http://unused", null, null, null)) {
			@Override
			public String fetchMusicXml(String jobId) {
				capturedJobId[0] = jobId;
				return SIMPLE_MUSIC_XML;
			}

			@Override
			public Map<String, String> fetchChordAssignments(String jobId) {
				return Map.of("1", "Gm7  C7", "2", "Fmaj7");
			}
		});

		SoloOmrProcessor.ProcessedSheetData result = processor.processJobResult("job-123");

		assertThat(capturedJobId[0]).isEqualTo("job-123");
		assertThat(result.sheetData().measures())
			.extracting(it -> it.chord())
			.containsExactly("Gm7  C7", "Fmaj7");
	}

	@Test
	void processJobResult_keepsOnlySafelyMappedMeasuresWhenAssignmentsArePartial() {
		SoloOmrProcessor processor = new SoloOmrProcessor(new OmrClient(new OmrProperties("http://unused", null, null, null)) {
			@Override
			public String fetchMusicXml(String jobId) {
				return SIMPLE_MUSIC_XML;
			}

			@Override
			public Map<String, String> fetchChordAssignments(String jobId) {
				return Map.of("1", "Gm7  C7");
			}
		});

		SoloOmrProcessor.ProcessedSheetData result = processor.processJobResult("job-456");

		assertThat(result.sheetData().measures())
			.extracting(it -> it.chord())
			.containsExactly("Gm7  C7", null);
	}

	@Test
	void process_throwsBecauseSyncOmarPathIsNoLongerSupported() {
		SoloOmrProcessor processor = new SoloOmrProcessor(new OmrClient(new OmrProperties("http://unused", null, null, null)));
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"score.png",
			"image/png",
			"dummy".getBytes(StandardCharsets.UTF_8)
		);

		CustomException exception = assertThrows(CustomException.class, () -> processor.process(file));
		assertThat(exception.getCode()).isEqualTo("OMR_002");
	}
}
