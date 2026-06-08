package com.jazzify.backend.shared.llm;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import org.springframework.web.util.DisconnectedClientHelper;

import com.jazzify.backend.shared.exception.code.LlmErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@NullMarked
@RequiredArgsConstructor
public class AnthropicStreamingClient {

	private final ObjectProvider<StreamingChatModel> streamingChatModelProvider;
	@Value("${spring.ai.anthropic.api-key:}")
	private String apiKey;

	public boolean isConfigured() {
		return apiKey != null && !apiKey.isBlank() && streamingChatModelProvider.getIfAvailable() != null;
	}

	public String streamPlainText(List<Message> messages, OutputStream outputStream) {
		StreamingChatModel streamingChatModel = streamingChatModelProvider.getIfAvailable();
		if (streamingChatModel == null || apiKey == null || apiKey.isBlank()) {
			throw LlmErrorCode.LLM_NOT_CONFIGURED.toException();
		}

		StringBuilder accumulated = new StringBuilder();

		try {
			streamingChatModel.stream(new Prompt(messages))
				.doOnNext(response -> writeChunk(response, outputStream, accumulated))
				.blockLast();
			return accumulated.toString();
		} catch (Exception e) {
			if (DisconnectedClientHelper.isClientDisconnectedException(e)) {
				throw propagateClientDisconnect(e);
			}
			throw LlmErrorCode.LLM_REQUEST_FAILED.toException(e.getMessage());
		}
	}

	private void writeChunk(ChatResponse response, OutputStream outputStream, StringBuilder accumulated) {
		if (response.getResult() == null || response.getResult().getOutput() == null) {
			return;
		}
		String text = response.getResult().getOutput().getText();
		if (text == null || text.isEmpty()) {
			return;
		}

		try {
			accumulated.append(text);
			outputStream.write(text.getBytes(StandardCharsets.UTF_8));
			outputStream.flush();
		} catch (Exception e) {
			if (DisconnectedClientHelper.isClientDisconnectedException(e)) {
				throw propagateClientDisconnect(e);
			}
			throw LlmErrorCode.LLM_STREAM_FAILED.toException(e.getMessage());
		}
	}

	private RuntimeException propagateClientDisconnect(Exception exception) {
		if (exception instanceof RuntimeException runtimeException) {
			return runtimeException;
		}
		return new IllegalStateException("Client disconnected during LLM streaming.", exception);
	}
}

