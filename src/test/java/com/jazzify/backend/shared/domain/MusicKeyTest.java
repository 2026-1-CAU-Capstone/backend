package com.jazzify.backend.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

@NullMarked
class MusicKeyTest {

	@Test
	void fromAnalysisKey_acceptsEnumNamesAndCommonMusicNotation() {
		assertThat(MusicKey.fromAnalysisKey("B_FLAT_MAJOR")).isEqualTo(MusicKey.B_FLAT_MAJOR);
		assertThat(MusicKey.fromAnalysisKey("Bb")).isEqualTo(MusicKey.B_FLAT_MAJOR);
		assertThat(MusicKey.fromAnalysisKey("B flat major")).isEqualTo(MusicKey.B_FLAT_MAJOR);
		assertThat(MusicKey.fromAnalysisKey("F#m")).isEqualTo(MusicKey.F_SHARP_MINOR);
		assertThat(MusicKey.fromAnalysisKey("C-min")).isEqualTo(MusicKey.C_MINOR);
		assertThat(MusicKey.fromAnalysisKey("C Major")).isEqualTo(MusicKey.C_MAJOR);
	}

	@Test
	void fromAnalysisKey_returnsNullForBlankOrUnknownKey() {
		assertThat(MusicKey.fromAnalysisKey(null)).isNull();
		assertThat(MusicKey.fromAnalysisKey(" ")).isNull();
		assertThat(MusicKey.fromAnalysisKey("not-a-key")).isNull();
	}
}
