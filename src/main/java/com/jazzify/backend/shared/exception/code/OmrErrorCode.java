package com.jazzify.backend.shared.exception.code;

import org.jspecify.annotations.NullMarked;
import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * OMR(Optical Music Recognition) 관련 에러 코드.
 */
@Getter
@NullMarked
@AllArgsConstructor
public enum OmrErrorCode implements BaseErrorCode {

	OMR_SERVER_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "OMR_001", "OMR 서버 주소가 설정되지 않았습니다."),
	OMR_RECOGNITION_FAILED(HttpStatus.BAD_GATEWAY, "OMR_002", "OMR 서버에서 악보 인식에 실패했습니다."),
	OMR_PARSE_FAILED(HttpStatus.UNPROCESSABLE_ENTITY, "OMR_003", "MusicXML 파싱에 실패했습니다."),
	OMR_INVALID_FILE_TYPE(HttpStatus.BAD_REQUEST, "OMR_004", "지원하지 않는 파일 형식입니다. PDF, PNG, JPG만 허용합니다."),
	OMR_FILE_EMPTY(HttpStatus.BAD_REQUEST, "OMR_005", "파일이 비어 있습니다.");

	private final HttpStatus httpStatus;
	private final String code;
	private final String message;
}

