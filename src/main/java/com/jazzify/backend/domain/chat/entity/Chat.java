package com.jazzify.backend.domain.chat.entity;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.domain.chat.model.ChatAnalysisCategory;
import com.jazzify.backend.domain.chat.model.ChatType;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_chat")
@Getter
@NullMarked
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Chat extends BaseEntity {

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private ChatType type;

	@Column(nullable = false, length = 120)
	private String title;

	@Column(length = 50)
	@Enumerated(EnumType.STRING)
	private @Nullable ChatAnalysisCategory category;

	@Column(length = 255)
	private @Nullable String songTitle;

	@Column(nullable = false)
	private int messageCount;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@OneToMany(mappedBy = "chat", fetch = FetchType.LAZY)
	@OrderBy("sortOrder ASC")
	@Builder.Default
	private List<ChatMessage> messages = new ArrayList<>();

	public void updateMetadata(@Nullable ChatAnalysisCategory category, @Nullable String songTitle) {
		this.category = category;
		this.songTitle = normalizeNullable(songTitle);
	}

	public void increaseMessageCount(int delta) {
		this.messageCount += delta;
	}

	private static @Nullable String normalizeNullable(@Nullable String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.trim();
		return normalized.isBlank() ? null : normalized;
	}
}


