package com.jazzify.backend.domain.chordproject.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.jazzify.backend.domain.chordinfo.entity.ChordAnalysis;
import com.jazzify.backend.domain.chordinfo.entity.ChordGroup;
import com.jazzify.backend.domain.chordinfo.entity.ChordGroupMember;
import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.chordinfo.entity.ChordSection;
import com.jazzify.backend.domain.chordinfo.repository.ChordAnalysisRepository;
import com.jazzify.backend.domain.chordinfo.repository.ChordGroupRepository;
import com.jazzify.backend.domain.chordinfo.repository.ChordInfoRepository;
import com.jazzify.backend.domain.chordinfo.repository.ChordSectionRepository;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.chordproject.repository.ChordProjectRepository;
import com.jazzify.backend.domain.sheetproject.entity.FileType;
import com.jazzify.backend.domain.sheetproject.entity.SheetFile;
import com.jazzify.backend.domain.sheetproject.entity.SheetProject;
import com.jazzify.backend.domain.sheetproject.repository.SheetFileRepository;
import com.jazzify.backend.domain.sheetproject.repository.SheetProjectRepository;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetProjectWriter;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.domain.user.repository.UserRepository;
import com.jazzify.backend.shared.domain.MusicKey;

import jakarta.persistence.EntityManager;

@DataJpaTest
@Import({ChordProjectWriter.class, SheetProjectWriter.class})
@NullMarked
class ProjectDeletionCascadeTest {

	@Autowired
	private ChordProjectWriter chordProjectWriter;

	@Autowired
	private SheetProjectWriter sheetProjectWriter;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ChordProjectRepository chordProjectRepository;

	@Autowired
	private SheetProjectRepository sheetProjectRepository;

	@Autowired
	private SheetFileRepository sheetFileRepository;

	@Autowired
	private ChordInfoRepository chordInfoRepository;

	@Autowired
	private ChordAnalysisRepository chordAnalysisRepository;

	@Autowired
	private ChordGroupRepository chordGroupRepository;

	@Autowired
	private ChordSectionRepository chordSectionRepository;

	@Autowired
	private EntityManager entityManager;

	@Test
	void deleteChordProject_removesChordAndAnalysisChildrenBeforeParent() {
		User user = saveUser("chord-owner");
		ChordProject project = chordProjectRepository.save(ChordProject.builder()
			.title("Delete chord project")
			.keySignature(MusicKey.C_MAJOR)
			.timeSignature("4/4")
			.user(user)
			.build());
		ChordInfo chordInfo = chordInfoRepository.save(ChordInfo.builder()
			.chord("Cmaj7")
			.bar(1)
			.beat(1)
			.durationBeats(4)
			.sortOrder(1)
			.chordProject(project)
			.build());
		ChordAnalysis analysis = chordAnalysisRepository.save(ChordAnalysis.builder()
			.ambiguityScore(0)
			.chordInfo(chordInfo)
			.build());
		chordInfo.assignAnalysis(analysis);

		ChordGroup group = ChordGroup.builder()
			.groupIndex(1)
			.groupType("ii_v_i")
			.variant("major")
			.targetKey("C")
			.isDiatonicTarget(true)
			.notes("")
			.chordProject(project)
			.build();
		group.getMembers().add(ChordGroupMember.builder()
			.chordGroup(group)
			.chordInfo(chordInfo)
			.role("I")
			.build());
		chordGroupRepository.save(group);
		chordSectionRepository.save(ChordSection.builder()
			.startBar(1)
			.endBar(1)
			.sectionKey("C")
			.sectionType("tonic")
			.chordProject(project)
			.build());
		entityManager.flush();
		entityManager.clear();

		ChordProject persisted = chordProjectRepository.findById(Objects.requireNonNull(project.getId())).orElseThrow();
		chordProjectWriter.delete(persisted);
		entityManager.flush();

		assertThat(chordProjectRepository.count()).isZero();
		assertThat(chordInfoRepository.count()).isZero();
		assertThat(chordAnalysisRepository.count()).isZero();
		assertThat(chordGroupRepository.count()).isZero();
		assertThat(chordSectionRepository.count()).isZero();
		assertThat(((Number) entityManager.createNativeQuery("select count(*) from tb_chord_group_member")
			.getSingleResult()).longValue()).isZero();
	}

	@Test
	void deleteSheetProject_removesChordInfosAndKeepsOrphanFileForCleanup() {
		User user = saveUser("sheet-owner");
		SheetFile sheetFile = sheetFileRepository.save(SheetFile.builder()
			.fileType(FileType.IMAGE)
			.build());
		SheetProject project = sheetProjectRepository.save(SheetProject.builder()
			.title("Delete sheet project")
			.keySignature(MusicKey.C_MAJOR)
			.user(user)
			.sheetFile(sheetFile)
			.build());
		chordInfoRepository.save(ChordInfo.builder()
			.chord("Dm7")
			.bar(1)
			.beat(1)
			.durationBeats(4)
			.sortOrder(1)
			.sheetProject(project)
			.build());
		entityManager.flush();
		entityManager.clear();

		SheetProject persisted = sheetProjectRepository.findById(Objects.requireNonNull(project.getId())).orElseThrow();
		sheetProjectWriter.delete(persisted);
		entityManager.flush();

		assertThat(sheetProjectRepository.count()).isZero();
		assertThat(chordInfoRepository.count()).isZero();
		assertThat(sheetFileRepository.count()).isOne();
	}

	private User saveUser(String username) {
		return userRepository.save(User.builder()
			.name(username)
			.username(username)
			.password("password")
			.build());
	}
}
