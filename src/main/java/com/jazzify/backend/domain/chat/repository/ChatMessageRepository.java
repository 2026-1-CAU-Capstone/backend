package com.jazzify.backend.domain.chat.repository;

import java.util.List;
import java.util.Optional;

import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jazzify.backend.domain.chat.entity.Chat;
import com.jazzify.backend.domain.chat.entity.ChatMessage;

@NullMarked
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findAllByChatOrderBySortOrderAsc(Chat chat);

	Optional<ChatMessage> findTopByChatOrderBySortOrderDesc(Chat chat);
}

