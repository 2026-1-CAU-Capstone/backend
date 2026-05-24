package com.jazzify.backend.domain.chordproject.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.omr.OmrClient;
import com.jazzify.backend.shared.omr.OmrProperties;

@NullMarked
class ChordProjectOmrProcessorTest {

	private static final String SIMPLE_MUSIC_XML = """
		<?xml version="1.0" encoding="UTF-8"?>
		<score-partwise version="3.1">
		  <work>
		    <work-title>Blue Bossa</work-title>
		  </work>
		  <part-list>
		    <score-part id="P1"><part-name>Music</part-name></score-part>
		  </part-list>
		  <part id="P1">
		    <measure number="1">
		      <attributes>
		        <divisions>1</divisions>
		        <key><fifths>-3</fifths><mode>minor</mode></key>
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
	void processJobResult_fetchesMusicXmlAndAssignments() {
		String[] capturedJobId = {null};
		ChordProjectOmrProcessor processor = new ChordProjectOmrProcessor(new OmrClient(new OmrProperties("http://unused", null, null, null)) {
			@Override
			public String fetchMusicXml(String jobId) {
				capturedJobId[0] = jobId;
				return SIMPLE_MUSIC_XML;
			}

			@Override
			public Map<String, String> fetchChordAssignments(String jobId) {
				return Map.of("1", "Cm7  F7", "2", "Bbmaj7");
			}
		});

		ChordProjectOmrProcessor.ChordProjectOmrData result = processor.processJobResult("job-123");
		assertThat(capturedJobId[0]).isEqualTo("job-123");
		assertThat(result.title()).isEqualTo("Blue Bossa");
		assertThat(result.key()).isEqualTo(MusicKey.C_MINOR);
		assertThat(result.timeSignature()).isEqualTo("4/4");
		assertThat(result.progression()).isEqualTo("Cm7 F7 | Bbmaj7");
	}

	@Test
	void processJobResult_buildsChordProjectDataFromMusicXmlAndAssignments() {
		ChordProjectOmrProcessor processor = new ChordProjectOmrProcessor(new OmrClient(new OmrProperties("http://unused", null, null, null)) {
			@Override
			public String fetchMusicXml(String jobId) {
				return SIMPLE_MUSIC_XML;
			}

			@Override
			public Map<String, String> fetchChordAssignments(String jobId) {
				return Map.of("1", "Cm7  F7", "2", "Bbmaj7");
			}
		});

		ChordProjectOmrProcessor.ChordProjectOmrData result = processor.processJobResult("job-456");

		assertThat(result.title()).isEqualTo("Blue Bossa");
		assertThat(result.key()).isEqualTo(MusicKey.C_MINOR);
		assertThat(result.timeSignature()).isEqualTo("4/4");
		assertThat(result.progression()).isEqualTo("Cm7 F7 | Bbmaj7");
	}
}
