package com.jazzify.backend.shared.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MusicKey {
	// C
	C_MAJOR("C"), C_SHARP_MAJOR("C#"), C_FLAT_MAJOR("Cb"),
	C_MINOR("Cm"), C_SHARP_MINOR("C#m"), C_FLAT_MINOR("Cbm"),
	// D
	D_MAJOR("D"), D_SHARP_MAJOR("D#"), D_FLAT_MAJOR("Db"),
	D_MINOR("Dm"), D_SHARP_MINOR("D#m"), D_FLAT_MINOR("Dbm"),
	// E
	E_MAJOR("E"), E_SHARP_MAJOR("E#"), E_FLAT_MAJOR("Eb"),
	E_MINOR("Em"), E_SHARP_MINOR("E#m"), E_FLAT_MINOR("Ebm"),
	// F
	F_MAJOR("F"), F_SHARP_MAJOR("F#"), F_FLAT_MAJOR("Fb"),
	F_MINOR("Fm"), F_SHARP_MINOR("F#m"), F_FLAT_MINOR("Fbm"),
	// G
	G_MAJOR("G"), G_SHARP_MAJOR("G#"), G_FLAT_MAJOR("Gb"),
	G_MINOR("Gm"), G_SHARP_MINOR("G#m"), G_FLAT_MINOR("Gbm"),
	// A
	A_MAJOR("A"), A_SHARP_MAJOR("A#"), A_FLAT_MAJOR("Ab"),
	A_MINOR("Am"), A_SHARP_MINOR("A#m"), A_FLAT_MINOR("Abm"),
	// B
	B_MAJOR("B"), B_SHARP_MAJOR("B#"), B_FLAT_MAJOR("Bb"),
	B_MINOR("Bm"), B_SHARP_MINOR("B#m"), B_FLAT_MINOR("Bbm");

	private final String analysisKey;
}

