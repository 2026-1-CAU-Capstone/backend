package com.jazzify.backend.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;

@NullMarked
class GlobalExceptionHandlerTest {

	private final GlobalExceptionHandler globalExceptionHandler = new GlobalExceptionHandler();

	@Test
	void handleException_returnsNoBodyWhenClientDisconnected() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/v1/chat/chord-project/stream");
		AsyncRequestNotUsableException exception = new AsyncRequestNotUsableException(
			"ServletOutputStream failed to flush",
			new IOException("Broken pipe")
		);

		assertThat(globalExceptionHandler.handleException(exception, request)).isNull();
	}
}
