package com.jazzify.backend.domain.lick.service.implementation;

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

import com.jazzify.backend.domain.lick.entity.Lick;
import com.jazzify.backend.domain.lick.entity.LickSource;
import com.jazzify.backend.domain.lick.repository.LickRepository;
import com.jazzify.backend.shared.domain.Instrument;

@DataJpaTest
@Import(LickReader.class)
@NullMarked
class LickReaderTest {

	@Autowired
	private LickRepository lickRepository;

	@Autowired
	private LickReader lickReader;

	@BeforeEach
	void setUp() {
		lickRepository.deleteAll();

		saveLick("Anthropology Fragment", "Charlie Parker", "Charlie Parker");
		saveLick("Donna Lee Fragment", "Charlie Parker", "Charlie Parker");
		saveLick("Round Midnight Fragment", "Thelonious Monk", "Charlie Parker");
		saveLick("Straight No Chaser Fragment", "Thelonious Monk", "Dizzy Gillespie");
		saveLick("Unknown Composer Fragment", null, "Miles Davis");
	}

	@Test
	void getAll_filtersByComposerAndPerformer_caseInsensitive() {
		var page = lickReader.getAll(PageRequest.of(0, 10), "thelonious monk", "charlie parker");

		assertThat(page.getContent())
			.hasSize(1)
			.extracting(Lick::getTitle)
			.containsExactly("Round Midnight Fragment");
	}

	@Test
	void getAll_ignoresBlankFilters() {
		var page = lickReader.getAll(PageRequest.of(0, 10), "   ", null);

		assertThat(page.getTotalElements()).isEqualTo(5);
	}

	@Test
	void getComposerCounts_returnsCountsAndExcludesNullComposer() {
		var counts = lickReader.getComposerCounts("Charlie Parker");

		assertThat(counts)
			.extracting(it -> it.name(), it -> it.count())
			.containsExactly(
				tuple("Charlie Parker", 2L),
				tuple("Thelonious Monk", 1L)
			);
	}

	@Test
	void getPerformerCounts_returnsCountsForComposer() {
		var counts = lickReader.getPerformerCounts("Thelonious Monk");

		assertThat(counts)
			.extracting(it -> it.name(), it -> it.count())
			.containsExactly(
				tuple("Charlie Parker", 1L),
				tuple("Dizzy Gillespie", 1L)
			);
	}

	private void saveLick(String title, @Nullable String composer, String performer) {
		lickRepository.save(Lick.builder()
			.source(LickSource.USER)
			.title(title)
			.composer(composer)
			.performer(performer)
			.instrument(Instrument.AS)
			.build());
	}
}


