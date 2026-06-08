package com.jazzify.backend.domain.lick.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
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
import com.jazzify.backend.domain.lick.service.LickService;
import com.jazzify.backend.domain.lick.util.LickMapper;
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
		assertThat(lick.getComposer()).isEqualTo("Miles Davis");
		assertThat(LickMapper.parseSheetData(lick.getSheetDataJson()))
			.isNotNull()
			.extracting(it -> it.title(), it -> it.key())
			.containsExactly("So What Fragment", "C");
	}

	@Test
	void create_setsIsOMRTrue_forOmrCreate() {
		Lick lick = lickWriter.create(sampleRequest(), sampleHarmonicData(), sampleFeatures(), true);

		assertThat(lick.isOMR()).isTrue();
		assertThat(lickRepository.findById(Objects.requireNonNull(lick.getId())).orElseThrow().isOMR()).isTrue();
	}

	@Test
	void createPending_normalizesBlankMetadataToUnknown() {
		Lick lick = lickWriter.createPending(
			LickSource.USER,
			null,
			null,
			"   ",
			" ",
			null,
			null,
			Instrument.TP,
			null,
			null,
			null,
			null,
			null
		);

		assertThat(lick.getPerformer()).isEqualTo("Unknown");
		assertThat(lick.getComposer()).isEqualTo("Unknown");
		assertThat(lick.getTitle()).isEqualTo("Unknown");
		assertThat(lickRepository.findById(Objects.requireNonNull(lick.getId())).orElseThrow())
			.extracting(Lick::getPerformer, Lick::getComposer, Lick::getTitle)
			.containsExactly("Unknown", "Unknown", "Unknown");
	}

	@Test
	void buildOmrCreateRequest_appliesUserMetadataToSheetData() throws Exception {
		Lick lick = Lick.builder()
			.source(LickSource.USER)
			.isOMR(true)
			.performer("User Performer")
			.composer("User Composer")
			.title("User Title")
			.omrRequestedTitle("User Title")
			.omrRequestedComposer("User Composer")
			.album("User Album")
			.instrument(Instrument.TP)
			.tempo(180)
			.musicalKey("F")
			.timeSignature("3/4")
			.build();
		LickOmrProcessor.ProcessedSheetData processedSheetData = new LickOmrProcessor.ProcessedSheetData(
			"OMR Composer",
			new SheetDataRequest(
				"OMR Title",
				"C",
				"4/4",
				120,
				List.of(new MeasureRequest(
					"Dm7",
					List.of(new NoteInfoRequest(List.of("d/4"), "q", null, null, null, null, null, null))
				))
			)
		);

		LickCreateRequest request = invokeBuildOmrCreateRequest(lick, processedSheetData);

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
		Lick lick = Lick.builder()
			.source(LickSource.USER)
			.isOMR(true)
			.title("Untitled")
			.composer("Unknown")
			.instrument(Instrument.TP)
			.build();
		LickOmrProcessor.ProcessedSheetData processedSheetData = new LickOmrProcessor.ProcessedSheetData(
			"Parsed Composer",
			new SheetDataRequest(
				"Parsed Title",
				"C",
				"4/4",
				120,
				List.of(new MeasureRequest(
					"Dm7",
					List.of(new NoteInfoRequest(List.of("d/4"), "q", null, null, null, null, null, null))
				))
			)
		);

		LickCreateRequest request = invokeBuildOmrCreateRequest(lick, processedSheetData);

		assertThat(request.title()).isEqualTo("Parsed Title");
		assertThat(request.composer()).isEqualTo("Parsed Composer");
		assertThat(request.sheetData().title()).isEqualTo("Parsed Title");
	}

	@Test
	void buildOmrCreateRequest_usesDefaultsWhenUserAndOmrMetadataAreMissing() throws Exception {
		Lick lick = Lick.builder()
			.source(LickSource.USER)
			.isOMR(true)
			.title("Untitled")
			.composer("Unknown")
			.performer("Unknown")
			.instrument(Instrument.TP)
			.build();
		LickOmrProcessor.ProcessedSheetData processedSheetData = new LickOmrProcessor.ProcessedSheetData(
			null,
			new SheetDataRequest(
				" ",
				null,
				null,
				null,
				List.of(new MeasureRequest(
					"Dm7",
					List.of(new NoteInfoRequest(List.of("d/4"), "q", null, null, null, null, null, null))
				))
			)
		);

		LickCreateRequest request = invokeBuildOmrCreateRequest(lick, processedSheetData);

		assertThat(request.title()).isEqualTo("Untitled");
		assertThat(request.composer()).isEqualTo("Unknown");
		assertThat(request.performer()).isEqualTo("Unknown");
		assertThat(request.key()).isNull();
		assertThat(request.timeSignature()).isNull();
		assertThat(request.sheetData().title()).isEqualTo("Untitled");
	}

	private static LickCreateRequest invokeBuildOmrCreateRequest(
		Lick lick,
		LickOmrProcessor.ProcessedSheetData processedSheetData
	) throws Exception {
		LickService service = new LickService(null, null, null, null, null, null);
		Method method = LickService.class.getDeclaredMethod(
			"buildOmrCreateRequest",
			Lick.class,
			LickOmrProcessor.ProcessedSheetData.class
		);
		method.setAccessible(true);
		return (LickCreateRequest) method.invoke(service, lick, processedSheetData);
	}

	private static LickCreateRequest sampleRequest() {
		return new LickCreateRequest(
			LickSource.USER,
			null,
			"Miles Davis",
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


