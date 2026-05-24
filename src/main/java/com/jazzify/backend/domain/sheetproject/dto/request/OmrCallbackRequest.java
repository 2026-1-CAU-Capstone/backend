package com.jazzify.backend.domain.sheetproject.dto.request;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * OMR 서버가 처리 완료 후 백엔드 콜백 엔드포인트로 전송하는 페이로드.
 *
 * <p>completed 예시:
 * <pre>{@code
 * {
 *   "job_id": "...",
 *   "status": "completed",
 *   "message": "OMR processing completed",
 *   "musicxml_path": "jobs/.../output/score.musicxml",
 *   "chord_assignments_path": "jobs/.../output/chord_assignments.json"
 * }
 * }</pre>
 *
 * <p>failed 예시:
 * <pre>{@code
 * {
 *   "job_id": "...",
 *   "status": "failed",
 *   "message": "OMR processing failed",
 *   "error": "..."
 * }
 * }</pre>
 */
@NullMarked
@JsonIgnoreProperties(ignoreUnknown = true)
public record OmrCallbackRequest(
	@JsonProperty("job_id") String jobId,
	String status,
	@Nullable String message,
	@JsonProperty("musicxml_path") @Nullable String musicxmlPath,
	@JsonProperty("chord_assignments_path") @Nullable String chordAssignmentsPath,
	@Nullable String error
) {

	public boolean isCompleted() {
		return "completed".equalsIgnoreCase(status);
	}

	public boolean isFailed() {
		return "failed".equalsIgnoreCase(status);
	}
}

