package com.jazzify.backend.domain.lick.entity;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.shared.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_lick")
@Getter
@NullMarked
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Lick extends BaseEntity {

	@Column(nullable = false)
	private String title;

	@Column
	private @Nullable String composer;

	@Lob
	@Column(nullable = false, columnDefinition = "TEXT")
	private String content;

	public void update(String title, @Nullable String composer, String content) {
		this.title = title;
		this.composer = composer;
		this.content = content;
	}
}

