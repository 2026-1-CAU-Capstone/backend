package com.jazzify.backend.shared.omr;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
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
				"http://localhost:8080"
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
				"http://localhost:8080/"
			));
			MockMultipartFile file = new MockMultipartFile("file", "score.png", "image/png", "dummy".getBytes(StandardCharsets.UTF_8));

			client.submitJob(file.getBytes(), file.getOriginalFilename(), "job-999", OmrCallbackDomain.SOLO);

			assertThat(server.lastRequestBody()).contains("http://localhost:8080/api/v1/solos/omr/callback");
			assertThat(server.lastRequestBody()).doesNotContain("http://localhost:8080//api/v1/solos");
		}
	}

	@Test
	void submitChordChartJob_usesChordChartEndpoint() throws Exception {
		try (TestOmrServer server = new TestOmrServer(MUSIC_XML, "{}")) {
			OmrClient client = new OmrClient(new OmrProperties(
				server.baseUrl(),
				"request-key",
				null,
				"http://localhost:8080"
			));
			MockMultipartFile file = new MockMultipartFile("file", "chart.png", "image/png", "dummy".getBytes(StandardCharsets.UTF_8));

			OmrClient.OmrSubmitResult result = client.submitChordChartJob(
				file.getBytes(), file.getOriginalFilename(), "job-999", OmrCallbackDomain.CHORD_PROJECT);

			assertThat(result.jobId()).isEqualTo(TestOmrServer.JOB_ID);
			assertThat(server.lastProcessPath()).isEqualTo("/chords/chart/dev/process");
			assertThat(server.lastOmrApiKey()).isEqualTo("request-key");
			assertThat(server.lastRequestBody()).contains("http://localhost:8080/api/v1/chord-projects/omr/callback");
		}
	}

	@Test
	void submitChordSheetMusicJob_usesChordSheetMusicEndpoint() throws Exception {
		try (TestOmrServer server = new TestOmrServer(MUSIC_XML, "{}")) {
			OmrClient client = new OmrClient(new OmrProperties(
				server.baseUrl(),
				"request-key",
				null,
				"http://localhost:8080"
			));
			MockMultipartFile file = new MockMultipartFile("file", "score.png", "image/png", "dummy".getBytes(StandardCharsets.UTF_8));

			OmrClient.OmrSubmitResult result = client.submitChordSheetMusicJob(
				file.getBytes(), file.getOriginalFilename(), "job-999", OmrCallbackDomain.CHORD_PROJECT);

			assertThat(result.jobId()).isEqualTo(TestOmrServer.JOB_ID);
			assertThat(server.lastProcessPath()).isEqualTo("/chords/sheet-music/dev/process");
			assertThat(server.lastOmrApiKey()).isEqualTo("request-key");
			assertThat(server.lastRequestBody()).contains("http://localhost:8080/api/v1/chord-projects/omr/callback");
		}
	}

	@Test
	void submitJob_usesProdEndpointWhenProdProfileIsActiveEvenWithCallbackUrl() throws Exception {
		try (TestOmrServer server = new TestOmrServer(MUSIC_XML, "{}")) {
			MockEnvironment environment = new MockEnvironment();
			environment.setActiveProfiles("prod");
			OmrClient client = new OmrClient(new OmrProperties(
				server.baseUrl(),
				"request-key",
				null,
				"http://backend.example"
			), environment);
			MockMultipartFile file = new MockMultipartFile("file", "score.png", "image/png", "dummy".getBytes(StandardCharsets.UTF_8));

			client.submitJob(file.getBytes(), file.getOriginalFilename(), "job-999", OmrCallbackDomain.SHEET_PROJECT);

			assertThat(server.lastProcessPath()).isEqualTo("/omr/prod/process");
			assertThat(server.lastRequestBody()).contains("name=\"callback_url\"");
			assertThat(server.lastRequestBody()).contains("http://backend.example/api/v1/sheet-projects/omr/callback");
		}
	}

	@Test
	void submitJob_usesDevEndpointWhenProdProfileIsNotActiveEvenWithoutCallbackUrl() throws Exception {
		try (TestOmrServer server = new TestOmrServer(MUSIC_XML, "{}")) {
			MockEnvironment environment = new MockEnvironment();
			environment.setActiveProfiles("dev");
			OmrClient client = new OmrClient(new OmrProperties(
				server.baseUrl(),
				null,
				null,
				null
			), environment);
			MockMultipartFile file = new MockMultipartFile("file", "score.png", "image/png", "dummy".getBytes(StandardCharsets.UTF_8));

			client.submitJob(file.getBytes(), file.getOriginalFilename(), "job-999", OmrCallbackDomain.SOLO);

			assertThat(server.lastProcessPath()).isEqualTo("/omr/dev/process");
			assertThat(server.lastRequestBody()).doesNotContain("name=\"callback_url\"");
		}
	}

	@Test
	void fetchChordChart_buildsProgressionFromChartJson() throws Exception {
		String chordChartJson = """
			{
			  "time_signature": {
			    "numerator": 3,
			    "denominator": 4
			  },
			  "pages": [
			    {
			      "systems": [
			        {
			          "measures": [
			            {
			              "chords": [
			                { "text_norm": "G7", "beat": 3 },
			                { "text_norm": "Dm7", "beat": 1 }
			              ]
			            },
			            {
			              "chords": [
			                { "text_raw": "Cmaj7", "beat": 1 }
			              ]
			            },
			            {
			              "chords": []
			            }
			          ]
			        }
			      ]
			    }
			  ]
			}
			""";

		try (TestOmrServer server = new TestOmrServer(MUSIC_XML, "{}", chordChartJson)) {
			OmrClient client = new OmrClient(new OmrProperties(server.baseUrl(), null, null, null));

			OmrClient.ChordChartResult result = client.fetchChordChart(TestOmrServer.JOB_ID);

			assertThat(result.title()).isEqualTo("Untitled");
			assertThat(result.timeSignature()).isEqualTo("3/4");
			assertThat(result.beatsPerBar()).isEqualTo(3);
			assertThat(result.chords()).containsExactly(
				new OmrClient.ChordChartChord(1, "Dm7", 1.0, 2.0),
				new OmrClient.ChordChartChord(1, "G7", 3.0, 1.0),
				new OmrClient.ChordChartChord(2, "Cmaj7", 1.0, 3.0),
				new OmrClient.ChordChartChord(3, null, 1.0, 3.0)
			);
		}
	}

	@Test
	void fetchChordChart_usesStructuredChordsAndResolvedRepeatChordsInsteadOfAcceptedTokenCandidates() throws Exception {
		String chordChartJson = """
			{
			  "title": "After You've Gone",
			  "time_signature": {
			    "text_raw": "4/4",
			    "numerator": 4,
			    "denominator": 4
			  },
			  "beats_per_bar": 4,
			  "chart_ocr": {
			    "accepted_tokens": [
			      { "kind": "chord", "text_norm": "Bb", "bbox": [518.0, 128.5, 616.5, 224.0], "confidence": 0.33 },
			      { "kind": "chord", "text_norm": "Bdim", "bbox": [518.0, 153.0, 615.0, 232.0], "confidence": 0.41 },
			      { "kind": "chord", "text_norm": "Dm0", "bbox": [520.5, 197.0, 607.5, 225.5], "confidence": 0.24 },
			      { "kind": "ending", "text": "2", "bbox": [363.0, 152.0, 409.0, 210.0], "confidence": 0.15 }
			    ]
			  },
			  "pages": [
			    {
			      "systems": [
			        {
			          "measures": [
			            {
			              "index": 1,
			              "chords": [
			                { "text_norm": "Bbmaj7", "beat": 1, "confidence": 0.92 }
			              ]
			            },
			            {
			              "index": 2,
			              "chords": [],
			              "resolved_chords": [
			                { "text_norm": "Bbmaj7", "beat": 1 }
			              ]
			            },
			            {
			              "index": 3,
			              "chords": [
			                { "text_norm": "Bdim", "beat": 1, "confidence": 0.41 }
			              ]
			            },
			            {
			              "index": 4,
			              "chords": [
			                { "text_norm": "Em7", "beat": 1, "confidence": 0.98 },
			                { "text_norm": "Ab7", "beat": 3, "confidence": 0.99 }
			              ]
			            },
			            {
			              "index": 5,
			              "chords": [
			                { "text_norm": "Eb7", "beat": 3, "confidence": 0.61 }
			              ]
			            },
			            {
			              "index": 6,
			              "chords": []
			            },
			            {
			              "index": 7,
			              "chords": [
			                { "text_norm": "Dm7", "beat": 1, "confidence": 0.96 },
			                { "text_norm": "Dm7", "beat": 3, "confidence": 0.89 }
			              ]
			            }
			          ]
			        }
			      ]
			    }
			  ]
			}
			""";

		try (TestOmrServer server = new TestOmrServer(MUSIC_XML, "{}", chordChartJson)) {
			OmrClient client = new OmrClient(new OmrProperties(server.baseUrl(), null, null, null));

			OmrClient.ChordChartResult result = client.fetchChordChart(TestOmrServer.JOB_ID);

			assertThat(result.title()).isEqualTo("After You've Gone");
			assertThat(result.timeSignature()).isEqualTo("4/4");
			assertThat(result.beatsPerBar()).isEqualTo(4);
			assertThat(result.chords()).containsExactlyElementsOf(List.of(
				new OmrClient.ChordChartChord(1, "Bbmaj7", 1.0, 4.0),
				new OmrClient.ChordChartChord(2, "Bbmaj7", 1.0, 4.0),
				new OmrClient.ChordChartChord(3, "Bdim", 1.0, 4.0),
				new OmrClient.ChordChartChord(4, "Em7", 1.0, 2.0),
				new OmrClient.ChordChartChord(4, "Ab7", 3.0, 2.0),
				new OmrClient.ChordChartChord(5, "Eb7", 3.0, 2.0),
				new OmrClient.ChordChartChord(6, null, 1.0, 4.0),
				new OmrClient.ChordChartChord(7, "Dm7", 1.0, 4.0)
			));
		}
	}

	@Test
	void fetchJobStatus_returnsProgressFromStatusEndpoint() throws Exception {
		String statusJson = """
			{
			  "job_id": "job-123",
			  "status": "processing",
			  "message": "Reading chart cells",
			  "progress": 62
			}
			""";

		try (TestOmrServer server = new TestOmrServer(MUSIC_XML, "{}", "{}", statusJson)) {
			OmrClient client = new OmrClient(new OmrProperties(server.baseUrl(), "request-key", null, null));

			OmrClient.OmrJobStatusResult result = client.fetchJobStatus(TestOmrServer.JOB_ID);

			assertThat(result.jobId()).isEqualTo(TestOmrServer.JOB_ID);
			assertThat(result.status()).isEqualTo("processing");
			assertThat(result.message()).isEqualTo("Reading chart cells");
			assertThat(result.progress()).isEqualTo(62);
			assertThat(server.lastOmrApiKey()).isEqualTo("request-key");
		}
	}

	private static final class TestOmrServer implements AutoCloseable {

		private static final String JOB_ID = "job-123";

		private final HttpServer server;
		private final String musicXml;
		private final String chordAssignmentsJson;
		private final String chordChartJson;
		private final String statusJson;
		private final AtomicReference<String> lastProcessPath = new AtomicReference<>("");
		private final AtomicReference<String> lastOmrApiKey = new AtomicReference<>("");
		private final AtomicReference<String> lastRequestBody = new AtomicReference<>("");

		private TestOmrServer(String musicXml, String chordAssignmentsJson) throws IOException {
			this(musicXml, chordAssignmentsJson, "{}");
		}

		private TestOmrServer(String musicXml, String chordAssignmentsJson, String chordChartJson) throws IOException {
			this(musicXml, chordAssignmentsJson, chordChartJson, """
				{
				  "job_id": "job-123",
				  "status": "completed",
				  "progress": 100
				}
				""");
		}

		private TestOmrServer(String musicXml, String chordAssignmentsJson, String chordChartJson, String statusJson) throws IOException {
			this.musicXml = musicXml;
			this.chordAssignmentsJson = chordAssignmentsJson;
			this.chordChartJson = chordChartJson;
			this.statusJson = statusJson;
			server = HttpServer.create(new InetSocketAddress(0), 0);
			server.createContext("/omr/dev/process", this::respondToProcess);
			server.createContext("/omr/prod/process", this::respondToProcess);
			server.createContext("/chords/sheet-music/dev/process", this::respondToProcess);
			server.createContext("/chords/sheet-music/prod/process", this::respondToProcess);
			server.createContext("/chords/chart/dev/process", this::respondToProcess);
			server.createContext("/chords/chart/prod/process", this::respondToProcess);
			server.createContext("/omr/jobs/" + JOB_ID + "/musicxml", exchange ->
				respond(exchange, 200, "application/vnd.recordare.musicxml+xml", this.musicXml));
			server.createContext("/omr/jobs/" + JOB_ID + "/chord-assignments", exchange ->
				respond(exchange, 200, "application/json", this.chordAssignmentsJson));
			server.createContext("/omr/jobs/" + JOB_ID + "/chord-chart", exchange ->
				respond(exchange, 200, "application/json", this.chordChartJson));
			server.createContext("/omr/jobs/" + JOB_ID, exchange -> {
				lastOmrApiKey.set(exchange.getRequestHeaders().getFirst("X-OMR-API-Key"));
				respond(exchange, 200, "application/json", this.statusJson);
			});
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
