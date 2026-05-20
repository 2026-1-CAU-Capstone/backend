package com.jazzify.backend.domain.solo.service.implementation;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.solo.dto.response.SheetDataResponse;
import com.jazzify.backend.domain.solo.entity.Solo;
import com.jazzify.backend.domain.solo.repository.SoloMeasureRepository;
import com.jazzify.backend.domain.solo.repository.SoloRepository;
import com.jazzify.backend.domain.solo.util.SoloMapper;

import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
public class SoloSheetDataMigrationService {

	private static final Logger log = LoggerFactory.getLogger(SoloSheetDataMigrationService.class);
	private static final int BATCH_SIZE = 200;

	private final SoloRepository soloRepository;
	private final SoloMeasureRepository soloMeasureRepository;

	@Transactional
	public void migrateMissingSheetDataJson() {
		int migratedCount = 0;
		while (true) {
			Page<Long> page = soloRepository.findIdsWithMissingSheetDataJson(PageRequest.of(0, BATCH_SIZE));
			if (page.isEmpty()) {
				break;
			}

			for (Long soloId : page.getContent()) {
				Solo solo = findSolo(soloId);
				if (solo == null || solo.getSheetDataJson() != null) {
					continue;
				}

				SheetDataResponse legacySheetData = SoloMapper.toLegacySheetDataResponse(
					solo,
					soloMeasureRepository.findAllBySoloIdWithNotes(soloId)
				);
				solo.replaceSheetDataJson(SoloMapper.serializeSheetData(legacySheetData));
				soloRepository.save(solo);
				migratedCount++;
			}

			soloRepository.flush();
		}

		if (migratedCount > 0) {
			log.info("Solo sheetData JSON migration completed. migratedCount={}", migratedCount);
		}
	}

	private @Nullable Solo findSolo(Long soloId) {
		return soloRepository.findById(soloId).orElse(null);
	}
}

