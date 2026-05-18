package com.jazzify.backend.domain.lick.service.implementation;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.lick.dto.response.SheetDataResponse;
import com.jazzify.backend.domain.lick.entity.Lick;
import com.jazzify.backend.domain.lick.repository.LickMeasureRepository;
import com.jazzify.backend.domain.lick.repository.LickRepository;
import com.jazzify.backend.domain.lick.util.LickMapper;

import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
public class LickSheetDataMigrationService {

	private static final Logger log = LoggerFactory.getLogger(LickSheetDataMigrationService.class);
	private static final int BATCH_SIZE = 200;

	private final LickRepository lickRepository;
	private final LickMeasureRepository lickMeasureRepository;

	@Transactional
	public void migrateMissingSheetDataJson() {
		int migratedCount = 0;
		while (true) {
			Page<Long> page = lickRepository.findIdsWithMissingSheetDataJson(PageRequest.of(0, BATCH_SIZE));
			if (page.isEmpty()) {
				break;
			}

			for (Long lickId : page.getContent()) {
				Lick lick = findLick(lickId);
				if (lick == null || lick.getSheetDataJson() != null) {
					continue;
				}

				SheetDataResponse legacySheetData = LickMapper.toLegacySheetDataResponse(
					lick,
					lickMeasureRepository.findAllByLickIdWithNotes(lickId)
				);
				lick.replaceSheetDataJson(LickMapper.serializeSheetData(legacySheetData));
				lickRepository.save(lick);
				migratedCount++;
			}

			lickRepository.flush();
		}

		if (migratedCount > 0) {
			log.info("Lick sheetData JSON migration completed. migratedCount={}", migratedCount);
		}
	}

	private @Nullable Lick findLick(Long lickId) {
		return lickRepository.findById(lickId).orElse(null);
	}
}


