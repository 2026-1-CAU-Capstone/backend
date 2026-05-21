package com.jazzify.backend.domain.chat.entity;

import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.shared.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_chat_message")
@Getter
@NullMarked
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ChatMessage extends BaseEntity {

	@Column(nullable = false, length = 20)
	private String role;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	@Column(nullable = false)
	private int sortOrder;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chat_id", nullable = false)
	private Chat chat;
}

