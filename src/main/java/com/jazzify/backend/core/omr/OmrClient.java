package com.jazzify.backend.core.omr;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.shared.exception.CustomException;
import com.jazzify.backend.shared.exception.code.OmrErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * OMR(Optical Music Recognition) 서버와 통신하는 HTTP 클라이언트.
 * <p>
 * 악보 이미지(PDF/PNG/JPG)를 전송하고 MusicXML 문자열을 수신한다.
 * OMR 서버 엔드포인트: {@code POST {omrServerUrl}/recognize}
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class OmrClient {

	private final OmrProperties omrProperties;

	/**
	 * 악보 파일을 OMR 서버에 전송하고 MusicXML 결과를 반환한다.
	 *
	 * @param file 악보 파일 (PDF · PNG · JPG)
	 * @return MusicXML 문자열
	 * @throws CustomException OMR 서버 미설정, 인식 실패 시
	 */
	public String recognize(MultipartFile file) {
		String serverUrl = omrProperties.serverUrl();
		if (serverUrl == null || serverUrl.isBlank()) {
			throw OmrErrorCode.OMR_SERVER_NOT_CONFIGURED.toException();
		}

		try {
			byte[] bytes = file.getBytes();

			MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
			body.add("file", new NamedByteArrayResource(bytes, file.getOriginalFilename()));

			@Nullable String result = RestClient.create(serverUrl)
				.post()
				.uri("/recognize")
				.contentType(MediaType.MULTIPART_FORM_DATA)
				.body(body)
				.retrieve()
				.body(String.class);

			if (result == null || result.isBlank()) {
				throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException("OMR 서버가 빈 응답을 반환했습니다.");
			}
			return result;

		} catch (CustomException e) {
			throw e;
		} catch (Exception e) {
			throw OmrErrorCode.OMR_RECOGNITION_FAILED.toException(e.getMessage());
		}
	}

	// ─── Inner Helper ───────────────────────────────────────────────────

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

