package com.jazzify.backend.shared.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.util.DisconnectedClientHelper;

import reactor.core.publisher.Flux;

@NullMarked
@ExtendWith(MockitoExtension.class)
class AnthropicStreamingClientTest {

	@Mock
	private ObjectProvider<StreamingChatModel> streamingChatModelProvider;

	@Mock
	private StreamingChatModel streamingChatModel;

	private AnthropicStreamingClient anthropicStreamingClient;

	@BeforeEach
	void setUp() {
		anthropicStreamingClient = new AnthropicStreamingClient(streamingChatModelProvider);
		ReflectionTestUtils.setField(anthropicStreamingClient, "apiKey", "test-api-key");
		when(streamingChatModelProvider.getIfAvailable()).thenReturn(streamingChatModel);
	}

	@Test
	void streamPlainText_preservesClientDisconnectCause() {
		ChatResponse response = mock(ChatResponse.class);
		Generation generation = mock(Generation.class);
		when(response.getResult()).thenReturn(generation);
		when(generation.getOutput()).thenReturn(new AssistantMessage("partial response"));
		when(streamingChatModel.stream(any(Prompt.class))).thenReturn(Flux.just(response));

		OutputStream disconnectedOutputStream = new ByteArrayOutputStream() {
			@Override
			public void flush() throws IOException {
				throw new IOException("Broken pipe");
			}
		};

		Throwable thrown = catchThrowable(() -> anthropicStreamingClient.streamPlainText(
			List.of(new UserMessage("question")),
			disconnectedOutputStream
		));

		assertThat(thrown).isNotNull();
		assertThat(DisconnectedClientHelper.isClientDisconnectedException(thrown)).isTrue();
	}
}
