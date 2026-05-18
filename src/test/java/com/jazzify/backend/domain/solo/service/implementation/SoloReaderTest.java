package com.jazzify.backend.domain.solo.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;

import com.jazzify.backend.domain.solo.entity.Solo;
import com.jazzify.backend.domain.solo.entity.SoloSource;
import com.jazzify.backend.domain.solo.repository.SoloRepository;
import com.jazzify.backend.shared.domain.Instrument;

@DataJpaTest
@Import(SoloReader.class)
@NullMarked
class SoloReaderTest {

	@Autowired
	private SoloRepository soloRepository;

	@Autowired
	private SoloReader soloReader;

	@BeforeEach
	void setUp() {
		soloRepository.deleteAll();

		saveSolo("Anthropology Solo", "Charlie Parker", "Charlie Parker");
		saveSolo("Donna Lee Solo", "Charlie Parker", "Charlie Parker");
		saveSolo("Round Midnight Solo", "Thelonious Monk", "Charlie Parker");
		saveSolo("Straight No Chaser Solo", "Thelonious Monk", "Dizzy Gillespie");
		saveSolo("Unknown Composer Solo", null, "Miles Davis");
	}

	@Test
	void getAll_filtersByComposerAndPerformer_caseInsensitive() {
		var page = soloReader.getAll(PageRequest.of(0, 10), "thelonious monk", "charlie parker");

		assertThat(page.getContent())
			.hasSize(1)
			.extracting(Solo::getTitle)
			.containsExactly("Round Midnight Solo");
	}

	@Test
	void getAll_ignoresBlankFilters() {
		var page = soloReader.getAll(PageRequest.of(0, 10), "   ", null);

		assertThat(page.getTotalElements()).isEqualTo(5);
	}

	@Test
	void getComposerCounts_returnsCountsAndExcludesNullComposer() {
		var counts = soloReader.getComposerCounts("Charlie Parker");

		assertThat(counts)
			.extracting(it -> it.name(), it -> it.count())
			.containsExactly(
				tuple("Charlie Parker", 2L),
				tuple("Thelonious Monk", 1L)
			);
	}

	@Test
	void getPerformerCounts_returnsCountsForComposer() {
		var counts = soloReader.getPerformerCounts("Thelonious Monk");

		assertThat(counts)
			.extracting(it -> it.name(), it -> it.count())
			.containsExactly(
				tuple("Charlie Parker", 1L),
				tuple("Dizzy Gillespie", 1L)
			);
	}

	private void saveSolo(String title, @Nullable String composer, String performer) {
		soloRepository.save(Solo.builder()
			.source(SoloSource.USER)
			.title(title)
			.composer(composer)
			.performer(performer)
			.instrument(Instrument.AS)
			.build());
	}
}


