package com.jazzify.backend.domain.chat.service.implementation;

import java.io.OutputStream;
import java.util.Base64;
import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;

import com.jazzify.backend.domain.chat.dto.request.ChatImageRequest;
import com.jazzify.backend.domain.chat.dto.request.ChatStreamRequest;
import com.jazzify.backend.domain.chat.model.ChatAnalysisCategory;
import com.jazzify.backend.domain.chat.model.ChatHistoryMessage;
import com.jazzify.backend.shared.llm.AnthropicStreamingClient;

import lombok.RequiredArgsConstructor;

@Component
@NullMarked
@RequiredArgsConstructor
public class ClaudeChatStreamer {

	private static final String BASE_SYSTEM = """
		You are Jazzify AI, a jazz harmony expert and educator.
		Respond in the same language the user writes in (Korean or English).
		Keep explanations concise but insightful. Use music theory terminology with brief explanations.
		Format with markdown: use **bold** for chord symbols and key terms, bullet points for lists.
		
		When you present a chord progression for a song or section, ALWAYS use a fenced
		```chart block containing JSON instead of an ASCII pipe diagram. The frontend
		renders this block as an iRealPro-style chord chart automatically.
		
		Schema:
		
		```chart
		{
		  "title": "Song Title",
		  "composer": "Composer (optional)",
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
		
		Rules:
		- Use jazz chord notation: Δ for major 7 (FΔ7), - for minor (G-7), ø for half-diminished, ° for diminished, alt/b9/#9/#11/b13 for altered dominants.
		- Each "bar" string is one measure. For bars with two chords, separate them with a single space ("G-7 C7").
		- Repeat sections explicitly (e.g. AABA = four section objects), not via repeat marks.
		- Section labels are short ("A", "A'", "B", "Bridge", "Intro", "Coda").
		- Output the JSON exactly — no comments, no trailing commas.
		- You may put text explanation BEFORE and AFTER the chart block, but the chart itself MUST be a clean fenced block.
		
		When you reference a section by its letter in prose (e.g. "the A section", "in the bridge"),
		wrap the letter/label with [SEC:X] so the frontend renders it as a small black filled
		section badge, just like the section labels printed on the chord chart itself.
		
		Examples:
		  - "[SEC:A] 섹션은 ii-V-I 진행이 두 번 나옵니다."
		  - "Notice how [SEC:B] modulates to the relative minor before returning to [SEC:A']."
		  - "Bridge ([SEC:B]) borrows from the parallel minor."
		
		Only wrap the section letter/label itself, not surrounding words. Use the same labels
		that appear in the chart (e.g. A, A', B, Bridge, Intro, Coda).
		""";

	private static final String CONTEXT_SUFFIX = """
		
		[CONTEXT NOTE] The user is currently viewing the chord chart referenced in the [Chord Analysis Context] above. Do NOT emit a ```chart fenced block for THIS song — they can already see it on screen. You may still use ```chart blocks for OTHER songs you reference.
		""";

	private final AnthropicStreamingClient anthropicStreamingClient;

	public String stream(ChatStreamRequest request, List<ChatHistoryMessage> history, OutputStream outputStream) {
		return anthropicStreamingClient.streamPlainText(
			buildMessages(request, history),
			outputStream
		);
	}

	public String buildSystem(@Nullable ChatAnalysisCategory category, @Nullable String chordContext, @Nullable String songTitle) {
		StringBuilder builder = new StringBuilder(BASE_SYSTEM);
		if (songTitle != null && !songTitle.isBlank()) {
			builder.append("\n\n현재 분석 중인 곡: ").append(songTitle);
		}
		if (category != null) {
			builder.append("\n\n[Analysis Focus: ").append(category.label()).append("]\n").append(category.prompt());
		}
		if (chordContext != null && !chordContext.isBlank()) {
			builder.append(CONTEXT_SUFFIX);
		}
		return builder.toString();
	}

	public List<Message> buildMessages(ChatStreamRequest request, List<ChatHistoryMessage> history) {
		List<Message> messages = new ArrayList<>();
		String chordContext = request.directChordContext();
		messages.add(new SystemMessage(buildSystem(request.analysisCategory(), chordContext, request.songTitle())));
		for (ChatHistoryMessage historyMessage : history) {
			messages.add(toTextMessage(historyMessage.role(), historyMessage.content()));
		}
		messages.add(toCurrentUserMessage(request.message(), chordContext, request.images()));
		return List.copyOf(messages);
	}

	private Message toCurrentUserMessage(String message, @Nullable String chordContext, List<ChatImageRequest> images) {
		String fullUserMessage = buildUserContent(message, chordContext);
		if (images.isEmpty()) {
			return toTextMessage("user", fullUserMessage);
		}

		List<Media> mediaList = new ArrayList<>();
		for (ChatImageRequest image : images) {
			mediaList.add(Media.builder()
				.mimeType(MimeType.valueOf(image.mediaType()))
				.data(new ByteArrayResource(Base64.getDecoder().decode(image.data())))
				.build());
		}
		return UserMessage.builder()
			.text(fullUserMessage)
			.media(mediaList)
			.build();
	}

	private Message toTextMessage(String role, String content) {
		return switch (role) {
			case "assistant" -> new AssistantMessage(content);
			default -> new UserMessage(content);
		};
	}

	private String buildUserContent(String message, @Nullable String chordContext) {
		if (chordContext == null || chordContext.isBlank()) {
			return message;
		}
		return "[Chord Analysis Context]\n" + chordContext + "\n\n[User Question]\n" + message;
	}
}

