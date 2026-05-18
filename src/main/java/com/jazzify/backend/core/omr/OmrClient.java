package com.jazzify.backend.core.omr;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.jazzify.backend.shared.exception.CustomException;
import com.jazzify.backend.shared.exception.code.OmrErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * OMR(Optical Music Recognition) 서버와 통신하는 HTTP 클라이언트.
 * <p>
 * MusicVision 명세에 따라 악보 이미지를 업로드하고,
 * 생성된 {@code job_id}를 이용해 MusicXML과 chord assignments를 조회한다.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class OmrClient {

	private static final String PROCESS_STATUS_COMPLETED = "completed";
	private static final String MEASURE_ALIGNMENT_STATUS_ALIGNED = "aligned";

	private final OmrProperties omrProperties;

	/**
	 * 악보 파일을 OMR 서버에 전송하고 MusicXML과 마디별 코드 매핑 결과를 반환한다.
	 *
	 * @param file 악보 파일 (PNG · JPG · JPEG)
	 * @return MusicXML 문자열과 chord assignments 기반 마디 코드 매핑
	 * @throws CustomException OMR 서버 미설정, 인식 실패 시
	 */
	public OmrRecognitionResult recognize(MultipartFile file) {
		String serverUrl = omrProperties.serverUrl();
		if (serverUrl == null || serverUrl.isBlank()) {
			throw OmrErrorCode.OMR_SERVER_NOT_CONFIGURED.toException();
		}

		try {
			byte[] bytes = file.getBytes();
			RestClient restClient = RestClient.create(serverUrl);

			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("file", new NamedByteArrayResource(bytes, file.getOriginalFilename()));

			OmrProcessResponse processResponse = restClient
				.post()
				.uri("/omr/process")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(body)
				.retrieve()
				.body(OmrProcessResponse.class);

			String jobId = requireCompletedJobId(processResponse);
			String musicXml = fetchMusicXml(restClient, jobId);
			Map<String, String> chordsByMeasureNumber = fetchChordsByMeasureNumber(restClient, jobId);

			return new OmrRecognitionResult(musicXml, chordsByMeasureNumber);

		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException(e.getMessage());
		}
	}

	private String requireCompletedJobId(@Nullable OmrProcessResponse processResponse) {
		if (processResponse == null) {
			throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException("OMR 서버가 처리 결과를 반환하지 않았습니다.");
		}

		String status = trimToNull(processResponse.status());
		if (status != null && !PROCESS_STATUS_COMPLETED.equalsIgnoreCase(status)) {
			throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException("OMR 처리 상태가 completed가 아닙니다: " + status);
		}

		String jobId = trimToNull(processResponse.jobId());
		if (jobId == null) {
			throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException("OMR 응답에 job_id가 없습니다.");
		}
		return jobId;
	}

	private String fetchMusicXml(RestClient restClient, String jobId) {
		String musicXml = restClient
			.get()
			.uri("/omr/jobs/{jobId}/musicxml", jobId)
			.retrieve()
			.body(String.class);

		if (musicXml == null || musicXml.isBlank()) {
			throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException("MusicXML 결과가 비어 있습니다. job_id=" + jobId);
		}
		return musicXml;
	}

	private Map<String, String> fetchChordsByMeasureNumber(RestClient restClient, String jobId) {
		ChordAssignmentsResponse response = restClient
			.get()
			.uri("/omr/jobs/{jobId}/chord-assignments", jobId)
			.retrieve()
			.body(ChordAssignmentsResponse.class);

		return extractChordsByMeasureNumber(response, jobId);
	}

	private Map<String, String> extractChordsByMeasureNumber(@Nullable ChordAssignmentsResponse response, String jobId) {
		if (response == null) {
			throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException("Chord assignments 결과가 비어 있습니다. job_id=" + jobId);
		}

		String alignmentStatus = trimToNull(
			response.measureAlignment() != null ? response.measureAlignment().status() : null
		);
		if (!MEASURE_ALIGNMENT_STATUS_ALIGNED.equalsIgnoreCase(alignmentStatus != null ? alignmentStatus : "")) {
			throw OmrErrorCode.OMR_MEASURE_ALIGNMENT_MISMATCH.toException(
				"job_id=" + jobId + ", status=" + (alignmentStatus != null ? alignmentStatus : "unknown")
			);
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

	// ─── Inner Helper ───────────────────────────────────────────────────

	/** OMR 인식 결과. */
	@NullMarked
	public record OmrRecognitionResult(
		String musicXml,
		Map<String, String> chordsByMeasureNumber
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record OmrProcessResponse(
		@JsonProperty("job_id") @Nullable String jobId,
		@Nullable String status
	) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ChordAssignmentsResponse(
		@JsonProperty("measure_alignment") @Nullable MeasureAlignment measureAlignment,
		@Nullable List<PageAssignments> pages
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

	/** multipart 요청 시 파일명을 함께 전송하기 위한 ByteArrayResource 확장. */
	private static class NamedByteArrayResource extends ByteArrayResource {

		private final @Nullable String filename;

		NamedByteArrayResource(byte[] bytes, @Nullable String filename) {
			super(bytes);
			this.filename = filename;
		}

		@Override
		public @Nullable String getFilename() {
			return filename;
		}
	}
}

