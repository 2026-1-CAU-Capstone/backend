package com.jazzify.backend.domain.sheetproject.event;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.jazzify.backend.domain.sheetproject.service.implementation.SheetProjectOmrProcessor;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetProjectOmrWriter;
import com.jazzify.backend.domain.sheetproject.service.implementation.SheetProjectReader;
import com.jazzify.backend.shared.domain.MusicKey;
import com.jazzify.backend.shared.exception.CustomException;
import com.jazzify.backend.shared.omr.InMemoryMultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
public class SheetProjectOmrEventListener {

	private final SheetProjectReader sheetProjectReader;
	private final SheetProjectOmrProcessor sheetProjectOmrProcessor;
	private final SheetProjectOmrWriter sheetProjectOmrWriter;

	@Async
	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void handle(SheetProjectOmrRequestedEvent event) {
		if (sheetProjectReader.findByPublicId(event.projectPublicId()).isEmpty()) {
			return;
		}

		sheetProjectOmrWriter.markProcessing(event.projectPublicId(), 10);
		try {
			InMemoryMultipartFile file = new InMemoryMultipartFile(
				"file",
				event.originalFilename(),
				event.contentType(),
				event.fileData()
			);

			sheetProjectOmrWriter.markProcessing(event.projectPublicId(), 40);
			SheetProjectOmrProcessor.SheetProjectOmrData omrData = sheetProjectOmrProcessor.process(file);

			String title = hasText(event.requestedTitle()) ? event.requestedTitle().trim() : omrData.title();
			MusicKey key = event.requestedKey() != null ? event.requestedKey() : omrData.key();

			sheetProjectOmrWriter.markProcessing(event.projectPublicId(), 80);
			sheetProjectOmrWriter.complete(
				event.projectPublicId(),
				title,
				key,
				omrData.timeSignature(),
				omrData.progression()
			);
		} catch (CustomException e) {
			log.warn("SheetProject OMR failed: projectPublicId={}, code={}, message={}",
				event.projectPublicId(), e.getCode(), e.getMessage());
			sheetProjectOmrWriter.fail(event.projectPublicId(), e.getMessage(), 80);
		} catch (Exception e) {
			log.error("SheetProject OMR unexpected failure: projectPublicId={}", event.projectPublicId(), e);
			sheetProjectOmrWriter.fail(event.projectPublicId(), e.getMessage(), 80);
		}
	}

	private static boolean hasText(@Nullable String value) {
		return value != null && !value.isBlank();
	}
}

