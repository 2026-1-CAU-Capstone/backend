package com.jazzify.backend.shared.omr;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jazzify.backend.shared.exception.CustomException;
import com.jazzify.backend.shared.exception.code.OmrErrorCode;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * OMR(Optical Music Recognition) 서버와 통신하는 HTTP 클라이언트.
 * <p>
 * MusicVision 비동기 명세에 따라 악보 파일을 업로드(202 Accepted)하고,
 * 콜백 또는 폴링으로 완료 확인 후 MusicXML과 chord assignments를 조회한다.
 * <ul>
 *   <li>prod profile 활성화: {@code /omr/prod/process} 엔드포인트 사용</li>
 *   <li>그 외 profile: {@code /omr/dev/process} 엔드포인트 사용</li>
 * </ul>
 */
@Slf4j
@NullMarked
@Component
public class OmrClient {

	private static final String MEASURE_ALIGNMENT_STATUS_ALIGNED = "aligned";
	private static final String MEASURE_ALIGNMENT_STATUS_PARTIAL = "partial";
	private static final String MEASURE_ALIGNMENT_STATUS_MISMATCH = "mismatch";
	private static final String HEADER_OMR_API_KEY = "X-OMR-API-Key";
	private static final String PROD_PROFILE = "prod";

	private final OmrProperties omrProperties;
	private final @Nullable Environment environment;

	@Autowired
	public OmrClient(OmrProperties omrProperties, Environment environment) {
		this.omrProperties = omrProperties;
		this.environment = environment;
	}

	public OmrClient(OmrProperties omrProperties) {
		this.omrProperties = omrProperties;
		this.environment = null;
	}

	/**
	 * 악보 파일을 OMR 서버에 비동기로 제출한다.
	 * <p>
	 * prod profile 활성화 시 → {@code POST /omr/prod/process}<br>
	 * 그 외 profile → {@code POST /omr/dev/process}<br>
	 * {@code omr.callback-url}이 설정된 경우 profile과 무관하게 {@code callback_url}에
	 * 베이스 URL + 도메인별 콜백 경로를 결합해 전달한다.
	 *
	 * @param fileData 악보 파일 바이트 배열
	 * @param filename 원본 파일명 (확장자 포함)
	 * @param jobId    OMR 서버에 전달할 job ID (project publicId 문자열 권장)
	 * @param domain   콜백을 받을 도메인 (도메인별 콜백 엔드포인트가 자동 부착됨)
	 * @return 제출 결과 (jobId, status)
	 * @throws CustomException OMR 서버 미설정, 제출 실패 시
	 */
	public OmrSubmitResult submitJob(byte[] fileData, String filename, String jobId, OmrCallbackDomain domain) {
		return submitJob(fileData, filename, jobId, domain, "/omr");
	}

	/**
	 * 코드 차트 이미지를 chord-chart 전용 OMR 서버에 비동기로 제출한다.
	 * <p>
	 * ChordProject OMR 생성 전용으로 사용한다.
	 */
	public OmrSubmitResult submitChordChartJob(byte[] fileData, String filename, String jobId, OmrCallbackDomain domain) {
		return submitJob(fileData, filename, jobId, domain, "/chords/chart");
	}

	/**
	 * 일반 악보 이미지를 chord sheet-music 전용 OMR 서버에 비동기로 제출한다.
	 * <p>
	 * ChordProject OMR 생성 전용으로 사용한다.
	 */
	public OmrSubmitResult submitChordSheetMusicJob(byte[] fileData, String filename, String jobId, OmrCallbackDomain domain) {
		return submitJob(fileData, filename, jobId, domain, "/chords/sheet-music");
	}

	private OmrSubmitResult submitJob(byte[] fileData, String filename, String jobId, OmrCallbackDomain domain, String endpointPrefix) {
		String serverUrl = requireServerUrl();
		WebClient webClient = createWebClient(serverUrl);

		String ext = extractExtension(filename);
		MediaType fileMediaType = deriveMediaType(ext);
		final String sanitizedFilename = sanitizeFilename(filename);

		MultipartBodyBuilder builder = new MultipartBodyBuilder();
		builder.part("file", new ByteArrayResource(fileData) {
			@Override
			public String getFilename() {
				return sanitizedFilename;
			}
		}).filename(sanitizedFilename).contentType(fileMediaType);

		builder.part("job_id", jobId);

		String callbackBaseUrl = omrProperties.callbackUrl();
		boolean isProdMode = isProdProfileActive();
		String endpoint;

		endpoint = endpointPrefix + (isProdMode ? "/prod/process" : "/dev/process");

		if (callbackBaseUrl != null && !callbackBaseUrl.isBlank()) {
			String fullCallbackUrl = buildCallbackUrl(callbackBaseUrl, domain);
			builder.part("callback_url", fullCallbackUrl);
			log.debug("[OMR] {} profile routing: endpoint={}, jobId={}, callbackUrl={}",
				isProdMode ? "prod" : "dev", endpoint, jobId, fullCallbackUrl);
		} else {
			log.debug("[OMR] {} profile routing (callback 없음): endpoint={}, jobId={}, domain={}",
				isProdMode ? "prod" : "dev", endpoint, jobId, domain);
		}

		try {
			WebClient.RequestBodySpec requestSpec = webClient.post().uri(endpoint);
			requestSpec = addApiKeyHeader(requestSpec);

			OmrSubmitResponse response = requestSpec
				.body(BodyInserters.fromMultipartData(builder.build()))
				.retrieve()
				.bodyToMono(OmrSubmitResponse.class)
				.block();

			if (response == null || response.jobId() == null) {
				throw OmrErrorCode.OMR_SUBMIT_FAILED.toException("OMR 서버 응답에 job_id가 없습니다.");
			}

			log.info("[OMR] 제출 완료: jobId={}, status={}", response.jobId(), response.status());
			return new OmrSubmitResult(response.jobId(), response.status());

		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			throw OmrErrorCode.OMR_SUBMIT_FAILED.toException(e.getMessage());
		}
	}

	/**
	 * OMR 서버에서 완료된 작업의 MusicXML을 가져온다.
	 *
	 * @param jobId OMR 작업 ID
	 * @return MusicXML 문자열
	 */
	public String fetchMusicXml(String jobId) {
		String serverUrl = requireServerUrl();
		WebClient webClient = createWebClient(serverUrl);

		try {
			String musicXml = addApiKeyHeader(
				webClient.get().uri("/omr/jobs/{jobId}/musicxml", jobId)
			).retrieve()
				.bodyToMono(String.class)
				.block();

			if (musicXml == null || musicXml.isBlank()) {
				throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException("MusicXML 결과가 비어 있습니다. job_id=" + jobId);
			}
			return musicXml;
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException("MusicXML 조회 중 오류: " + e.getMessage());
		}
	}

	/**
	 * OMR 서버에서 완료된 작업의 chord assignments를 가져와 마디번호→코드 매핑으로 변환한다.
	 *
	 * @param jobId OMR 작업 ID
	 * @return 마디번호(String) → 코드 문자열 매핑
	 */
	public Map<String, String> fetchChordAssignments(String jobId) {
		String serverUrl = requireServerUrl();
		WebClient webClient = createWebClient(serverUrl);

		try {
			ChordAssignmentsResponse response = addApiKeyHeader(
				webClient.get().uri("/omr/jobs/{jobId}/chord-assignments", jobId)
			).retrieve()
				.bodyToMono(ChordAssignmentsResponse.class)
				.block();

			return extractChordsByMeasureNumber(response, jobId);
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException("Chord assignments 조회 중 오류: " + e.getMessage());
		}
	}

	/**
	 * chord-chart 전용 OMR 결과를 가져와 ChordProject 진행 문자열로 변환한다.
	 *
	 * @param jobId OMR 작업 ID
	 * @return 코드 차트 데이터
	 */
	public ChordChartResult fetchChordChart(String jobId) {
		String serverUrl = requireServerUrl();
		WebClient webClient = createWebClient(serverUrl);

		try {
			ChordChartResponse response = addApiKeyHeader(
				webClient.get().uri("/omr/jobs/{jobId}/chord-chart", jobId)
			).retrieve()
				.bodyToMono(ChordChartResponse.class)
				.block();

			return extractChordChart(response, jobId);
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException("Chord chart 조회 중 오류: " + e.getMessage());
		}
	}

	/**
	 * OMR 서버 작업 상태를 조회한다.
	 *
	 * @param jobId OMR 작업 ID
	 * @return 작업 상태, 메시지, 진행률
	 */
	public OmrJobStatusResult fetchJobStatus(String jobId) {
		String serverUrl = requireServerUrl();
		WebClient webClient = createWebClient(serverUrl);

		try {
			OmrJobStatusResponse response = addApiKeyHeader(
				webClient.get().uri("/omr/jobs/{jobId}", jobId)
			).retrieve()
				.bodyToMono(OmrJobStatusResponse.class)
				.block();

			if (response == null || response.jobId() == null) {
				throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException("OMR 작업 상태 응답이 비어 있습니다. job_id=" + jobId);
			}
			return new OmrJobStatusResult(response.jobId(), response.status(), response.message(), response.progress());
		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException("OMR 작업 상태 조회 중 오류: " + e.getMessage());
		}
	}

	// ─── Private Helpers ─────────────────────────────────────────────────

	private boolean isProdProfileActive() {
		if (environment == null) {
			return false;
		}
		return Arrays.stream(environment.getActiveProfiles())
			.anyMatch(profile -> PROD_PROFILE.equalsIgnoreCase(profile));
	}

	private String buildCallbackUrl(String baseUrl, OmrCallbackDomain domain) {
		String trimmedBase = baseUrl.endsWith("/")
			? baseUrl.substring(0, baseUrl.length() - 1)
			: baseUrl;
		return trimmedBase + domain.path();
	}

	private String requireServerUrl() {
		String serverUrl = omrProperties.serverUrl();
		if (serverUrl == null || serverUrl.isBlank()) {
			throw OmrErrorCode.OMR_SERVER_NOT_CONFIGURED.toException();
		}
		return serverUrl;
	}

	private WebClient createWebClient(String serverUrl) {
		return WebClient.builder()
			.baseUrl(serverUrl)
			.filter(logRequest())
			.filter(logResponse())
			.build();
	}

	/**
	 * POST/GET RequestBodySpec에 API 키 헤더를 추가한다.
	 * {@code omr.api-key}가 설정된 경우에만 헤더를 추가한다.
	 */
	private WebClient.RequestBodySpec addApiKeyHeader(WebClient.RequestBodySpec spec) {
		String apiKey = omrProperties.apiKey();
		if (apiKey != null && !apiKey.isBlank()) {
			return spec.header(HEADER_OMR_API_KEY, apiKey);
		}
		return spec;
	}

	/**
	 * GET RequestHeadersSpec에 API 키 헤더를 추가한다.
	 */
	@SuppressWarnings("unchecked")
	private <S extends WebClient.RequestHeadersSpec<?>> S addApiKeyHeader(S spec) {
		String apiKey = omrProperties.apiKey();
		if (apiKey != null && !apiKey.isBlank()) {
			return (S) spec.header(HEADER_OMR_API_KEY, apiKey);
		}
		return spec;
	}

	private ExchangeFilterFunction logRequest() {
		return ExchangeFilterFunction.ofRequestProcessor(request -> {
			log.debug("[OMR] ▶ {} {}", request.method(), request.url());
			request.headers().forEach((name, values) ->
				log.debug("[OMR]   Header | {}: {}", name, String.join(", ", values)));
			return Mono.just(request);
		});
	}

	private ExchangeFilterFunction logResponse() {
		return ExchangeFilterFunction.ofResponseProcessor(response -> {
			log.debug("[OMR] ◀ Status | {}", response.statusCode());
			return Mono.just(response);
		});
	}

	private String sanitizeFilename(String filename) {
		return filename
			.replace("\r", "_")
			.replace("\n", "_")
			.replace("\"", "_");
	}

	private String extractExtension(String filename) {
		return filename.contains(".")
			? filename.substring(filename.lastIndexOf('.')).toLowerCase()
			: ".jpg";
	}

	private Map<String, String> extractChordsByMeasureNumber(@Nullable ChordAssignmentsResponse response, String jobId) {
		if (response == null) {
			throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException("Chord assignments 결과가 비어 있습니다. job_id=" + jobId);
		}

		String alignmentStatus = trimToNull(
			response.measureAlignment() != null ? response.measureAlignment().status() : null
		);
		if (alignmentStatus == null) {
			log.warn("[OMR] measure_alignment.status 가 없어 코드 결합을 건너뜁니다. job_id={}", jobId);
			return Map.of();
		}

		if (MEASURE_ALIGNMENT_STATUS_MISMATCH.equalsIgnoreCase(alignmentStatus)) {
			log.warn("[OMR] 마디 정렬 mismatch 로 자동 코드 결합을 건너뜁니다. job_id={}", jobId);
			return Map.of();
		}

		if (MEASURE_ALIGNMENT_STATUS_PARTIAL.equalsIgnoreCase(alignmentStatus)) {
			log.warn("[OMR] 마디 정렬 partial 상태입니다. musicxml_measure_number 가 있는 마디만 결합합니다. job_id={}", jobId);
		} else if (!MEASURE_ALIGNMENT_STATUS_ALIGNED.equalsIgnoreCase(alignmentStatus)) {
			log.warn("[OMR] 알 수 없는 마디 정렬 상태({})로 자동 코드 결합을 건너뜁니다. job_id={}", alignmentStatus, jobId);
			return Map.of();
		}

		Map<String, List<MeasureChord>> grouped = new LinkedHashMap<>();
		for (PageAssignments page : nullSafeList(response.pages())) {
			for (SystemAssignments system : nullSafeList(page.systems())) {
				for (MeasureAssignments measure : nullSafeList(system.measures())) {
					String measureNumber = trimToNull(measure.musicXmlMeasureNumber());
					if (measureNumber == null) {
						continue;
					}

					List<MeasureChord> measureChords = grouped.computeIfAbsent(measureNumber, key -> new ArrayList<>());
					for (ChordAssignment chord : nullSafeList(measure.chords())) {
						MeasureChord measureChord = toMeasureChord(chord);
						if (measureChord != null) {
							measureChords.add(measureChord);
						}
					}
				}
			}
		}

		Map<String, String> chordsByMeasureNumber = new LinkedHashMap<>();
		for (Map.Entry<String, List<MeasureChord>> entry : grouped.entrySet()) {
			List<MeasureChord> measureChords = entry.getValue();
			measureChords.sort(Comparator.comparingInt(MeasureChord::beat).thenComparing(MeasureChord::text));
			String joined = measureChords.stream()
				.map(MeasureChord::text)
				.reduce((left, right) -> left + "  " + right)
				.orElse("");
			if (!joined.isBlank()) {
				chordsByMeasureNumber.put(entry.getKey(), joined);
			}
		}

		return chordsByMeasureNumber;
	}

	private ChordChartResult extractChordChart(@Nullable ChordChartResponse response, String jobId) {
		if (response == null) {
			throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException("Chord chart 결과가 비어 있습니다. job_id=" + jobId);
		}

		List<String> bars = new ArrayList<>();
		for (ChartPage page : nullSafeList(response.pages())) {
			for (ChartSystem system : nullSafeList(page.systems())) {
				for (ChartMeasure measure : nullSafeList(system.measures())) {
					bars.add(toChartBarToken(measure));
				}
			}
		}

		if (bars.isEmpty()) {
			throw OmrErrorCode.OMR_PARSE_FAILED.toException("Chord chart에서 인식된 마디가 없습니다. job_id=" + jobId);
		}

		return new ChordChartResult(toTimeSignature(response.timeSignature()), String.join(" | ", bars));
	}

	private String toChartBarToken(ChartMeasure measure) {
		List<MeasureChord> measureChords = new ArrayList<>();
		for (ChordAssignment chord : nullSafeList(measure.chords())) {
			MeasureChord measureChord = toMeasureChord(chord);
			if (measureChord != null) {
				measureChords.add(measureChord);
			}
		}

		measureChords.sort(Comparator.comparingInt(MeasureChord::beat).thenComparing(MeasureChord::text));
		String joined = measureChords.stream()
			.map(MeasureChord::text)
			.reduce((left, right) -> left + " " + right)
			.orElse("");
		return joined.isBlank() ? "N.C." : joined.trim().replaceAll("\\s+", " ");
	}

	private String toTimeSignature(@Nullable ChartTimeSignature timeSignature) {
		if (timeSignature == null) {
			return "4/4";
		}
		String textRaw = trimToNull(timeSignature.textRaw());
		if (textRaw != null) {
			return textRaw;
		}
		if (timeSignature.numerator() != null && timeSignature.denominator() != null) {
			return timeSignature.numerator() + "/" + timeSignature.denominator();
		}
		return "4/4";
	}

	@Nullable
	private MeasureChord toMeasureChord(ChordAssignment chord) {
		String chordText = trimToNull(chord.textNorm());
		if (chordText == null) {
			chordText = trimToNull(chord.textRaw());
		}
		if (chordText == null) {
			return null;
		}

		int beat = chord.beat() != null ? chord.beat() : Integer.MAX_VALUE;
		return new MeasureChord(beat, chordText);
	}

	private static MediaType deriveMediaType(String ext) {
		return switch (ext) {
			case ".jpg", ".jpeg" -> MediaType.IMAGE_JPEG;
			case ".png" -> MediaType.IMAGE_PNG;
			default -> throw OmrErrorCode.OMR_INVALID_FILE_TYPE.toException("지원하지 않는 파일 확장자입니다: " + ext);
		};
	}

	private static <T> List<T> nullSafeList(@Nullable List<T> values) {
		return values != null ? values : List.of();
	}

	private static @Nullable String trimToNull(@Nullable String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	// ─── Inner DTO Records ───────────────────────────────────────────────

	/** OMR 비동기 제출 결과. */
	@NullMarked
	public record OmrSubmitResult(
		String jobId,
		@Nullable String status
	) {
	}

	public record ChordChartResult(
		String timeSignature,
		String progression
	) {
	}

	public record OmrJobStatusResult(
		String jobId,
		@Nullable String status,
		@Nullable String message,
		@Nullable Integer progress
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record OmrSubmitResponse(
		@JsonProperty("job_id") @Nullable String jobId,
		@Nullable String status,
		@Nullable String message
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ChordAssignmentsResponse(
		@JsonProperty("measure_alignment") @Nullable MeasureAlignment measureAlignment,
		@Nullable List<PageAssignments> pages
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ChordChartResponse(
		@JsonProperty("time_signature") @Nullable ChartTimeSignature timeSignature,
		@Nullable List<ChartPage> pages
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record OmrJobStatusResponse(
		@JsonProperty("job_id") @Nullable String jobId,
		@Nullable String status,
		@Nullable String message,
		@Nullable Integer progress
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ChartTimeSignature(
		@JsonProperty("text_raw") @JsonAlias("text") @Nullable String textRaw,
		@Nullable Integer numerator,
		@Nullable Integer denominator
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ChartPage(
		@Nullable List<ChartSystem> systems
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ChartSystem(
		@Nullable List<ChartMeasure> measures
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ChartMeasure(
		@Nullable List<ChordAssignment> chords
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record MeasureAlignment(
		@Nullable String status
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record PageAssignments(
		@Nullable List<SystemAssignments> systems
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record SystemAssignments(
		@Nullable List<MeasureAssignments> measures
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record MeasureAssignments(
		@JsonProperty("musicxml_measure_number") @Nullable String musicXmlMeasureNumber,
		@Nullable List<ChordAssignment> chords
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ChordAssignment(
		@JsonProperty("text_raw") @Nullable String textRaw,
		@JsonProperty("text_norm") @Nullable String textNorm,
		@Nullable Integer beat
	) {
	}

	private record MeasureChord(int beat, String text) {
	}

}
