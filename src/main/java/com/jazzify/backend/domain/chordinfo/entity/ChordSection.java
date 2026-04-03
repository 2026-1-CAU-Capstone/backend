package com.jazzify.backend.domain.chordinfo.entity;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.shared.persistence.BaseEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_chord_section")
@Getter
@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChordSection extends BaseEntity {

	@Column(nullable = false)
	private int startBar;

	@Column(nullable = false)
	private int endBar;

	@Column(nullable = false, length = 30)
	private String sectionKey;

	@Column(nullable = false, length = 30)
	private String sectionType;

	@Column(length = 30)
	private @Nullable String mode;

	@Column(columnDefinition = "JSON")
	private @Nullable String tonicizations;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chord_project_id", nullable = false)
	private ChordProject chordProject;

	@Builder
	public ChordSection(int startBar, int endBar, String sectionKey, String sectionType,
		@Nullable String mode, @Nullable String tonicizations, ChordProject chordProject) {
		this.startBar = startBar;
		this.endBar = endBar;
		this.sectionKey = sectionKey;
		this.sectionType = sectionType;
		this.mode = mode;
		this.tonicizations = tonicizations;
		this.chordProject = chordProject;
	}
}

