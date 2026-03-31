package com.jazzify.backend.domain.chordinfo.entity;

import org.jspecify.annotations.NullMarked;

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
@Table(name = "tb_chord_group_member")
@Getter
@NullMarked
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChordGroupMember extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chord_group_id", nullable = false)
	private ChordGroup chordGroup;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "chord_info_id", nullable = false)
	private ChordInfo chordInfo;

	@Column(nullable = false, length = 60)
	private String role;

	@Builder
	public ChordGroupMember(ChordGroup chordGroup, ChordInfo chordInfo, String role) {
		this.chordGroup = chordGroup;
		this.chordInfo = chordInfo;
		this.role = role;
	}
}

