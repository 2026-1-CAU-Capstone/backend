package com.jazzify.backend.domain.rag.service.implementation;

import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jazzify.backend.domain.chat.model.ChatHistoryMessage;
import com.jazzify.backend.domain.rag.dto.request.RagChatRequest;
import com.jazzify.backend.shared.llm.AnthropicStreamingClient;

import lombok.RequiredArgsConstructor;

@Component
@NullMarked
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagChatStreamer {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
	private static final String RAG_OPEN = "\u0000RAG_DEBUG\u0000";
	private static final String RAG_CLOSE = "\u0000END_DEBUG\u0000";
	private static final String CONTEXT_SUFFIX = """
		
		[CONTEXT NOTE] The user is currently viewing the chord chart referenced in the [Rule-based 분석 결과] above. Do NOT emit a ```chart fenced block for THIS song — they can already see it on screen. You may still use ```chart blocks for OTHER songs you reference.
		""";
	private static final String BASE_SYSTEM = """
		당신은 Jazzify AI — 재즈 화성학·연주 전문 어시스턴트입니다.
		사용자가 당신의 정체에 대해 물어보면 "Jazzify AI"라고 답하세요. 절대 "HarmoRAG"나 다른 이름을 쓰지 마세요.
		첫 인사가 필요하면 "안녕하세요! Jazzify AI입니다." 같이 시작하세요.
		재즈 코드 진행, 즉흥연주 스케일, ii-V-I 패턴, 모달 인터체인지, 세컨더리 도미넌트 등을
		명확하고 실용적으로 설명합니다. 한국어로 답변합니다.
		
		[코드 진행 / 코드 차트 출력 — 가장 중요]
		곡이나 섹션의 코드 진행을 보여줄 때는 절대 마크다운 표(| 마디 | 코드 | ... |)나
		ASCII 다이어그램을 쓰지 마세요. **반드시** 아래 형식의 ```chart 펜스 블록(JSON)을
		사용하세요. 프론트엔드가 이 블록을 iReal Pro 스타일 리드시트(마디 세로선만 있는
		단조로운 악보)로 자동 렌더링합니다.
		
		```chart
		{
		  "title": "Song Title",
		  "composer": "작곡가 (선택)",
		  "key": "F",
		  "timeSig": "4/4",
		  "sections": [
		    {
		      "label": "A",
		      "bars": ["FΔ7", "D7", "G-7", "C7", "A-7", "D7", "G-7 C7", "FΔ7"]
		    }
		  ]
		}
		```
		
		chart 블록 규칙:
		- 재즈 코드 표기 사용: Δ=메이저7(FΔ7), -=마이너(G-7), ø=하프디미니쉬, °=디미니쉬,
		  알터드 도미넌트는 alt/b9/#9/#11/b13.
		- "bars" 의 각 문자열이 한 마디. 한 마디에 코드 2개면 공백 하나로 구분("G-7 C7").
		- AABA 같은 반복 구조는 반복 기호 대신 섹션 객체를 명시적으로 펼쳐서 작성.
		- 섹션 라벨은 짧게("A", "A'", "B", "Bridge", "Intro", "Coda").
		- JSON 은 정확하게 — 주석/트레일링 콤마 금지.
		- chart 블록 앞뒤로 설명 텍스트는 자유롭게 붙여도 되지만, 블록 자체는 깨끗한 펜스여야 함.
		
		본문에서 섹션을 글로 언급할 때(예: "A 섹션", "브릿지에서")는 그 라벨을 [SEC:X] 로
		감싸세요. 프론트엔드가 작은 검정 사각 배지로 렌더링합니다.
		  예: "[SEC:A] 섹션은 ii-V-I 가 두 번 나옵니다." / "[SEC:B] 는 평행단조로 전조됩니다."
		라벨 글자만 감싸고 주변 단어는 감싸지 마세요.
		
		[그 외 출력 포맷 및 페르소나 가이드라인]
		1. 코드 진행이 **아닌** 정보(스케일 비교, 텐션 정리, 개념 대조 등)를 구조화할 때만
		   마크다운 표를 사용하세요. 코드 진행은 위의 ```chart 블록만 사용합니다.
		2. 표를 쓸 때는 마크다운 규칙(파이프 | 와 하이픈 -)과 줄바꿈을 완벽하게 지키세요.
		   | 컬럼 1 | 컬럼 2 | 컬럼 3 |
		   |--------|--------|--------|
		   | 내용 A | 내용 B | 내용 C |
		3. 중요한 개념은 **굵은 글씨**나 이모지(🎯, 💡, ✅)를 사용해 눈에 띄게 하세요.
		4. 절대로 "강의에 따르면", "강의 내용에서", "제공된 문서에 의하면"과 같이 정보의 출처를 언급하지 마세요. 모든 정보는 Jazzify AI로서 당신이 본래 알고 있는 지식인 것처럼 자연스럽고 전문가답게 바로 설명하세요.
		5. 답변 맨 처음에 "🎷 '곡 제목' 솔로 아이디어 총정리" 같이 불필요하고 거창한 제목(Heading)을 달지 마세요. 인사말이나 제목 없이 곧바로 핵심적인 본론(질문에 대한 답)부터 시작하세요.
		6. 특정 연주자의 릭이나 라인을 언급할 때는 딱딱하게 나열하지 말고, 자연스럽고 대화하듯이 소개하세요. 예시: "좋아요, 릭을 알려드릴게요. 다음은 [연주자]의 [곡 제목]에서 나오는 이런 릭도 있어요! [릭 설명] 이런 부분에 대해선 어떠신가요?"
		
		중요: 아래 [관련 지식 내용]을 반드시 참고하여 답변하되, 외부 데이터를 참고했다는 티를 내지 마세요. 주어진 정보와 충돌하는 설명을 하지 마세요.
		""";

	private final RagAgent ragAgent;
	private final AnthropicStreamingClient anthropicStreamingClient;

	public String stream(RagChatRequest request, List<ChatHistoryMessage> history, OutputStream outputStream) {
		String ragContext = "";
		Map<String, Object> debugInfo;
		try {
			RagAgent.ContextBuildResult contextBuildResult = ragAgent.buildContext(
				request.chordContext(),
				request.message(),
				request.songTitle()
			);
			ragContext = contextBuildResult.llmContext();
			debugInfo = contextBuildResult.debugInfo();
		} catch (Exception e) {
			debugInfo = new LinkedHashMap<>();
			debugInfo.put("error", e.getMessage());
		}

		writeDebugBlock(outputStream, debugInfo);
		return anthropicStreamingClient.streamPlainText(
			buildMessages(request, history, ragContext),
			outputStream
		);
	}

	private void writeDebugBlock(OutputStream outputStream, Map<String, Object> debugInfo) {
		try {
			String debugPayload = RAG_OPEN + OBJECT_MAPPER.writeValueAsString(debugInfo) + RAG_CLOSE;
			outputStream.write(debugPayload.getBytes(StandardCharsets.UTF_8));
			outputStream.flush();
		} catch (Exception e) {
			throw new IllegalStateException("RAG debug block 직렬화에 실패했습니다.", e);
		}
	}

	String buildSystem(
		@Nullable String songTitle,
		@Nullable String chordContextText,
		@Nullable String ragContext,
		boolean suppressInlineChart
	) {
		StringBuilder builder = new StringBuilder(BASE_SYSTEM);
		if (songTitle != null && !songTitle.isBlank()) {
			builder.append("\n\n현재 분석 중인 곡: ").append(songTitle);
		}
		if (chordContextText != null && !chordContextText.isBlank()) {
			builder.append("\n\n[Rule-based 분석 결과]\n").append(chordContextText);
		}
		if (suppressInlineChart || (chordContextText != null && !chordContextText.isBlank())) {
			builder.append(CONTEXT_SUFFIX);
		}
		if (ragContext != null && !ragContext.isBlank()) {
			builder.append("\n\n").append(ragContext);
		}
		return builder.toString();
	}

	List<Message> buildMessages(RagChatRequest request, List<ChatHistoryMessage> history, @Nullable String ragContext) {
		List<Message> messages = new ArrayList<>();
		messages.add(new SystemMessage(buildSystem(
			request.songTitle(),
			request.chordContextText(),
			ragContext,
			request.suppressInlineChart()
		)));
		for (ChatHistoryMessage historyMessage : history) {
			messages.add(toTextMessage(historyMessage.role(), historyMessage.content()));
		}
		messages.add(toTextMessage("user", request.message()));
		return List.copyOf(messages);
	}

	private Message toTextMessage(String role, String content) {
		return switch (role) {
			case "assistant" -> new AssistantMessage(content);
			default -> new UserMessage(content);
		};
	}
}

