package com.jazzify.backend.shared.omr;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * OMR 서버 연동 설정.
 * <p>
 * {@code application.yml}에서 {@code omr.*} 키로 주입된다.
 *
 * <pre>
 * omr:
 *   server-url: ${OMR_SERVER_URL:}
 *   api-key: ${OMR_API_KEY:}
 *   callback-api-key: ${OMR_CALLBACK_API_KEY:}
 *   callback-url: ${OMR_CALLBACK_URL:}
 * </pre>
 *
 * @param serverUrl      OMR 서버 베이스 URL (미설정이면 null 또는 빈 문자열)
 * @param apiKey         OMR 서버 요청 시 X-OMR-API-Key 헤더값 (미설정이면 인증 생략)
 * @param callbackApiKey OMR 서버가 콜백 시 보내는 X-OMR-Callback-API-Key 헤더 검증용 값
 * @param callbackUrl    OMR 서버에 전달할 콜백 베이스 URL. endpoint dev/prod 분기는 active profile로 결정된다.
 *                       도메인별 콜백 경로는 {@link OmrCallbackDomain}에 따라 자동 부착됨.
 */
@NullMarked
@ConfigurationProperties(prefix = "omr")
public record OmrProperties(
	@Nullable String serverUrl,
	@Nullable String apiKey,
	@Nullable String callbackApiKey,
	@Nullable String callbackUrl
) {
}
