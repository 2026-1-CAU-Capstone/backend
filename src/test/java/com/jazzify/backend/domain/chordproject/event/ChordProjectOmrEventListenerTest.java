package com.jazzify.backend.domain.chordproject.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectOmrProcessor;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectOmrWriter;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectReader;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.exception.code.ChordProjectErrorCode;

@NullMarked
class ChordProjectOmrEventListenerTest {

	@Test
	void handle_completesProjectWhenOmrSucceeds() {
		UUID projectPublicId = UUID.randomUUID();
		CapturingChordProjectOmrWriter writer = new CapturingChordProjectOmrWriter();
		ChordProjectOmrEventListener listener = new ChordProjectOmrEventListener(
			new StubChordProjectReader(projectPublicId),
			new ChordProjectOmrProcessor(null) {
				@Override
				public ChordProjectOmrData process(MultipartFile file) {
					return new ChordProjectOmrData("Blue Bossa", MusicKey.C_MINOR, "4/4", "Cm7 F7 | Bbmaj7");
				}
			},
			writer
		);

		listener.handle(new ChordProjectOmrRequestedEvent(
			projectPublicId,
			"blue-bossa.pdf",
			"application/pdf",
			"dummy".getBytes(StandardCharsets.UTF_8),
			null,
			null,
			null
		));

		assertThat(writer.progresses).containsExactly(10, 40, 80);
		assertThat(writer.completedProjectPublicId).isEqualTo(projectPublicId);
		assertThat(writer.completedTitle).isEqualTo("Blue Bossa");
		assertThat(writer.completedKey).isEqualTo(MusicKey.C_MINOR);
		assertThat(writer.completedTimeSignature).isEqualTo("4/4");
		assertThat(writer.completedProgression).isEqualTo("Cm7 F7 | Bbmaj7");
		assertThat(writer.failureReason).isNull();
	}

	@Test
	void handle_marksFailedWhenKeyCannotBeResolved() {
		UUID projectPublicId = UUID.randomUUID();
		CapturingChordProjectOmrWriter writer = new CapturingChordProjectOmrWriter();
		ChordProjectOmrEventListener listener = new ChordProjectOmrEventListener(
			new StubChordProjectReader(projectPublicId),
			new ChordProjectOmrProcessor(null) {
				@Override
				public ChordProjectOmrData process(MultipartFile file) {
					return new ChordProjectOmrData("Untitled", null, "4/4", "Cm7 F7");
				}
			},
			writer
		);

		listener.handle(new ChordProjectOmrRequestedEvent(
			projectPublicId,
			"unknown.pdf",
			"application/pdf",
			"dummy".getBytes(StandardCharsets.UTF_8),
			null,
			null,
			null
		));

		assertThat(writer.completedProjectPublicId).isNull();
		assertThat(writer.failedProjectPublicId).isEqualTo(projectPublicId);
		assertThat(writer.failureReason).isEqualTo(ChordProjectErrorCode.CHORD_PROJECT_KEY_REQUIRED.getMessage());
		assertThat(writer.failedProgress).isEqualTo(70);
	}

	private static final class StubChordProjectReader extends ChordProjectReader {
		private final UUID projectPublicId;

		private StubChordProjectReader(UUID projectPublicId) {
			super(null);
			this.projectPublicId = projectPublicId;
		}

		@Override
		public Optional<ChordProject> findByPublicId(UUID publicId) {
			if (!projectPublicId.equals(publicId)) {
				return Optional.empty();
			}
			return Optional.of(ChordProject.builder()
				.title("pending")
				.keySignature(MusicKey.C_MAJOR)
				.timeSignature("4/4")
				.user(User.builder().name("user").username("user").password("pw").build())
				.build());
		}
	}

	private static final class CapturingChordProjectOmrWriter extends ChordProjectOmrWriter {
		private final List<Integer> progresses = new ArrayList<>();
		private @Nullable UUID completedProjectPublicId;
		private @Nullable String completedTitle;
		private @Nullable MusicKey completedKey;
		private @Nullable String completedTimeSignature;
		private @Nullable String completedProgression;
		private @Nullable UUID failedProjectPublicId;
		private @Nullable String failureReason;
		private int failedProgress;

		private CapturingChordProjectOmrWriter() {
			super(null, null);
		}

		@Override
		public void markProcessing(UUID projectPublicId, int progress) {
			progresses.add(progress);
		}

		@Override
		public void complete(UUID projectPublicId, String title, MusicKey key, String timeSignature, String progression) {
			this.completedProjectPublicId = projectPublicId;
			this.completedTitle = title;
			this.completedKey = key;
			this.completedTimeSignature = timeSignature;
			this.completedProgression = progression;
		}

		@Override
		public void fail(UUID projectPublicId, @Nullable String failureReason, int progress) {
			this.failedProjectPublicId = projectPublicId;
			this.failureReason = failureReason;
			this.failedProgress = progress;
		}
	}
}



