package com.jazzify.backend.domain.lick.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.jazzify.backend.domain.lick.entity.Lick;
import com.jazzify.backend.domain.lick.entity.LickMeasure;
import com.jazzify.backend.domain.lick.entity.LickNote;
import com.jazzify.backend.domain.lick.entity.LickSource;
import com.jazzify.backend.domain.lick.repository.LickMeasureRepository;
import com.jazzify.backend.domain.lick.repository.LickRepository;
import com.jazzify.backend.domain.lick.dto.response.SheetDataResponse;
import com.jazzify.backend.domain.lick.util.LickMapper;
import com.jazzify.backend.shared.domain.Instrument;

@DataJpaTest
@Import(LickSheetDataMigrationService.class)
@NullMarked
class LickSheetDataMigrationServiceTest {

	@Autowired
	private LickRepository lickRepository;

	@Autowired
	private LickMeasureRepository lickMeasureRepository;

	@Autowired
	private LickSheetDataMigrationService migrationService;

	@Test
	void migrateMissingSheetDataJson_backfillsFromLegacyMeasuresAndNotes() {
		Lick lick = lickRepository.saveAndFlush(Lick.builder()
			.source(LickSource.USER)
			.isOMR(false)
			.performer("Charlie Parker")
			.composer("Charlie Parker")
			.title("Anthropology Fragment")
			.instrument(Instrument.AS)
			.tempo(220)
			.musicalKey("Bb-maj")
			.timeSignature("4/4")
			.build());

		LickMeasure measure = LickMeasure.builder()
			.lick(lick)
			.measureIndex(0)
			.chord("F7")
			.build();
		measure.addNote(LickNote.builder()
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
		measure.addNote(LickNote.builder()
			.measure(measure)
			.noteIndex(1)
			.keys("[\"c/5\"]")
			.duration("8")
			.dotted(true)
			.tie(true)
			.gliss(false)
			.beamBreak(true)
			.build());
		lickMeasureRepository.saveAndFlush(measure);

		migrationService.migrateMissingSheetDataJson();

		Lick migrated = lickRepository.findById(Objects.requireNonNull(lick.getId())).orElseThrow();
		SheetDataResponse sheetData = Objects.requireNonNull(LickMapper.parseSheetData(migrated.getSheetDataJson()));
		assertThat(migrated.getSheetDataJson()).isNotBlank();
		assertThat(migrated.getComposer()).isEqualTo("Charlie Parker");
		assertThat(sheetData.title()).isEqualTo("Anthropology Fragment");
		assertThat(sheetData.key()).isEqualTo("Bb-maj");
		assertThat(sheetData.timeSignature()).isEqualTo("4/4");
		assertThat(sheetData.tempo()).isEqualTo(220);
		assertThat(sheetData.measures()).hasSize(1);
		assertThat(sheetData.measures().getFirst().chord()).isEqualTo("F7");
		assertThat(sheetData.measures().getFirst().notes())
			.extracting(com.jazzify.backend.domain.lick.dto.response.NoteInfoResponse::duration)
			.containsExactly("8", "8");
	}
}



