package com.jazzify.backend.domain.chordproject.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.chordinfo.service.implementation.ChordInfoWriter;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.chordproject.model.ChordProjectOmrChord;
import com.jazzify.backend.domain.chordproject.repository.ChordProjectRepository;
import com.jazzify.backend.domain.session.entity.Session;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.domain.MusicKey;

import jakarta.persistence.EntityManager;

@NullMarked
@ExtendWith(MockitoExtension.class)
class ChordProjectOmrWriterTest {

	@Mock
	private ChordProjectRepository chordProjectRepository;

	@Mock
	private ChordInfoWriter chordInfoWriter;

	@Mock
	private EntityManager entityManager;

	@InjectMocks
	private ChordProjectOmrWriter writer;

	@Test
	void complete_preservesStructuredBarsAndBeatsWhenSavingChordInfos() {
		UUID projectPublicId = UUID.randomUUID();
		ChordProject project = project("Pending", "4/4");
		when(chordProjectRepository.findByPublicId(projectPublicId)).thenReturn(Optional.of(project));

		writer.complete(
			projectPublicId,
			"After You've Gone",
			MusicKey.C_MAJOR,
			"4/4",
			4,
			List.of(
				new ChordProjectOmrChord(3, "Dm7", 1.0, 2.0),
				new ChordProjectOmrChord(3, "Dm7", 3.0, 2.0),
				new ChordProjectOmrChord(2, null, 1.0, 4.0),
				new ChordProjectOmrChord(1, "Ab7", 3.0, 2.0),
				new ChordProjectOmrChord(1, "Em7", 1.0, 2.0)
			)
		);

		ArgumentCaptor<List<ChordInfo>> captor = chordInfoListCaptor();
		verify(chordInfoWriter).deleteAllByChordProject(project);
		verify(chordInfoWriter).saveAll(captor.capture());

		assertThat(captor.getValue())
			.extracting(
				ChordInfo::getChord,
				ChordInfo::getBar,
				ChordInfo::getBeat,
				ChordInfo::getDurationBeats,
				ChordInfo::getSortOrder
			)
			.containsExactly(
				tuple("Em7", 1, 1.0, 2.0, 1),
				tuple("Ab7", 1, 3.0, 2.0, 2),
				tuple(null, 2, 1.0, 4.0, 3),
				tuple("Dm7", 3, 1.0, 4.0, 4)
			);
		assertThat(project.getTitle()).isEqualTo("After You've Gone");
		assertThat(project.getTimeSignature()).isEqualTo("4/4");
	}

	@Test
	void complete_scalesOmrBeatPositionsWhenRequestedTimeSignatureOverridesSource() {
		UUID projectPublicId = UUID.randomUUID();
		ChordProject project = project("Pending", "4/4");
		when(chordProjectRepository.findByPublicId(projectPublicId)).thenReturn(Optional.of(project));

		writer.complete(
			projectPublicId,
			"Override",
			MusicKey.C_MAJOR,
			"3/4",
			4,
			List.of(
				new ChordProjectOmrChord(1, "Em7", 1.0, 2.0),
				new ChordProjectOmrChord(1, "Ab7", 3.0, 2.0)
			)
		);

		ArgumentCaptor<List<ChordInfo>> captor = chordInfoListCaptor();
		verify(chordInfoWriter).saveAll(captor.capture());

		assertThat(captor.getValue())
			.extracting(ChordInfo::getBeat, ChordInfo::getDurationBeats)
			.containsExactly(
				tuple(1.0, 1.5),
				tuple(2.5, 1.5)
			);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static ArgumentCaptor<List<ChordInfo>> chordInfoListCaptor() {
		return (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
	}

	private static ChordProject project(String title, String timeSignature) {
		Session session = Session.builder()
			.title(title)
			.build();
		return ChordProject.builder()
			.title(title)
			.keySignature(MusicKey.C_MAJOR)
			.timeSignature(timeSignature)
			.user(User.builder()
				.name("Tester")
				.username("tester")
				.password("password")
				.build())
			.session(session)
			.build();
	}
}
