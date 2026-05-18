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
 * </pre>
 *
 * @param serverUrl OMR 서버 베이스 URL (미설정이면 null 또는 빈 문자열)
 */
@NullMarked
@ConfigurationProperties(prefix = "omr")
public record OmrProperties(
	@Nullable String serverUrl
) {
}

