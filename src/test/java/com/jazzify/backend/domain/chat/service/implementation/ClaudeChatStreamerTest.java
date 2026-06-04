package com.jazzify.backend.domain.chat.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jazzify.backend.domain.chat.dto.request.ChatImageRequest;
import com.jazzify.backend.domain.chat.dto.request.ChatStreamRequest;
import com.jazzify.backend.domain.chat.model.ChatAnalysisCategory;
import com.jazzify.backend.domain.chat.model.ChatHistoryMessage;
import com.jazzify.backend.shared.llm.AnthropicStreamingClient;

@NullMarked
@ExtendWith(MockitoExtension.class)
class ClaudeChatStreamerTest {

	@Mock
	private AnthropicStreamingClient anthropicStreamingClient;

	@InjectMocks
	private ClaudeChatStreamer claudeChatStreamer;

	@Test
	void stream_buildsCategoryContextAndImageMessages() {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		doAnswer(invocation -> "응답")
			.when(anthropicStreamingClient).streamPlainText(anyList(), any(ByteArrayOutputStream.class));

		ChatStreamRequest request = new ChatStreamRequest(
			"이 코드 진행을 어떻게 보면 돼?",
			List.of(),
			"Eb 키의 II7 코드입니다.",
			ChatAnalysisCategory.IMPROV,
			"It Could Happen to You",
			List.of(new ChatImageRequest("image/png", "YmFzZTY0ZGF0YQ==")),
			null
		);

		List<ChatHistoryMessage> history = List.of(new ChatHistoryMessage("assistant", "이전 응답"));
		String response = claudeChatStreamer.stream(request, history, outputStream);

		assertThat(claudeChatStreamer.buildSystem(request.category(), request.directChordContext(), request.songTitle()))
			.contains("현재 분석 중인 곡: It Could Happen to You")
			.contains("[Analysis Focus: Improvisation]")
			.contains("Do NOT emit a ```chart fenced block for THIS song");
		assertThat(claudeChatStreamer.buildMessages(request, history)).hasSize(3);
		assertThat(response).isEqualTo("응답");
	}
}


