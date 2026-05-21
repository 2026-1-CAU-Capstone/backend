package com.jazzify.backend.domain.rag.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jazzify.backend.domain.chat.model.ChatHistoryMessage;
import com.jazzify.backend.domain.rag.dto.request.RagChatRequest;
import com.jazzify.backend.shared.llm.AnthropicStreamingClient;

@NullMarked
@ExtendWith(MockitoExtension.class)
class RagChatStreamerTest {

	@Mock
	private RagAgent ragAgent;

	@Mock
	private AnthropicStreamingClient anthropicStreamingClient;

	@InjectMocks
	private RagChatStreamer ragChatStreamer;

	@Test
	void stream_writesDebugBlockThenStreamsClaudeText() {
		Map<String, Object> debugInfo = new LinkedHashMap<>();
		debugInfo.put("fusion", "rrf");
		debugInfo.put("rrf_k", 60);
		debugInfo.put("queries", List.of(Map.of("query", "All of Me 코드 분석 화성")));
		when(ragAgent.buildContext(any(), anyString(), any())).thenReturn(
			new RagAgent.ContextBuildResult("[관련 강의 내용 (HarmoRAG)]\n\n[1] All of Me — Guide", debugInfo)
		);
		doAnswer(invocation -> {
			ByteArrayOutputStream out = invocation.getArgument(1);
			out.write("LLM응답".getBytes(StandardCharsets.UTF_8));
			return "LLM응답";
		}).when(anthropicStreamingClient).streamPlainText(anyList(), any(ByteArrayOutputStream.class));

		RagChatRequest request = new RagChatRequest(
			"이 F7 위에서 어떻게 솔로해?",
			Map.of("chord", "F7", "key", "Eb", "function", "II7"),
			"Eb 키의 II7 코드입니다.",
			List.of(),
			"It Could Happen to You",
			true,
			null
		);

		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		ragChatStreamer.stream(request, List.of(new ChatHistoryMessage("assistant", "이전 응답")), outputStream);

		String streamed = outputStream.toString(StandardCharsets.UTF_8);
		assertThat(streamed)
			.contains("\u0000RAG_DEBUG\u0000")
			.contains("\u0000END_DEBUG\u0000")
			.contains("\"fusion\":\"rrf\"")
			.contains("LLM응답");

		assertThat(ragChatStreamer.buildSystem(
			request.songTitle(),
			request.chordContextText(),
			"[관련 강의 내용 (HarmoRAG)]\n\n[1] All of Me — Guide",
			request.suppressInlineChart()
		))
			.contains("현재 분석 중인 곡: It Could Happen to You")
			.contains("[Rule-based 분석 결과]")
			.contains("[관련 강의 내용 (HarmoRAG)]");
	}
}

