package com.jazzify.backend.domain.chordproject.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import com.jazzify.backend.domain.analysis.service.HarmonicAnalysisService;
import com.jazzify.backend.domain.chordinfo.service.implementation.ChordAnalysisReader;
import com.jazzify.backend.domain.chordinfo.service.implementation.ChordAnalysisWriter;
import com.jazzify.backend.domain.chordinfo.service.implementation.ChordInfoReader;
import com.jazzify.backend.domain.chordinfo.service.implementation.ChordInfoWriter;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.chordproject.model.ChordProjectOmrChord;
import com.jazzify.backend.domain.chordproject.model.ChordProjectOmrSourceType;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectOmrProcessor;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectOmrWriter;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectReader;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectWriter;
import com.jazzify.backend.domain.sheetproject.dto.request.OmrCallbackRequest;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.domain.user.service.implementation.UserReader;
import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.exception.code.ChordProjectErrorCode;
import com.jazzify.backend.shared.omr.OmrClient;
import com.jazzify.backend.shared.omr.OmrProperties;

@NullMarked
@ExtendWith(MockitoExtension.class)
class ChordProjectServiceOmrMetadataTest {

	@Mock
	private ChordProjectReader chordProjectReader;

	@Mock
	private ChordProjectWriter chordProjectWriter;

	@Mock
	private ChordProjectOmrWriter chordProjectOmrWriter;

	@Mock
	private ChordProjectOmrProcessor chordProjectOmrProcessor;

	@Mock
	private TransactionTemplate transactionTemplate;

	@Mock
	private UserReader userReader;

	@Mock
	private ChordInfoReader chordInfoReader;

	@Mock
	private ChordInfoWriter chordInfoWriter;

	@Mock
	private ChordAnalysisReader chordAnalysisReader;

	@Mock
	private ChordAnalysisWriter chordAnalysisWriter;

	@Mock
	private HarmonicAnalysisService harmonicAnalysisService;

	@Mock
	private OmrClient omrClient;

	@Mock
	private OmrProperties omrProperties;

	@InjectMocks
	private ChordProjectService service;

	@Test
	void handleOmrCallback_failsWhenNeitherUserNorOmrProvidesRequiredKey() {
		UUID publicId = UUID.randomUUID();
		ChordProject project = pendingProject(publicId);
		when(chordProjectReader.findByPublicId(publicId)).thenReturn(Optional.of(project));
		when(chordProjectOmrProcessor.processJobResult(publicId.toString(), ChordProjectOmrSourceType.CHORD_CHART))
			.thenReturn(new ChordProjectOmrProcessor.ChordProjectOmrData(
				"OMR Title",
				null,
				"4/4",
				4,
				List.of(new ChordProjectOmrChord(1, "Cmaj7", 1.0, 4.0))
			));

		service.handleOmrCallback("", completedCallback(publicId));

		verify(chordProjectOmrWriter).fail(
			publicId,
			ChordProjectErrorCode.CHORD_PROJECT_KEY_REQUIRED.getMessage(),
			80
		);
		verify(chordProjectOmrWriter, never()).complete(eq(publicId), any(), any(), any(), anyInt(), any());
	}

	@Test
	void handleOmrCallback_preservesRequestedTitleEvenWhenItMatchesPendingTitleSentinel() {
		UUID publicId = UUID.randomUUID();
		ChordProject project = pendingProject(publicId);
		project.markOmrQueued("OMR Processing", MusicKey.C_MAJOR, null, ChordProjectOmrSourceType.CHORD_CHART);
		when(chordProjectReader.findByPublicId(publicId)).thenReturn(Optional.of(project));
		when(chordProjectOmrProcessor.processJobResult(publicId.toString(), ChordProjectOmrSourceType.CHORD_CHART))
			.thenReturn(new ChordProjectOmrProcessor.ChordProjectOmrData(
				"OMR Title",
				null,
				"4/4",
				4,
				List.of(new ChordProjectOmrChord(1, "Cmaj7", 1.0, 4.0))
			));

		service.handleOmrCallback("", completedCallback(publicId));

		verify(chordProjectOmrWriter).complete(
			publicId,
			"OMR Processing",
			MusicKey.C_MAJOR,
			"4/4",
			4,
			List.of(new ChordProjectOmrChord(1, "Cmaj7", 1.0, 4.0))
		);
	}

	private static ChordProject pendingProject(UUID publicId) {
		ChordProject project = ChordProject.builder()
			.title("OMR Processing")
			.keySignature(MusicKey.C_MAJOR)
			.timeSignature("4/4")
			.user(User.builder()
				.name("Tester")
				.username("tester")
				.password("password")
				.build())
			.build();
		project.markOmrQueued(null, null, null, ChordProjectOmrSourceType.CHORD_CHART);
		ReflectionTestUtils.setField(project, "publicId", publicId);
		return project;
	}

	private static OmrCallbackRequest completedCallback(UUID publicId) {
		return new OmrCallbackRequest(publicId.toString(), "completed", null, null, null, null);
	}
}
