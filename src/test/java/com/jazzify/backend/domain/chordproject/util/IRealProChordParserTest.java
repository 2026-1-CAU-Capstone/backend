package com.jazzify.backend.domain.chordproject.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;

import com.jazzify.backend.domain.chordinfo.entity.ChordInfo;
import com.jazzify.backend.domain.chordproject.entity.ChordProject;
import com.jazzify.backend.domain.session.entity.Session;
import com.jazzify.backend.domain.sheetproject.entity.FileType;
import com.jazzify.backend.domain.sheetproject.entity.SheetFile;
import com.jazzify.backend.domain.sheetproject.entity.SheetProject;
import com.jazzify.backend.domain.user.entity.User;
import com.jazzify.backend.shared.domain.MusicKey;

@NullMarked
class IRealProChordParserTest {

	@Test
	void parse_propagatesChordProjectSessionToChordInfos() {
		Session session = Session.builder()
			.title("OMR Processing")
			.build();
		ChordProject project = ChordProject.builder()
			.title("Autumn Leaves")
			.keySignature(MusicKey.C_MAJOR)
			.timeSignature("4/4")
			.user(user())
			.session(session)
			.build();

		List<ChordInfo> chordInfos = IRealProChordParser.parse("Cmaj7 Cmaj7 | Dm7 G7", "4/4", project);

		assertThat(chordInfos).hasSize(3);
		assertThat(chordInfos)
			.allSatisfy(chordInfo -> {
				assertThat(chordInfo.getChordProject()).isSameAs(project);
				assertThat(chordInfo.getSession()).isSameAs(session);
			});
	}

	@Test
	void parseForSheetProject_propagatesSheetProjectSessionToChordInfos() {
		Session session = Session.builder()
			.title("OMR Processing")
			.build();
		SheetProject project = SheetProject.builder()
			.title("Sheet OMR")
			.keySignature(MusicKey.C_MAJOR)
			.user(user())
			.sheetFile(SheetFile.builder()
				.fileType(FileType.IMAGE)
				.build())
			.session(session)
			.build();

		List<ChordInfo> chordInfos = IRealProChordParser.parseForSheetProject("Fmaj7 | Gm7 C7", "4/4", project);

		assertThat(chordInfos).hasSize(3);
		assertThat(chordInfos)
			.allSatisfy(chordInfo -> {
				assertThat(chordInfo.getSheetProject()).isSameAs(project);
				assertThat(chordInfo.getSession()).isSameAs(session);
			});
	}

	private static User user() {
		return User.builder()
			.name("Tester")
			.username("tester")
			.password("password")
			.build();
	}
}
