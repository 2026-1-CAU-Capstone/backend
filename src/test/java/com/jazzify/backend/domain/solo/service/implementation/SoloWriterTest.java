package com.jazzify.backend.domain.solo.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import com.jazzify.backend.domain.solo.dto.request.MeasureRequest;
import com.jazzify.backend.domain.solo.dto.request.NoteInfoRequest;
import com.jazzify.backend.domain.solo.dto.request.SheetDataRequest;
import com.jazzify.backend.domain.solo.dto.request.SoloCreateRequest;
import com.jazzify.backend.domain.solo.entity.Solo;
import com.jazzify.backend.domain.solo.entity.SoloSource;
import com.jazzify.backend.domain.solo.model.SoloFeatures;
import com.jazzify.backend.domain.solo.model.SoloHarmonicData;
import com.jazzify.backend.domain.solo.repository.SoloRepository;
import com.jazzify.backend.shared.domain.HarmonicContext;
import com.jazzify.backend.shared.domain.Instrument;

@DataJpaTest
@Import(SoloWriter.class)
@NullMarked
class SoloWriterTest {

	@Autowired
	private SoloWriter soloWriter;

	@Autowired
	private SoloRepository soloRepository;

	@Test
	void create_setsIsOMRFalse_forRegularCreate() {
		Solo solo = soloWriter.create(sampleRequest(), sampleHarmonicData(), sampleFeatures(), false);

		assertThat(solo.isOMR()).isFalse();
		assertThat(solo.getComposer()).isEqualTo("Charlie Parker");
		assertThat(solo.getSheetDataJson()).isNotBlank();
		assertThat(solo.getMeasures()).isEmpty();
		assertThat(soloRepository.findById(Objects.requireNonNull(solo.getId())).orElseThrow().isOMR()).isFalse();
	}

	@Test
	void create_setsIsOMRTrue_forOmrCreate() {
		Solo solo = soloWriter.create(sampleRequest(), sampleHarmonicData(), sampleFeatures(), true);

		assertThat(solo.isOMR()).isTrue();
		assertThat(soloRepository.findById(Objects.requireNonNull(solo.getId())).orElseThrow().isOMR()).isTrue();
	}

	private static SoloCreateRequest sampleRequest() {
		return new SoloCreateRequest(
			SoloSource.USER,
			null,
			"Charlie Parker",
			"Charlie Parker",
			"Confirmation Solo",
			null,
			Instrument.AS,
			null,
			220,
			"F-maj",
			null,
			"4/4",
			null,
			null,
			null,
			null,
			new SheetDataRequest(
				"Confirmation Solo",
				"F",
				"4/4",
				220,
				List.of(new MeasureRequest(
					"Gm7",
					List.of(new NoteInfoRequest(List.of("g/4"), "q", null, null, null, null, null, null))
				))
			),
			null
		);
	}

	private static SoloHarmonicData sampleHarmonicData() {
		return new SoloHarmonicData(List.of("Gm7"), List.of("Gm7"), HarmonicContext.II_V_I, "Gm7");
	}

	private static SoloFeatures sampleFeatures() {
		return new SoloFeatures(1, List.of(67), List.of(), List.of(), List.of(), List.of(), 67, 67, 0, 67.0, 67, 67);
	}
}


