package com.jazzify.backend.domain.lick.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.jazzify.backend.domain.lick.entity.Lick;
import com.jazzify.backend.domain.lick.entity.LickMeasure;
import com.jazzify.backend.domain.lick.entity.LickNote;
import com.jazzify.backend.domain.lick.entity.LickSource;
import com.jazzify.backend.domain.lick.repository.LickMeasureRepository;
import com.jazzify.backend.domain.lick.repository.LickRepository;
import com.jazzify.backend.shared.domain.Instrument;

@DataJpaTest
@Import({LickSheetDataMigrationService.class, LickSheetDataMigrationRunner.class})
@NullMarked
class LickSheetDataMigrationRunnerTest {

	@Autowired
	private LickRepository lickRepository;

	@Autowired
	private LickMeasureRepository lickMeasureRepository;

	@Autowired
	private LickSheetDataMigrationRunner migrationRunner;

	@Test
	void run_migratesLegacyLickDataViaSeparateRunnerBean() {
		Lick lick = lickRepository.saveAndFlush(Lick.builder()
			.source(LickSource.USER)
			.isOMR(false)
			.title("Runner Migration")
			.composer("Test Composer")
			.instrument(Instrument.AS)
			.tempo(180)
			.timeSignature("4/4")
			.musicalKey("C")
			.build());

		LickMeasure measure = LickMeasure.builder()
			.lick(lick)
			.measureIndex(0)
			.chord("Cmaj7")
			.build();
		measure.addNote(LickNote.builder()
			.measure(measure)
			.noteIndex(0)
			.keys("[\"c/4\"]")
			.duration("q")
			.build());
		lickMeasureRepository.saveAndFlush(measure);

		migrationRunner.run(new DefaultApplicationArguments());

		Lick migrated = lickRepository.findById(Objects.requireNonNull(lick.getId())).orElseThrow();
		assertThat(migrated.getSheetDataJson()).isNotBlank();
	}
}


