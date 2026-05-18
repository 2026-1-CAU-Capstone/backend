package com.jazzify.backend.domain.lick.service.implementation;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@NullMarked
@Component
@RequiredArgsConstructor
public class LickSheetDataMigrationRunner implements ApplicationRunner {

	private final LickSheetDataMigrationService lickSheetDataMigrationService;

	@Override
	public void run(ApplicationArguments args) {
		lickSheetDataMigrationService.migrateMissingSheetDataJson();
	}
}

