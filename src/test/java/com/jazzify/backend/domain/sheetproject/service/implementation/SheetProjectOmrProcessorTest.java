package com.jazzify.backend.domain.sheetproject.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.omr.OmrClient;
import com.jazzify.backend.shared.omr.OmrProperties;

@NullMarked
class SheetProjectOmrProcessorTest {

	private static final String SIMPLE_MUSIC_XML = """
		<?xml version="1.0" encoding="UTF-8"?>
		<score-partwise version="3.1">
		  <work>
		    <work-title>Autumn Leaves</work-title>
		  </work>
		  <part-list>
		    <score-part id="P1"><part-name>Music</part-name></score-part>
		  </part-list>
		  <part id="P1">
		    <measure number="1">
		      <attributes>
		        <divisions>1</divisions>
		        <key><fifths>0</fifths><mode>major</mode></key>
		        <time><beats>3</beats><beat-type>4</beat-type></time>
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
	void processJobResult_fetchesMusicXmlAndAssignments() {
		String[] capturedJobId = {null};
		SheetProjectOmrProcessor processor = new SheetProjectOmrProcessor(new OmrClient(new OmrProperties("http://unused", null, null, null)) {
			@Override
			public String fetchMusicXml(String jobId) {
				capturedJobId[0] = jobId;
				return SIMPLE_MUSIC_XML;
			}

			@Override
			public Map<String, String> fetchChordAssignments(String jobId) {
				return Map.of("1", "Am7  D7", "2", "Gmaj7");
			}
		});

		SheetProjectOmrProcessor.SheetProjectOmrData result = processor.processJobResult("job-123");
		assertThat(capturedJobId[0]).isEqualTo("job-123");
		assertThat(result.title()).isEqualTo("Autumn Leaves");
		assertThat(result.key()).isEqualTo(MusicKey.C_MAJOR);
		assertThat(result.timeSignature()).isEqualTo("3/4");
		assertThat(result.progression()).isEqualTo("Am7 D7 | Gmaj7");
	}

	@Test
	void processJobResult_buildsSheetProjectDataFromMusicXmlAndAssignments() {
		SheetProjectOmrProcessor processor = new SheetProjectOmrProcessor(new OmrClient(new OmrProperties("http://unused", null, null, null)) {
			@Override
			public String fetchMusicXml(String jobId) {
				return SIMPLE_MUSIC_XML;
			}

			@Override
			public Map<String, String> fetchChordAssignments(String jobId) {
				return Map.of("1", "Am7  D7", "2", "Gmaj7");
			}
		});

		SheetProjectOmrProcessor.SheetProjectOmrData result = processor.processJobResult("job-456");

		assertThat(result.title()).isEqualTo("Autumn Leaves");
		assertThat(result.key()).isEqualTo(MusicKey.C_MAJOR);
		assertThat(result.timeSignature()).isEqualTo("3/4");
		assertThat(result.progression()).isEqualTo("Am7 D7 | Gmaj7");
	}
}
