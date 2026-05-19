package com.jazzify.backend.domain.chordproject.event;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectOmrProcessor;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectOmrWriter;
import com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectReader;
import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.exception.CustomException;
import com.jazzify.backend.shared.exception.code.ChordProjectErrorCode;
import com.jazzify.backend.shared.omr.InMemoryMultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
public class ChordProjectOmrEventListener {

	private final ChordProjectReader chordProjectReader;
	private final ChordProjectOmrProcessor chordProjectOmrProcessor;
	private final ChordProjectOmrWriter chordProjectOmrWriter;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(ChordProjectOmrRequestedEvent event) {
		if (chordProjectReader.findByPublicId(event.projectPublicId()).isEmpty()) {
			return;
		}

		chordProjectOmrWriter.markProcessing(event.projectPublicId(), 10);
		try {
			InMemoryMultipartFile file = new InMemoryMultipartFile(
				"file",
				event.originalFilename(),
				event.contentType(),
				event.fileData()
			);

			chordProjectOmrWriter.markProcessing(event.projectPublicId(), 40);
			ChordProjectOmrProcessor.ChordProjectOmrData omrData = chordProjectOmrProcessor.process(file);

			String title = hasText(event.requestedTitle()) ? event.requestedTitle().trim() : omrData.title();
			MusicKey key = event.requestedKey() != null ? event.requestedKey() : omrData.key();
			if (key == null) {
				chordProjectOmrWriter.fail(
					event.projectPublicId(),
					ChordProjectErrorCode.CHORD_PROJECT_KEY_REQUIRED.getMessage(),
					70
				);
				return;
			}

			String timeSignature = hasText(event.requestedTimeSignature())
				? event.requestedTimeSignature().trim()
				: omrData.timeSignature();

			chordProjectOmrWriter.markProcessing(event.projectPublicId(), 80);
			chordProjectOmrWriter.complete(
				event.projectPublicId(),
				title,
				key,
				timeSignature,
				omrData.progression()
			);
		} catch (CustomException e) {
			log.warn("ChordProject OMR failed: projectPublicId={}, code={}, message={}",
				event.projectPublicId(), e.getCode(), e.getMessage());
			chordProjectOmrWriter.fail(event.projectPublicId(), e.getMessage(), 80);
		} catch (Exception e) {
			log.error("ChordProject OMR unexpected failure: projectPublicId={}", event.projectPublicId(), e);
			chordProjectOmrWriter.fail(event.projectPublicId(), e.getMessage(), 80);
		}
	}

	private static boolean hasText(@Nullable String value) {
		return value != null && !value.isBlank();
	}
}

