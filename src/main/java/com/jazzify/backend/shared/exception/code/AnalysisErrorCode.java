package com.jazzify.backend.shared.exception.code;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@NullMarked
@AllArgsConstructor
public enum AnalysisErrorCode implements BaseErrorCode {

	INVALID_KEY(HttpStatus.BAD_REQUEST, "ANALYSIS_001", "유효하지 않은 키입니다."),
	INVALID_CHORD_SYMBOL(HttpStatus.BAD_REQUEST, "ANALYSIS_002", "코드 기호를 파싱할 수 없습니다."),
	INVALID_ROOT_NOTE(HttpStatus.BAD_REQUEST, "ANALYSIS_003", "근음을 파싱할 수 없습니다."),
	NO_CHORDS_PARSED(HttpStatus.BAD_REQUEST, "ANALYSIS_004", "입력에서 코드를 파싱할 수 없습니다."),
	INVALID_TIME_SIGNATURE(HttpStatus.BAD_REQUEST, "ANALYSIS_005", "유효하지 않은 박자표입니다."),
	ANALYSIS_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ANALYSIS_006", "화성 분석 중 오류가 발생했습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}

