package com.jazzify.backend.domain.lick.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.jazzify.backend.domain.lick.dto.request.LickCreateRequest;
import com.jazzify.backend.domain.lick.dto.request.MeasureRequest;
import com.jazzify.backend.domain.lick.dto.request.NoteInfoRequest;
import com.jazzify.backend.domain.lick.dto.request.SheetDataRequest;
import com.jazzify.backend.domain.lick.entity.Lick;
import com.jazzify.backend.domain.lick.entity.LickSource;
import com.jazzify.backend.domain.lick.model.LickFeatures;
import com.jazzify.backend.domain.lick.model.LickHarmonicData;
import com.jazzify.backend.domain.lick.repository.LickRepository;
import com.jazzify.backend.shared.domain.HarmonicContext;
import com.jazzify.backend.shared.domain.Instrument;

@DataJpaTest
@Import(LickWriter.class)
@NullMarked
class LickWriterTest {

	@Autowired
	private LickWriter lickWriter;

	@Autowired
	private LickRepository lickRepository;

	@Test
	void create_setsIsOMRFalse_forRegularCreate() {
		Lick lick = lickWriter.create(sampleRequest(), sampleHarmonicData(), sampleFeatures(), false);

		assertThat(lick.isOMR()).isFalse();
		assertThat(lickRepository.findById(Objects.requireNonNull(lick.getId())).orElseThrow().isOMR()).isFalse();
	}

	@Test
	void create_setsIsOMRTrue_forOmrCreate() {
		Lick lick = lickWriter.create(sampleRequest(), sampleHarmonicData(), sampleFeatures(), true);

		assertThat(lick.isOMR()).isTrue();
		assertThat(lickRepository.findById(Objects.requireNonNull(lick.getId())).orElseThrow().isOMR()).isTrue();
	}

	private static LickCreateRequest sampleRequest() {
		return new LickCreateRequest(
			LickSource.USER,
			null,
			"Miles Davis",
			"So What Fragment",
			null,
			Instrument.TP,
			null,
			160,
			"C-maj",
			null,
			"4/4",
			null,
			null,
			null,
			null,
			new SheetDataRequest(
				"So What Fragment",
				"Miles Davis",
				"C",
				"4/4",
				160,
				List.of(new MeasureRequest(
					"Dm7",
					List.of(new NoteInfoRequest(List.of("d/4"), "q", null, null, null, null, null, null))
				))
			),
			null
		);
	}

	private static LickHarmonicData sampleHarmonicData() {
		return new LickHarmonicData(List.of("Dm7"), List.of("Dm7"), HarmonicContext.II_V_I, "Dm7");
	}

	private static LickFeatures sampleFeatures() {
		return new LickFeatures(1, List.of(62), List.of(), List.of(), List.of(), List.of(), 62, 62, 0, 62.0, 62, 62);
	}
}


