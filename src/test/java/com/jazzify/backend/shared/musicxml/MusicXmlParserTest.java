package com.jazzify.backend.shared.musicxml;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class MusicXmlParserTest {

	private static final String SIMPLE_MUSIC_XML = """
		<?xml version="1.0" encoding="UTF-8"?>
		<score-partwise version="3.1">
		  <work>
		    <work-title>OMR Test</work-title>
		  </work>
		  <identification>
		    <creator type="composer">Test Composer</creator>
		  </identification>
		  <part-list>
		    <score-part id="P1">
		      <part-name>Music</part-name>
		    </score-part>
		  </part-list>
		  <part id="P1">
		    <measure number="1">
		      <attributes>
		        <divisions>1</divisions>
		        <key><fifths>0</fifths></key>
		        <time><beats>4</beats><beat-type>4</beat-type></time>
		      </attributes>
		      <harmony>
		        <root><root-step>C</root-step></root>
		        <kind>major-seventh</kind>
		      </harmony>
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

	private static final String MINOR_MODE_MUSIC_XML = """
		<?xml version="1.0" encoding="UTF-8"?>
		<score-partwise version="3.1">
		  <part-list>
		    <score-part id="P1"><part-name>Music</part-name></score-part>
		  </part-list>
		  <part id="P1">
		    <measure number="1">
		      <attributes>
		        <divisions>1</divisions>
		        <key><fifths>-3</fifths><mode>minor</mode></key>
		        <time><beats>3</beats><beat-type>4</beat-type></time>
		      </attributes>
		      <note>
		        <pitch><step>C</step><octave>4</octave></pitch>
		        <duration>1</duration>
		        <type>quarter</type>
		      </note>
		    </measure>
		  </part>
		</score-partwise>
		""";

	@Test
	void parse_usesChordAssignmentsByMusicXmlMeasureNumber_andFallsBackToHarmonyWhenMissing() {
		ParsedSheetData parsed = MusicXmlParser.parse(
			SIMPLE_MUSIC_XML,
			Map.of("2", "Dm7  G7")
		);

		assertThat(parsed.measures())
			.extracting(ParsedMeasure::chord)
			.containsExactly("CΔ7", "Dm7  G7");
	}

	@Test
	void parse_includesMinorModeInKey() {
		ParsedSheetData parsed = MusicXmlParser.parse(MINOR_MODE_MUSIC_XML, Map.of());

		assertThat(parsed.key()).isEqualTo("Cm");
		assertThat(parsed.timeSignature()).isEqualTo("3/4");
	}
}

