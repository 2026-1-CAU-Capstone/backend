package com.jazzify.backend.domain.chat.service.implementation;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.chat.entity.Chat;
import com.jazzify.backend.domain.chat.entity.ChatMessage;
import com.jazzify.backend.domain.chat.model.ChatAnalysisCategory;
import com.jazzify.backend.domain.chat.model.ChatMessageDraft;
import com.jazzify.backend.domain.chat.model.ChatType;
import com.jazzify.backend.domain.chat.repository.ChatMessageRepository;
import com.jazzify.backend.domain.chat.repository.ChatRepository;
import com.jazzify.backend.domain.user.entity.User;

import lombok.RequiredArgsConstructor;

@Component
@NullMarked
@RequiredArgsConstructor
@Transactional
public class ChatWriter {

	private final ChatRepository chatRepository;
	private final ChatMessageRepository chatMessageRepository;

	public Chat create(
		User user,
		ChatType type,
		String title,
		@Nullable ChatAnalysisCategory category,
		@Nullable String songTitle
	) {
		Chat chat = Chat.builder()
			.user(user)
			.type(type)
			.title(title)
			.category(category)
			.songTitle(songTitle != null && !songTitle.isBlank() ? songTitle.trim() : null)
			.build();
		return chatRepository.save(chat);
	}

	public Chat updateMetadata(Chat chat, @Nullable ChatAnalysisCategory category, @Nullable String songTitle) {
		chat.updateMetadata(category, songTitle);
		return chatRepository.save(chat);
	}

	public void appendMessages(Chat chat, List<ChatMessageDraft> drafts) {
		if (drafts.isEmpty()) {
			return;
		}

		int nextSortOrder = chatMessageRepository.findTopByChatOrderBySortOrderDesc(chat)
			.map(found -> found.getSortOrder() + 1)
			.orElse(0);

		for (ChatMessageDraft draft : drafts) {
			ChatMessage message = ChatMessage.builder()
				.chat(chat)
				.role(draft.role())
				.content(draft.content())
				.sortOrder(nextSortOrder++)
				.build();
			chatMessageRepository.save(message);
		}
		chat.increaseMessageCount(drafts.size());
		chatRepository.save(chat);
	}
}


