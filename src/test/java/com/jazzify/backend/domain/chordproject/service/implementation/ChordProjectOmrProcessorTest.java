package com.jazzify.backend.domain.chordproject.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import com.jazzify.backend.domain.chordproject.model.ChordProjectOmrChord;
import com.jazzify.backend.domain.chordproject.model.ChordProjectOmrSourceType;
import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.omr.OmrClient;
import com.jazzify.backend.shared.omr.OmrProperties;

@NullMarked
class ChordProjectOmrProcessorTest {

	private static final String SIMPLE_MUSIC_XML = """
		<?xml version="1.0" encoding="UTF-8"?>
		<score-partwise version="3.1">
		  <work><work-title>Autumn Leaves</work-title></work>
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
	void processJobResult_fetchesChordChart() {
		String[] capturedJobId = {null};
		ChordProjectOmrProcessor processor = new ChordProjectOmrProcessor(new OmrClient(new OmrProperties("http://unused", null, null, null)) {
			@Override
			public ChordChartResult fetchChordChart(String jobId) {
				capturedJobId[0] = jobId;
				return new ChordChartResult(
					"Blue Monk",
					"4/4",
					4,
					List.of(
						new ChordChartChord(1, "Cm7", 1.0, 2.0),
						new ChordChartChord(1, "F7", 3.0, 2.0),
						new ChordChartChord(2, "Bbmaj7", 1.0, 4.0)
					)
				);
			}
		});

		ChordProjectOmrProcessor.ChordProjectOmrData result = processor.processJobResult("job-123");
		assertThat(capturedJobId[0]).isEqualTo("job-123");
		assertThat(result.title()).isEqualTo("Blue Monk");
		assertThat(result.key()).isNull();
		assertThat(result.timeSignature()).isEqualTo("4/4");
		assertThat(result.beatsPerBar()).isEqualTo(4);
		assertThat(result.chords()).containsExactly(
			new ChordProjectOmrChord(1, "Cm7", 1.0, 2.0),
			new ChordProjectOmrChord(1, "F7", 3.0, 2.0),
			new ChordProjectOmrChord(2, "Bbmaj7", 1.0, 4.0)
		);
	}

	@Test
	void processJobResult_buildsChordProjectDataFromChordChart() {
		ChordProjectOmrProcessor processor = new ChordProjectOmrProcessor(new OmrClient(new OmrProperties("http://unused", null, null, null)) {
			@Override
			public ChordChartResult fetchChordChart(String jobId) {
				return new ChordChartResult(
					"Untitled",
					"3/4",
					3,
					List.of(
						new ChordChartChord(1, "Dm7", 1.0, 3.0),
						new ChordChartChord(2, "G7", 1.0, 3.0),
						new ChordChartChord(3, "Cmaj7", 1.0, 3.0)
					)
				);
			}
		});

		ChordProjectOmrProcessor.ChordProjectOmrData result = processor.processJobResult("job-456");

		assertThat(result.title()).isEqualTo("Untitled");
		assertThat(result.key()).isNull();
		assertThat(result.timeSignature()).isEqualTo("3/4");
		assertThat(result.beatsPerBar()).isEqualTo(3);
		assertThat(result.chords()).extracting(ChordProjectOmrChord::chord)
			.containsExactly("Dm7", "G7", "Cmaj7");
	}

	@Test
	void processJobResult_buildsStructuredChordsForSheetMusicSource() {
		ChordProjectOmrProcessor processor = new ChordProjectOmrProcessor(new OmrClient(new OmrProperties("http://unused", null, null, null)) {
			@Override
			public String fetchMusicXml(String jobId) {
				return SIMPLE_MUSIC_XML;
			}

			@Override
			public Map<String, String> fetchChordAssignments(String jobId) {
				return Map.of("1", "Am7  D7", "2", "Gmaj7");
			}
		});

		ChordProjectOmrProcessor.ChordProjectOmrData result = processor.processJobResult(
			"job-sheet",
			ChordProjectOmrSourceType.SHEET_MUSIC
		);

		assertThat(result.title()).isEqualTo("Autumn Leaves");
		assertThat(result.key()).isEqualTo(MusicKey.C_MAJOR);
		assertThat(result.timeSignature()).isEqualTo("3/4");
		assertThat(result.beatsPerBar()).isEqualTo(3);
		assertThat(result.chords()).containsExactly(
			new ChordProjectOmrChord(1, "Am7", 1.0, 2.0),
			new ChordProjectOmrChord(1, "D7", 3.0, 1.0),
			new ChordProjectOmrChord(2, "Gmaj7", 1.0, 3.0)
		);
	}
}
