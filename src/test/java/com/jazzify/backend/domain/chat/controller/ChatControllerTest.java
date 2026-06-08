package com.jazzify.backend.domain.chat.controller;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.chat.dto.request.ChatStreamRequest;
import com.jazzify.backend.domain.chat.service.ChatService;
import com.jazzify.backend.domain.rag.service.RagService;
import com.jazzify.backend.domain.user.entity.UserRole;

@NullMarked
@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

	@Mock
	private ChatService chatService;

	@Mock
	private ObjectProvider<RagService> ragServiceProvider;

	@InjectMocks
	private ChatController chatController;

	@Test
	void streamGlobal_ignoresClientDisconnectDuringStreaming() {
		CustomPrincipal principal = new CustomPrincipal(UUID.randomUUID(), "tester", UserRole.MEMBER);
		ChatStreamRequest request = new ChatStreamRequest(
			"question",
			List.of(),
			null,
			null,
			null,
			List.of(),
			null
		);
		ChatService.PreparedChatStream prepared = new ChatService.PreparedChatStream(UUID.randomUUID(), List.of());
		when(chatService.prepareDirectStream(any(), any(), any(), any())).thenReturn(prepared);
		doThrow(new IllegalStateException("Client disconnected", new IOException("Broken pipe")))
			.when(chatService).streamPreparedDirect(any(), any(), any());

		ResponseEntity<StreamingResponseBody> response = chatController.streamGlobal(principal, request);
		StreamingResponseBody body = Objects.requireNonNull(response.getBody());

		assertThatCode(() -> body.writeTo(new ByteArrayOutputStream())).doesNotThrowAnyException();
	}
}
