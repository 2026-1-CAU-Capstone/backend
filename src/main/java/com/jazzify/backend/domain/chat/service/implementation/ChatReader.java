package com.jazzify.backend.domain.chat.service.implementation;

import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.chat.entity.Chat;
import com.jazzify.backend.domain.chat.entity.ChatMessage;
import com.jazzify.backend.domain.chat.repository.ChatMessageRepository;
import com.jazzify.backend.domain.chat.repository.ChatRepository;
import com.jazzify.backend.shared.exception.code.ChatErrorCode;

import lombok.RequiredArgsConstructor;

@Component
@NullMarked
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatReader {

	private final ChatRepository chatRepository;
	private final ChatMessageRepository chatMessageRepository;

	public Chat getChatByPublicId(UUID publicId) {
		return chatRepository.findByPublicId(publicId)
			.orElseThrow(ChatErrorCode.CHAT_NOT_FOUND::toException);
	}

	public Chat getChatByPublicId(UUID publicId, UUID userPublicId) {
		return chatRepository.findByPublicIdAndUser_PublicId(publicId, userPublicId)
			.orElseThrow(ChatErrorCode.CHAT_NOT_FOUND::toException);
	}

	public Page<Chat> getChats(UUID userPublicId, Pageable pageable) {
		return chatRepository.findAllByUser_PublicId(userPublicId, pageable);
	}

	public List<ChatMessage> getMessages(Chat chat) {
		return chatMessageRepository.findAllByChatOrderBySortOrderAsc(chat);
	}
}

