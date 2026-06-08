package com.jazzify.backend.domain.solo.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
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
import com.jazzify.backend.domain.solo.service.SoloService;
import com.jazzify.backend.domain.solo.util.SoloMapper;
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
		var sheetData = SoloMapper.parseSheetData(solo.getSheetDataJson());

		assertThat(solo.isOMR()).isFalse();
		assertThat(solo.getComposer()).isEqualTo("Charlie Parker");
		assertThat(sheetData).isNotNull();
		assertThat(Objects.requireNonNull(sheetData).title()).isEqualTo("Confirmation Solo");
		assertThat(sheetData.key()).isEqualTo("F");
		assertThat(soloRepository.findById(Objects.requireNonNull(solo.getId())).orElseThrow().isOMR()).isFalse();
	}

	@Test
	void create_setsIsOMRTrue_forOmrCreate() {
		Solo solo = soloWriter.create(sampleRequest(), sampleHarmonicData(), sampleFeatures(), true);

		assertThat(solo.isOMR()).isTrue();
		assertThat(soloRepository.findById(Objects.requireNonNull(solo.getId())).orElseThrow().isOMR()).isTrue();
	}

	@Test
	void createPending_normalizesBlankMetadataToUnknown() {
		Solo solo = soloWriter.createPending(
			SoloSource.USER,
			null,
			null,
			"   ",
			" ",
			null,
			null,
			Instrument.AS,
			null,
			null,
			null,
			null,
			null
		);

		assertThat(solo.getPerformer()).isEqualTo("Unknown");
		assertThat(solo.getComposer()).isEqualTo("Unknown");
		assertThat(solo.getTitle()).isEqualTo("Unknown");
		assertThat(soloRepository.findById(Objects.requireNonNull(solo.getId())).orElseThrow())
			.extracting(Solo::getPerformer, Solo::getComposer, Solo::getTitle)
			.containsExactly("Unknown", "Unknown", "Unknown");
	}

	@Test
	void buildOmrCreateRequest_appliesUserMetadataToSheetData() throws Exception {
		Solo solo = Solo.builder()
			.source(SoloSource.USER)
			.isOMR(true)
			.performer("User Performer")
			.composer("User Composer")
			.title("User Title")
			.omrRequestedTitle("User Title")
			.omrRequestedComposer("User Composer")
			.album("User Album")
			.instrument(Instrument.AS)
			.tempo(180)
			.musicalKey("F")
			.timeSignature("3/4")
			.build();
		SoloOmrProcessor.ProcessedSheetData processedSheetData = new SoloOmrProcessor.ProcessedSheetData(
			"OMR Composer",
			new SheetDataRequest(
				"OMR Title",
				"C",
				"4/4",
				120,
				List.of(new MeasureRequest(
					"Gm7",
					List.of(new NoteInfoRequest(List.of("g/4"), "q", null, null, null, null, null, null))
				))
			)
		);

		SoloCreateRequest request = invokeBuildOmrCreateRequest(solo, processedSheetData);

		assertThat(request.title()).isEqualTo("User Title");
		assertThat(request.composer()).isEqualTo("User Composer");
		assertThat(request.tempo()).isEqualTo(180);
		assertThat(request.key()).isEqualTo("F");
		assertThat(request.timeSignature()).isEqualTo("3/4");
		assertThat(request.sheetData())
			.extracting(SheetDataRequest::title, SheetDataRequest::key, SheetDataRequest::timeSignature, SheetDataRequest::tempo)
			.containsExactly("User Title", "F", "3/4", 180);
	}

	@Test
	void buildOmrCreateRequest_usesParsedTitleWhenUserTitleWasNotProvided() throws Exception {
		Solo solo = Solo.builder()
			.source(SoloSource.USER)
			.isOMR(true)
			.title("OMR Processing")
			.composer("Unknown")
			.instrument(Instrument.AS)
			.build();
		SoloOmrProcessor.ProcessedSheetData processedSheetData = new SoloOmrProcessor.ProcessedSheetData(
			"Parsed Composer",
			new SheetDataRequest(
				"Parsed Title",
				"C",
				"4/4",
				120,
				List.of(new MeasureRequest(
					"Gm7",
					List.of(new NoteInfoRequest(List.of("g/4"), "q", null, null, null, null, null, null))
				))
			)
		);

		SoloCreateRequest request = invokeBuildOmrCreateRequest(solo, processedSheetData);

		assertThat(request.title()).isEqualTo("Parsed Title");
		assertThat(request.composer()).isEqualTo("Parsed Composer");
		assertThat(request.sheetData().title()).isEqualTo("Parsed Title");
	}

	@Test
	void buildOmrCreateRequest_usesDefaultsWhenUserAndOmrMetadataAreMissing() throws Exception {
		Solo solo = Solo.builder()
			.source(SoloSource.USER)
			.isOMR(true)
			.title("OMR Processing")
			.composer("Unknown")
			.performer("Unknown")
			.instrument(Instrument.AS)
			.build();
		SoloOmrProcessor.ProcessedSheetData processedSheetData = new SoloOmrProcessor.ProcessedSheetData(
			null,
			new SheetDataRequest(
				" ",
				null,
				null,
				null,
				List.of(new MeasureRequest(
					"Gm7",
					List.of(new NoteInfoRequest(List.of("g/4"), "q", null, null, null, null, null, null))
				))
			)
		);

		SoloCreateRequest request = invokeBuildOmrCreateRequest(solo, processedSheetData);

		assertThat(request.title()).isEqualTo("Untitled");
		assertThat(request.composer()).isEqualTo("Unknown");
		assertThat(request.performer()).isEqualTo("Unknown");
		assertThat(request.key()).isNull();
		assertThat(request.timeSignature()).isNull();
		assertThat(request.sheetData().title()).isEqualTo("Untitled");
	}

	private static SoloCreateRequest invokeBuildOmrCreateRequest(
		Solo solo,
		SoloOmrProcessor.ProcessedSheetData processedSheetData
	) throws Exception {
		SoloService service = new SoloService(null, null, null, null, null, null);
		Method method = SoloService.class.getDeclaredMethod(
			"buildOmrCreateRequest",
			Solo.class,
			SoloOmrProcessor.ProcessedSheetData.class
		);
		method.setAccessible(true);
		return (SoloCreateRequest) method.invoke(service, solo, processedSheetData);
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


