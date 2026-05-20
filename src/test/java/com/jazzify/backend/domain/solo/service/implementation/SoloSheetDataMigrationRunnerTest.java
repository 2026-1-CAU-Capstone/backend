package com.jazzify.backend.domain.solo.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.jazzify.backend.domain.solo.entity.Solo;
import com.jazzify.backend.domain.solo.entity.SoloMeasure;
import com.jazzify.backend.domain.solo.entity.SoloNote;
import com.jazzify.backend.domain.solo.entity.SoloSource;
import com.jazzify.backend.domain.solo.repository.SoloMeasureRepository;
import com.jazzify.backend.domain.solo.repository.SoloRepository;
import com.jazzify.backend.shared.domain.Instrument;

@DataJpaTest
@Import({SoloSheetDataMigrationService.class, SoloSheetDataMigrationRunner.class})
@NullMarked
class SoloSheetDataMigrationRunnerTest {

	@Autowired
	private SoloRepository soloRepository;

	@Autowired
	private SoloMeasureRepository soloMeasureRepository;

	@Autowired
	private SoloSheetDataMigrationRunner migrationRunner;

	@Test
	void run_migratesLegacySoloDataViaSeparateRunnerBean() {
		Solo solo = soloRepository.saveAndFlush(Solo.builder()
			.source(SoloSource.USER)
			.isOMR(false)
			.title("Runner Migration")
			.composer("Test Composer")
			.instrument(Instrument.AS)
			.tempo(180)
			.timeSignature("4/4")
			.musicalKey("C")
			.build());

		SoloMeasure measure = SoloMeasure.builder()
			.solo(solo)
			.measureIndex(0)
			.chord("Cmaj7")
			.build();
		measure.addNote(SoloNote.builder()
			.measure(measure)
			.noteIndex(0)
			.keys("[\"c/4\"]")
			.duration("q")
			.build());
		soloMeasureRepository.saveAndFlush(measure);

		migrationRunner.run(new DefaultApplicationArguments());

		Solo migrated = soloRepository.findById(Objects.requireNonNull(solo.getId())).orElseThrow();
		assertThat(migrated.getSheetDataJson()).isNotBlank();
	}
}

