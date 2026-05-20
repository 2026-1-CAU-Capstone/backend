package com.jazzify.backend.domain.solo.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.jazzify.backend.domain.solo.dto.response.SheetDataResponse;
import com.jazzify.backend.domain.solo.entity.Solo;
import com.jazzify.backend.domain.solo.entity.SoloMeasure;
import com.jazzify.backend.domain.solo.entity.SoloNote;
import com.jazzify.backend.domain.solo.entity.SoloSource;
import com.jazzify.backend.domain.solo.repository.SoloMeasureRepository;
import com.jazzify.backend.domain.solo.repository.SoloRepository;
import com.jazzify.backend.domain.solo.util.SoloMapper;
import com.jazzify.backend.shared.domain.Instrument;

@DataJpaTest
@Import(SoloSheetDataMigrationService.class)
@NullMarked
class SoloSheetDataMigrationServiceTest {

	@Autowired
	private SoloRepository soloRepository;

	@Autowired
	private SoloMeasureRepository soloMeasureRepository;

	@Autowired
	private SoloSheetDataMigrationService migrationService;

	@Test
	void migrateMissingSheetDataJson_backfillsFromLegacyMeasuresAndNotes() {
		Solo solo = soloRepository.saveAndFlush(Solo.builder()
			.source(SoloSource.USER)
			.isOMR(false)
			.performer("Charlie Parker")
			.composer("Charlie Parker")
			.title("Anthropology Solo")
			.instrument(Instrument.AS)
			.tempo(220)
			.musicalKey("Bb-maj")
			.timeSignature("4/4")
			.build());

		SoloMeasure measure = SoloMeasure.builder()
			.solo(solo)
			.measureIndex(0)
			.chord("F7")
			.build();
		measure.addNote(SoloNote.builder()
			.measure(measure)
			.noteIndex(0)
			.keys("[\"a/4\"]")
			.duration("8")
			.dotted(false)
			.tie(false)
			.gliss(false)
			.beamBreak(false)
			.accidentals("{\"0\":\"b\"}")
			.build());
		measure.addNote(SoloNote.builder()
			.measure(measure)
			.noteIndex(1)
			.keys("[\"c/5\"]")
			.duration("8")
			.dotted(true)
			.tie(true)
			.gliss(false)
			.beamBreak(true)
			.build());
		soloMeasureRepository.saveAndFlush(measure);

		migrationService.migrateMissingSheetDataJson();

		Solo migrated = soloRepository.findById(Objects.requireNonNull(solo.getId())).orElseThrow();
		SheetDataResponse sheetData = Objects.requireNonNull(SoloMapper.parseSheetData(migrated.getSheetDataJson()));
		assertThat(migrated.getSheetDataJson()).isNotBlank();
		assertThat(migrated.getComposer()).isEqualTo("Charlie Parker");
		assertThat(sheetData.title()).isEqualTo("Anthropology Solo");
		assertThat(sheetData.key()).isEqualTo("Bb-maj");
		assertThat(sheetData.timeSignature()).isEqualTo("4/4");
		assertThat(sheetData.tempo()).isEqualTo(220);
		assertThat(sheetData.measures()).hasSize(1);
		assertThat(sheetData.measures().getFirst().chord()).isEqualTo("F7");
		assertThat(sheetData.measures().getFirst().notes())
			.extracting(com.jazzify.backend.domain.solo.dto.response.NoteInfoResponse::duration)
			.containsExactly("8", "8");
	}
}

