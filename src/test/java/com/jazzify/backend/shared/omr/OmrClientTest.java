package com.jazzify.backend.shared.omr;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

@NullMarked
class OmrClientTest {

	private static final String MUSIC_XML = """
		<?xml version="1.0" encoding="UTF-8"?>
		<score-partwise version="3.1">
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
		  </part>
		</score-partwise>
		""";

	@Test
	void recognize_joinsOnlyMeasuresWithMusicXmlMeasureNumberWhenAlignmentIsPartial() throws Exception {
		String chordAssignmentsJson = """
			{
			  "measure_alignment": {
			    "status": "partial"
			  },
			  "pages": [
			    {
			      "systems": [
			        {
			          "measures": [
			            {
			              "musicxml_measure_number": "1",
			              "chords": [
			                { "text_norm": "Dm7", "beat": 1 },
			                { "text_norm": "G7", "beat": 3 }
			              ]
			            },
			            {
			              "chords": [
			                { "text_norm": "Cmaj7", "beat": 1 }
			              ]
			            }
			          ]
			        }
			      ]
			    }
			  ]
			}
			""";

		try (TestOmrServer server = new TestOmrServer(MUSIC_XML, chordAssignmentsJson)) {
			OmrClient client = new OmrClient(new OmrProperties(server.baseUrl()));
			MockMultipartFile file = new MockMultipartFile(
				"file",
				"score.png",
				"image/png",
				"dummy".getBytes(StandardCharsets.UTF_8)
			);

			OmrClient.OmrRecognitionResult result = client.recognize(file);

			assertThat(result.musicXml()).isEqualTo(MUSIC_XML);
			assertThat(result.chordsByMeasureNumber()).containsExactly(Map.entry("1", "Dm7  G7"));
		}
	}

	@Test
	void recognize_skipsAutomaticChordJoinWhenAlignmentIsMismatch() throws Exception {
		String chordAssignmentsJson = """
			{
			  "measure_alignment": {
			    "status": "mismatch"
			  },
			  "pages": [
			    {
			      "systems": [
			        {
			          "measures": [
			            {
			              "musicxml_measure_number": "1",
			              "chords": [
			                { "text_norm": "Fmaj7", "beat": 1 }
			              ]
			            }
			          ]
			        }
			      ]
			    }
			  ]
			}
			""";

		try (TestOmrServer server = new TestOmrServer(MUSIC_XML, chordAssignmentsJson)) {
			OmrClient client = new OmrClient(new OmrProperties(server.baseUrl()));
			MockMultipartFile file = new MockMultipartFile(
				"file",
				"score.png",
				"image/png",
				"dummy".getBytes(StandardCharsets.UTF_8)
			);

			OmrClient.OmrRecognitionResult result = client.recognize(file);

			assertThat(result.musicXml()).isEqualTo(MUSIC_XML);
			assertThat(result.chordsByMeasureNumber()).isEmpty();
		}
	}

	private static final class TestOmrServer implements AutoCloseable {

		private static final String JOB_ID = "job-123";

		private final HttpServer server;

		private TestOmrServer(String musicXml, String chordAssignmentsJson) throws IOException {
			server = HttpServer.create(new InetSocketAddress(0), 0);
			server.createContext("/omr/process", this::respondToProcess);
			server.createContext("/omr/jobs/" + JOB_ID + "/musicxml", exchange ->
				respond(exchange, "application/vnd.recordare.musicxml+xml", musicXml));
			server.createContext("/omr/jobs/" + JOB_ID + "/chord-assignments", exchange ->
				respond(exchange, "application/json", chordAssignmentsJson));
			server.start();
		}

		private String baseUrl() {
			return "http://localhost:" + server.getAddress().getPort();
		}

		private void respondToProcess(HttpExchange exchange) throws IOException {
			consume(exchange.getRequestBody());
			respond(exchange, "application/json", """
				{
				  "job_id": "job-123",
				  "status": "completed"
				}
				""");
		}

		private static void respond(HttpExchange exchange, String contentType, String body) throws IOException {
			exchange.getResponseHeaders().add("Content-Type", contentType + "; charset=UTF-8");
			byte[] payload = body.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(200, payload.length);
			try (OutputStream outputStream = exchange.getResponseBody()) {
				outputStream.write(payload);
			}
			exchange.close();
		}

		private static void consume(InputStream inputStream) throws IOException {
			try (InputStream in = inputStream) {
				in.transferTo(OutputStream.nullOutputStream());
			}
		}

		@Override
		public void close() {
			server.stop(0);
		}
	}
}


