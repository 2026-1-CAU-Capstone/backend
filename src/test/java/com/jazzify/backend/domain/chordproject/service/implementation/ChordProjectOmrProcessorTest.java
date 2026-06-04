package com.jazzify.backend.domain.chordproject.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import com.jazzify.backend.shared.omr.OmrClient;
import com.jazzify.backend.shared.omr.OmrProperties;

@NullMarked
class ChordProjectOmrProcessorTest {

	@Test
	void processJobResult_fetchesChordChart() {
		String[] capturedJobId = {null};
		ChordProjectOmrProcessor processor = new ChordProjectOmrProcessor(new OmrClient(new OmrProperties("http://unused", null, null, null)) {
			@Override
			public ChordChartResult fetchChordChart(String jobId) {
				capturedJobId[0] = jobId;
				return new ChordChartResult("4/4", "Cm7 F7 | Bbmaj7");
			}
		});

		ChordProjectOmrProcessor.ChordProjectOmrData result = processor.processJobResult("job-123");
		assertThat(capturedJobId[0]).isEqualTo("job-123");
		assertThat(result.title()).isEqualTo("Untitled");
		assertThat(result.key()).isNull();
		assertThat(result.timeSignature()).isEqualTo("4/4");
		assertThat(result.progression()).isEqualTo("Cm7 F7 | Bbmaj7");
	}

	@Test
	void processJobResult_buildsChordProjectDataFromChordChart() {
		ChordProjectOmrProcessor processor = new ChordProjectOmrProcessor(new OmrClient(new OmrProperties("http://unused", null, null, null)) {
			@Override
			public ChordChartResult fetchChordChart(String jobId) {
				return new ChordChartResult("3/4", "Dm7 | G7 | Cmaj7");
			}
		});

		ChordProjectOmrProcessor.ChordProjectOmrData result = processor.processJobResult("job-456");

		assertThat(result.title()).isEqualTo("Untitled");
		assertThat(result.key()).isNull();
		assertThat(result.timeSignature()).isEqualTo("3/4");
		assertThat(result.progression()).isEqualTo("Dm7 | G7 | Cmaj7");
	}
}
