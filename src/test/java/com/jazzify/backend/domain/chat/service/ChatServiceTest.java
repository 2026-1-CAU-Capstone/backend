package com.jazzify.backend.domain.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionTemplate;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.user.entity.UserRole;
import com.jazzify.backend.domain.chat.dto.request.ChatMessageRequest;
import com.jazzify.backend.domain.chat.dto.request.ChatStreamRequest;
import com.jazzify.backend.domain.chat.entity.Chat;
import com.jazzify.backend.domain.chat.model.ChatAnalysisCategory;
import com.jazzify.backend.domain.chat.model.ChatMessageDraft;
import com.jazzify.backend.domain.chat.model.ChatSourceCategory;
import com.jazzify.backend.domain.chat.model.ChatType;
import com.jazzify.backend.domain.chat.service.implementation.ChatReader;
import com.jazzify.backend.domain.chat.service.implementation.ChatWriter;
import com.jazzify.backend.domain.chat.service.implementation.ClaudeChatStreamer;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.domain.user.service.implementation.UserReader;

@NullMarked
@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

	@Mock
	private ClaudeChatStreamer claudeChatStreamer;

	@Mock
	private ChatReader chatReader;

	@Mock
	private ChatWriter chatWriter;

	@Mock
	private UserReader userReader;

	@Mock
	private TransactionTemplate transactionTemplate;

	@Mock
	private User user;

	@Mock
	private Chat chat;

	private ChatService chatService;

	@BeforeEach
	void setUp() {
		chatService = new ChatService(claudeChatStreamer, chatReader, chatWriter, userReader, transactionTemplate);
	}

	@Test
	void prepareDirectStream_createsChatAndSeedsHistoryForNewConversation() {
		UUID userPublicId = UUID.randomUUID();
		UUID chatPublicId = UUID.randomUUID();
		when(userReader.getByPublicId(userPublicId)).thenReturn(user);
		when(chatWriter.create(
			eq(user),
			eq(ChatType.GLOBAL),
			any(),
			eq(ChatAnalysisCategory.IMPROV),
			eq(ChatSourceCategory.DIRECT),
			eq("Blue Bossa"),
			isNull()
		))
			.thenReturn(chat);
		when(chat.getPublicId()).thenReturn(chatPublicId);

		ChatStreamRequest request = new ChatStreamRequest(
			"ii-V-I를 설명해줘",
			List.of(new ChatMessageRequest("assistant", "이전 응답")),
			null,
			ChatAnalysisCategory.IMPROV,
			"Blue Bossa",
			List.of(),
			null
		);

		ChatService.PreparedChatStream prepared = chatService.prepareDirectStream(
			new CustomPrincipal(userPublicId, "tester", UserRole.MEMBER),
			request
		);

		assertThat(prepared.chatPublicId()).isEqualTo(chatPublicId);
		assertThat(prepared.history()).hasSize(1);
		verify(chatWriter).appendMessages(eq(chat), any());
	}

	@Test
	void prepareDirectStream_createsChordProjectChatWithProjectMetadata() {
		UUID userPublicId = UUID.randomUUID();
		UUID chatPublicId = UUID.randomUUID();
		String projectPublicId = UUID.randomUUID().toString();
		when(userReader.getByPublicId(userPublicId)).thenReturn(user);
		when(chatWriter.create(
			eq(user),
			eq(ChatType.CHORD_PROJECT),
			eq("Giant Steps"),
			isNull(),
			eq(ChatSourceCategory.CHORD),
			eq("Giant Steps"),
			eq(projectPublicId)
		)).thenReturn(chat);
		when(chat.getPublicId()).thenReturn(chatPublicId);

		ChatStreamRequest request = new ChatStreamRequest(
			"이 곡 설명해줘",
			List.of(),
			null,
			null,
			"Giant Steps",
			projectPublicId,
			List.of(),
			null,
			false,
			null,
			true
		);

		ChatService.PreparedChatStream prepared = chatService.prepareDirectStream(
			new CustomPrincipal(userPublicId, "tester", UserRole.MEMBER),
			request,
			ChatType.CHORD_PROJECT,
			ChatSourceCategory.CHORD
		);

		assertThat(prepared.chatPublicId()).isEqualTo(chatPublicId);
		assertThat(prepared.history()).isEmpty();
	}

	@Test
	void delete_verifiesOwnershipAndDeletesChat() {
		UUID userPublicId = UUID.randomUUID();
		UUID chatPublicId = UUID.randomUUID();
		when(chatReader.getChatByPublicId(chatPublicId, userPublicId)).thenReturn(chat);

		chatService.delete(new CustomPrincipal(userPublicId, "tester", UserRole.MEMBER), chatPublicId);

		verify(chatWriter).delete(chat);
	}

	@Test
	void persistTurn_appendsUserAndAssistantMessages() {
		UUID chatPublicId = UUID.randomUUID();
		when(chatReader.getChatByPublicId(chatPublicId)).thenReturn(chat);
		doAnswer(invocation -> {
			invocation.<Consumer<org.springframework.transaction.TransactionStatus>>getArgument(0).accept(null);
			return null;
		}).when(transactionTemplate).executeWithoutResult(any());

		chatService.persistTurn(chatPublicId, "질문", "응답");

		ArgumentCaptor<List<ChatMessageDraft>> draftsCaptor = ArgumentCaptor.forClass(List.class);
		verify(chatWriter).appendMessages(eq(chat), draftsCaptor.capture());
		assertThat(draftsCaptor.getValue())
			.extracting(ChatMessageDraft::role, ChatMessageDraft::content)
			.containsExactly(
				org.assertj.core.groups.Tuple.tuple("user", "질문"),
				org.assertj.core.groups.Tuple.tuple("assistant", "응답")
			);
	}
}
