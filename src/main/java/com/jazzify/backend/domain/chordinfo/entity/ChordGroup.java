package com.jazzify.backend.domain.chordinfo.entity;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.shared.persistence.BaseEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tb_chord_group")
@Getter
@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChordGroup extends BaseEntity {

	@Column(nullable = false)
	private int groupIndex;

	@Column(nullable = false, length = 30)
	private String groupType;

	@Column(nullable = false, length = 60)
	private String variant;

	@Column(nullable = false, length = 30)
	private String targetKey;

	@Column(nullable = false)
	private boolean isDiatonicTarget;

	@Column(columnDefinition = "TEXT")
	private String notes;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chord_project_id", nullable = false)
	private ChordProject chordProject;

	@OneToMany(mappedBy = "chordGroup", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
	private List<ChordGroupMember> members = new ArrayList<>();

	@Builder
	public ChordGroup(int groupIndex, String groupType, String variant,
		String targetKey, boolean isDiatonicTarget, String notes,
		ChordProject chordProject) {
		this.groupIndex = groupIndex;
		this.groupType = groupType;
		this.variant = variant;
		this.targetKey = targetKey;
		this.isDiatonicTarget = isDiatonicTarget;
		this.notes = notes;
		this.chordProject = chordProject;
	}
}

