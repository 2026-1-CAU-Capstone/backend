package com.jazzify.backend.domain.solo.service.implementation;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
public class SoloSheetDataMigrationRunner implements ApplicationRunner {

	private final SoloSheetDataMigrationService soloSheetDataMigrationService;

	@Override
	public void run(ApplicationArguments args) {
		soloSheetDataMigrationService.migrateMissingSheetDataJson();
	}
}

