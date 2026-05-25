package com.jazzify.backend.shared.omr;

import org.jspecify.annotations.NullMarked;

/**
 * OMR 비동기 콜백 도메인.
 * <p>
 * {@code OmrClient#submitJob} 호출 시 도메인을 지정하면, 베이스 {@code callbackUrl}에
 * 도메인별 콜백 엔드포인트가 자동으로 부착되어 OMR 서버에 전달된다.
 */
@NullMarked
public enum OmrCallbackDomain {

	SOLO("/api/v1/solos/omr/callback"),
	LICK("/api/v1/licks/omr/callback"),
	CHORD_PROJECT("/api/v1/chord-projects/omr/callback"),
	SHEET_PROJECT("/api/v1/sheet-projects/omr/callback");

	private final String path;

	OmrCallbackDomain(String path) {
		this.path = path;
	}

	public String path() {
		return path;
	}
}
