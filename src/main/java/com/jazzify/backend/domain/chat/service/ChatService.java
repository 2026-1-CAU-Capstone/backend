package com.jazzify.backend.domain.chat.service;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.chat.dto.request.ChatStreamRequest;
import com.jazzify.backend.domain.chat.dto.response.ChatDetailResponse;
import com.jazzify.backend.domain.chat.dto.response.ChatSummaryResponse;
import com.jazzify.backend.domain.chat.entity.Chat;
import com.jazzify.backend.domain.chat.model.ChatAnalysisCategory;
import com.jazzify.backend.domain.chat.model.ChatHistoryMessage;
import com.jazzify.backend.domain.chat.model.ChatMessageDraft;
import com.jazzify.backend.domain.chat.model.ChatSourceCategory;
import com.jazzify.backend.domain.chat.model.ChatType;
import com.jazzify.backend.domain.chat.service.implementation.ClaudeChatStreamer;
import com.jazzify.backend.domain.chat.service.implementation.ChatReader;
import com.jazzify.backend.domain.chat.service.implementation.ChatWriter;
import com.jazzify.backend.domain.chat.util.ChatMapper;
import com.jazzify.backend.domain.rag.dto.request.RagChatRequest;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.domain.user.service.implementation.UserReader;
import com.jazzify.backend.shared.exception.code.ChatErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@NullMarked
@RequiredArgsConstructor
public class ChatService {

	private static final int TITLE_MAX_LENGTH = 120;

	private final ClaudeChatStreamer claudeChatStreamer;
	private final ChatReader chatReader;
	private final ChatWriter chatWriter;
	private final UserReader userReader;
	private final TransactionTemplate transactionTemplate;

	@Transactional(readOnly = true)
	public Page<ChatSummaryResponse> getChats(CustomPrincipal principal, Pageable pageable) {
		return chatReader.getChats(principal.publicId(), pageable)
			.map(ChatMapper::toSummaryResponse);
	}

	@Transactional(readOnly = true)
	public ChatDetailResponse getChat(CustomPrincipal principal, UUID publicId) {
		Chat chat = chatReader.getChatByPublicId(publicId, principal.publicId());
		return ChatMapper.toDetailResponse(chat, chatReader.getMessages(chat));
	}

	@Transactional
	public void delete(CustomPrincipal principal, UUID publicId) {
		Chat chat = chatReader.getChatByPublicId(publicId, principal.publicId());
		chatWriter.delete(chat);
	}

	@Transactional
	public PreparedChatStream prepareDirectStream(CustomPrincipal principal, ChatStreamRequest request) {
		return prepareDirectStream(principal, request, ChatType.GLOBAL, ChatSourceCategory.DIRECT);
	}

	@Transactional
	public PreparedChatStream prepareDirectStream(
		CustomPrincipal principal,
		ChatStreamRequest request,
		ChatType type,
		ChatSourceCategory sourceCategory
	) {
		return prepareStream(
			userReader.getByPublicId(principal.publicId()),
			request.chatPublicId(),
			type,
			sourceCategory,
			request.history().stream().map(ChatMapper::toHistoryMessage).toList(),
			request.message(),
			request.analysisCategory(),
			request.songTitle(),
			projectPublicId(sourceCategory, request.projectPublicId())
		);
	}

	@Transactional
	public PreparedChatStream prepareRagStream(CustomPrincipal principal, RagChatRequest request) {
		return prepareRagStream(principal, request, ChatType.GLOBAL, ChatSourceCategory.DIRECT);
	}

	@Transactional
	public PreparedChatStream prepareRagStream(
		CustomPrincipal principal,
		RagChatRequest request,
		ChatType type,
		ChatSourceCategory sourceCategory
	) {
		return prepareStream(
			userReader.getByPublicId(principal.publicId()),
			request.chatPublicId(),
			type,
			sourceCategory,
			request.history().stream().map(message -> new ChatHistoryMessage(message.role(), message.content())).toList(),
			request.message(),
			null,
			request.songTitle(),
			projectPublicId(sourceCategory, request.projectPublicId())
		);
	}

	public void streamPreparedDirect(PreparedChatStream preparedChatStream, ChatStreamRequest request, OutputStream outputStream) {
		String assistantMessage = null;
		try {
			assistantMessage = claudeChatStreamer.stream(request, preparedChatStream.history(), outputStream);
		} finally {
			persistTurn(preparedChatStream.chatPublicId(), request.message(), assistantMessage);
		}
	}

	public void persistTurn(UUID chatPublicId, String userMessage, @Nullable String assistantMessage) {
		transactionTemplate.executeWithoutResult(status -> {
			Chat chat = chatReader.getChatByPublicId(chatPublicId);
			List<ChatMessageDraft> drafts = new ArrayList<>();
			drafts.add(new ChatMessageDraft("user", userMessage));
			if (assistantMessage != null && !assistantMessage.isBlank()) {
				drafts.add(new ChatMessageDraft("assistant", assistantMessage));
			}
			chatWriter.appendMessages(chat, drafts);
		});
	}

	private PreparedChatStream prepareStream(
		User user,
		@Nullable UUID chatPublicId,
		ChatType type,
		ChatSourceCategory sourceCategory,
		List<ChatHistoryMessage> requestHistory,
		String pendingMessage,
		@Nullable ChatAnalysisCategory analysisCategory,
		@Nullable String songTitle,
		@Nullable String projectPublicId
	) {
		if (chatPublicId != null) {
			Chat existing = chatReader.getChatByPublicId(chatPublicId, requireUserPublicId(user));
			if (!isCompatibleType(existing.getType(), type)) {
				throw ChatErrorCode.CHAT_TYPE_MISMATCH.toException();
			}
			chatWriter.updateMetadata(existing, type, analysisCategory, sourceCategory, songTitle, projectPublicId);
			return new PreparedChatStream(
				requireChatPublicId(existing),
				chatReader.getMessages(existing).stream().map(ChatMapper::toHistoryMessage).toList()
			);
		}

		Chat chat = chatWriter.create(
			user,
			type,
			buildTitle(sourceCategory, requestHistory, pendingMessage, songTitle),
			analysisCategory,
			sourceCategory,
			songTitle,
			projectPublicId
		);
		if (!requestHistory.isEmpty()) {
			chatWriter.appendMessages(chat, requestHistory.stream().map(this::toDraft).toList());
		}
		return new PreparedChatStream(requireChatPublicId(chat), List.copyOf(requestHistory));
	}

	private ChatMessageDraft toDraft(ChatHistoryMessage historyMessage) {
		return new ChatMessageDraft(historyMessage.role(), historyMessage.content());
	}

	private String buildTitle(
		ChatSourceCategory sourceCategory,
		List<ChatHistoryMessage> requestHistory,
		String pendingMessage,
		@Nullable String songTitle
	) {
		String source = titleSource(sourceCategory, requestHistory, pendingMessage, songTitle);
		String normalized = source.replaceAll("\\s+", " ").trim();
		if (normalized.length() <= TITLE_MAX_LENGTH) {
			return normalized;
		}
		return normalized.substring(0, TITLE_MAX_LENGTH - 1) + "…";
	}

	private String titleSource(
		ChatSourceCategory sourceCategory,
		List<ChatHistoryMessage> requestHistory,
		String pendingMessage,
		@Nullable String songTitle
	) {
		if (sourceCategory != ChatSourceCategory.DIRECT && songTitle != null && !songTitle.isBlank()) {
			return songTitle;
		}
		return requestHistory.stream()
			.filter(message -> "user".equals(message.role()))
			.map(ChatHistoryMessage::content)
			.findFirst()
			.orElse(pendingMessage);
	}

	private boolean isCompatibleType(ChatType existing, ChatType requested) {
		if (existing == requested) {
			return true;
		}
		return existing.isLegacyAiMode();
	}

	private @Nullable String projectPublicId(
		ChatSourceCategory sourceCategory,
		@Nullable String requestedProjectPublicId
	) {
		if (sourceCategory == ChatSourceCategory.DIRECT) {
			return null;
		}
		if (requestedProjectPublicId == null) {
			return null;
		}
		String normalized = requestedProjectPublicId.trim();
		return normalized.isBlank() ? null : normalized;
	}

	private UUID requireUserPublicId(User user) {
		return Objects.requireNonNull(user.getPublicId(), "user.publicId must not be null");
	}

	private UUID requireChatPublicId(Chat chat) {
		return Objects.requireNonNull(chat.getPublicId(), "chat.publicId must not be null");
	}

	public record PreparedChatStream(
		UUID chatPublicId,
		List<ChatHistoryMessage> history
	) {
	}
}

