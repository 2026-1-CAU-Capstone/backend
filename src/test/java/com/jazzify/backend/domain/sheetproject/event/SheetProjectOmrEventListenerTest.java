package com.jazzify.backend.domain.sheetproject.event;

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

import com.jazzify.backend.domain.sheetproject.entity.SheetProject;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetProjectOmrProcessor;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetProjectOmrWriter;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetProjectReader;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.domain.MusicKey;

@NullMarked
class SheetProjectOmrEventListenerTest {

	@Test
	void handle_completesProjectWhenOmrSucceeds() {
		UUID projectPublicId = UUID.randomUUID();
		CapturingSheetProjectOmrWriter writer = new CapturingSheetProjectOmrWriter();
		SheetProjectOmrEventListener listener = new SheetProjectOmrEventListener(
			new StubSheetProjectReader(projectPublicId),
			new SheetProjectOmrProcessor(null) {
				@Override
				public SheetProjectOmrData process(MultipartFile file) {
					return new SheetProjectOmrData("Autumn Leaves", MusicKey.G_MINOR, "4/4", "Am7 D7 | Gmaj7");
				}
			},
			writer
		);

		listener.handle(new SheetProjectOmrRequestedEvent(
			projectPublicId,
			"autumn-leaves.pdf",
			"application/pdf",
			"dummy".getBytes(StandardCharsets.UTF_8),
			null,
			null
		));

		assertThat(writer.progresses).containsExactly(10, 40, 80);
		assertThat(writer.completedProjectPublicId).isEqualTo(projectPublicId);
		assertThat(writer.completedTitle).isEqualTo("Autumn Leaves");
		assertThat(writer.completedKey).isEqualTo(MusicKey.G_MINOR);
		assertThat(writer.completedTimeSignature).isEqualTo("4/4");
		assertThat(writer.completedProgression).isEqualTo("Am7 D7 | Gmaj7");
		assertThat(writer.failureReason).isNull();
	}

	@Test
	void handle_prefersRequestedOverrides() {
		UUID projectPublicId = UUID.randomUUID();
		CapturingSheetProjectOmrWriter writer = new CapturingSheetProjectOmrWriter();
		SheetProjectOmrEventListener listener = new SheetProjectOmrEventListener(
			new StubSheetProjectReader(projectPublicId),
			new SheetProjectOmrProcessor(null) {
				@Override
				public SheetProjectOmrData process(MultipartFile file) {
					return new SheetProjectOmrData("Parsed Title", MusicKey.C_MAJOR, "3/4", "Cmaj7");
				}
			},
			writer
		);

		listener.handle(new SheetProjectOmrRequestedEvent(
			projectPublicId,
			"override.pdf",
			"application/pdf",
			"dummy".getBytes(StandardCharsets.UTF_8),
			"Manual Title",
			MusicKey.E_FLAT_MAJOR
		));

		assertThat(writer.completedTitle).isEqualTo("Manual Title");
		assertThat(writer.completedKey).isEqualTo(MusicKey.E_FLAT_MAJOR);
	}

	private static final class StubSheetProjectReader extends SheetProjectReader {

		private final UUID projectPublicId;

		private StubSheetProjectReader(UUID projectPublicId) {
			super(null);
			this.projectPublicId = projectPublicId;
		}

		@Override
		public Optional<SheetProject> findByPublicId(UUID publicId) {
			if (!projectPublicId.equals(publicId)) {
				return Optional.empty();
			}
			return Optional.of(SheetProject.builder()
				.title("pending")
				.keySignature(null)
				.user(User.builder().name("user").username("user").password("pw").build())
				.sheetFile(null)
				.build());
		}
	}

	private static final class CapturingSheetProjectOmrWriter extends SheetProjectOmrWriter {

		private final List<Integer> progresses = new ArrayList<>();
		private @Nullable UUID completedProjectPublicId;
		private @Nullable String completedTitle;
		private @Nullable MusicKey completedKey;
		private @Nullable String completedTimeSignature;
		private @Nullable String completedProgression;
		private @Nullable String failureReason;

		private CapturingSheetProjectOmrWriter() {
			super(null, null, null, null, null, null);
		}

		@Override
		public void markProcessing(UUID projectPublicId, int progress) {
			progresses.add(progress);
		}

		@Override
		public void complete(UUID projectPublicId, String title, @Nullable MusicKey key, String timeSignature,
			String progression) {
			this.completedProjectPublicId = projectPublicId;
			this.completedTitle = title;
			this.completedKey = key;
			this.completedTimeSignature = timeSignature;
			this.completedProgression = progression;
		}

		@Override
		public void fail(UUID projectPublicId, @Nullable String failureReason, int progress) {
			this.failureReason = failureReason;
		}
	}
}

