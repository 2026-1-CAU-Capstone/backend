package com.jazzify.backend.shared.omr;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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
	void fetchChordAssignments_joinsOnlyMeasuresWithMusicXmlMeasureNumberWhenAlignmentIsPartial() throws Exception {
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
			OmrClient client = new OmrClient(new OmrProperties(server.baseUrl(), null, null, null));
			assertThat(client.fetchMusicXml(TestOmrServer.JOB_ID)).isEqualTo(MUSIC_XML);
			assertThat(client.fetchChordAssignments(TestOmrServer.JOB_ID))
				.containsExactly(Map.entry("1", "Dm7  G7"));
		}
	}

	@Test
	void fetchChordAssignments_skipsAutomaticChordJoinWhenAlignmentIsMismatch() throws Exception {
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
			OmrClient client = new OmrClient(new OmrProperties(server.baseUrl(), null, null, null));
			assertThat(client.fetchChordAssignments(TestOmrServer.JOB_ID)).isEmpty();
		}
	}

	@Test
	void submitJob_usesDevEndpointAndAppendsDomainPathToCallbackUrlWhenConfigured() throws Exception {
		try (TestOmrServer server = new TestOmrServer(MUSIC_XML, "{}")) {
			OmrClient client = new OmrClient(new OmrProperties(
				server.baseUrl(),
				"request-key",
				null,
				"http://localhost:8080/api"
			));
			MockMultipartFile file = new MockMultipartFile("file", "score.png", "image/png", "dummy".getBytes(StandardCharsets.UTF_8));

			OmrClient.OmrSubmitResult result = client.submitJob(
				file.getBytes(), file.getOriginalFilename(), "job-999", OmrCallbackDomain.LICK);

			assertThat(result.jobId()).isEqualTo(TestOmrServer.JOB_ID);
			assertThat(server.lastProcessPath()).isEqualTo("/omr/dev/process");
			assertThat(server.lastOmrApiKey()).isEqualTo("request-key");
			assertThat(server.lastRequestBody()).contains("name=\"callback_url\"");
			assertThat(server.lastRequestBody()).contains("http://localhost:8080/api/v1/licks/omr/callback");
		}
	}

	@Test
	void submitJob_stripsTrailingSlashFromCallbackBaseUrlBeforeAppendingDomainPath() throws Exception {
		try (TestOmrServer server = new TestOmrServer(MUSIC_XML, "{}")) {
			OmrClient client = new OmrClient(new OmrProperties(
				server.baseUrl(),
				null,
				null,
				"http://localhost:8080/api/"
			));
			MockMultipartFile file = new MockMultipartFile("file", "score.png", "image/png", "dummy".getBytes(StandardCharsets.UTF_8));

			client.submitJob(file.getBytes(), file.getOriginalFilename(), "job-999", OmrCallbackDomain.SOLO);

			assertThat(server.lastRequestBody()).contains("http://localhost:8080/api/v1/solos/omr/callback");
			assertThat(server.lastRequestBody()).doesNotContain("http://localhost:8080/api//v1/solos");
		}
	}

	private static final class TestOmrServer implements AutoCloseable {

		private static final String JOB_ID = "job-123";

		private final HttpServer server;
		private final String musicXml;
		private final String chordAssignmentsJson;
		private final AtomicReference<String> lastProcessPath = new AtomicReference<>("");
		private final AtomicReference<String> lastOmrApiKey = new AtomicReference<>("");
		private final AtomicReference<String> lastRequestBody = new AtomicReference<>("");

		private TestOmrServer(String musicXml, String chordAssignmentsJson) throws IOException {
			this.musicXml = musicXml;
			this.chordAssignmentsJson = chordAssignmentsJson;
			server = HttpServer.create(new InetSocketAddress(0), 0);
			server.createContext("/omr/dev/process", this::respondToProcess);
			server.createContext("/omr/prod/process", this::respondToProcess);
			server.createContext("/omr/jobs/" + JOB_ID + "/musicxml", exchange ->
				respond(exchange, 200, "application/vnd.recordare.musicxml+xml", this.musicXml));
			server.createContext("/omr/jobs/" + JOB_ID + "/chord-assignments", exchange ->
				respond(exchange, 200, "application/json", this.chordAssignmentsJson));
			server.start();
		}

		private String baseUrl() {
			return "http://localhost:" + server.getAddress().getPort();
		}

		private String lastProcessPath() {
			return lastProcessPath.get();
		}

		private String lastOmrApiKey() {
			return lastOmrApiKey.get();
		}

		private String lastRequestBody() {
			return lastRequestBody.get();
		}

		private void respondToProcess(HttpExchange exchange) throws IOException {
			lastProcessPath.set(exchange.getRequestURI().getPath());
			lastOmrApiKey.set(exchange.getRequestHeaders().getFirst("X-OMR-API-Key"));
			lastRequestBody.set(readBody(exchange.getRequestBody()));
			respond(exchange, 202, "application/json", """
				{
				  "job_id": "job-123",
				  "status": "queued"
				}
				""");
		}

		private static String readBody(InputStream inputStream) throws IOException {
			try (InputStream in = inputStream) {
				return new String(in.readAllBytes(), StandardCharsets.UTF_8);
			}
		}

		private static void respond(HttpExchange exchange, int status, String contentType, String body) throws IOException {
			exchange.getResponseHeaders().add("Content-Type", contentType + "; charset=UTF-8");
			byte[] payload = body.getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(status, payload.length);
			try (OutputStream outputStream = exchange.getResponseBody()) {
				outputStream.write(payload);
			}
			exchange.close();
		}

		@Override
		public void close() {
			server.stop(0);
		}
	}
}
