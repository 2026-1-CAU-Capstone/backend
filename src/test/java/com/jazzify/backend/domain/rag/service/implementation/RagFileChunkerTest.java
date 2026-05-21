package com.jazzify.backend.domain.rag.service.implementation;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.jazzify.backend.domain.rag.model.RagDocumentDraft;
import com.jazzify.backend.domain.rag.model.RagSourceType;

@NullMarked
class RagFileChunkerTest {

	private final RagFileChunker ragFileChunker = new RagFileChunker();

	@TempDir
	Path tempDir;

	@Test
	void parseDocuments_extractsStandardMetadataAndChunk() throws IOException {
		Path standardsDir = Files.createDirectories(tempDir.resolve("standards"));
		Files.createDirectories(tempDir.resolve("lessons"));
		Files.writeString(standardsDir.resolve("allofme.txt"), """
			- **곡명:** All of Me
			- **작곡:** Gerald Marks / Seymour Simons
			- **센터 키:** C Major
			- **형식:** 32마디 ABAC
			- **강의 출처:** 준킴뮤직
			
			### 1-1. 곡의 키 센터 확인법
			**instruction:** 이 곡의 키 센터는 어디야?
			**response:** C 메이저를 중심으로 보되 세컨더리 도미넌트를 함께 봐야 합니다.
			""");

		var documents = ragFileChunker.parseDocuments(tempDir);

		assertThat(documents).hasSize(1);
		assertThat(documents.getFirst().document().slug()).isEqualTo("allofme");
		assertThat(documents.getFirst().document().sourceType()).isEqualTo(RagSourceType.STANDARD);
		assertThat(documents.getFirst().document().topicTags()).contains("secondary-dominant");
		assertThat(documents.getFirst().chunks()).hasSize(1);
		assertThat(documents.getFirst().chunks().getFirst().embedText())
			.contains("[All of Me · C Major]")
			.contains("질문: 이 곡의 키 센터는 어디야?")
			.contains("답변: C 메이저를 중심으로 보되 세컨더리 도미넌트를 함께 봐야 합니다.");
	}

	@Test
	void parseDocument_mergesMetadataAndUsesProvidedTopicTags() {
		var parsedDocument = ragFileChunker.parseDocument(
			UUID.randomUUID(),
			2,
			new RagDocumentDraft(
				"custom-standard",
				RagSourceType.STANDARD,
				"Custom Standard",
				"""
				- **곡명:** Original Song
				- **센터 키:** F Major
				
				### 1-1. 텐션 선택
				**instruction:** 어떤 스케일을 쓰면 돼?
				**response:** 메이저 중심으로 접근합니다.
				""",
				Map.of("song", "Overridden Song", "source", "Admin UI"),
				List.of("custom-tag")
			)
		);

		assertThat(parsedDocument.document().embeddingVersion()).isEqualTo(2);
		assertThat(parsedDocument.document().metadata())
			.containsEntry("song", "Overridden Song")
			.containsEntry("key", "F Major")
			.containsEntry("source", "Admin UI");
		assertThat(parsedDocument.document().topicTags()).containsExactly("custom-tag");
		assertThat(parsedDocument.chunks()).hasSize(1);
		assertThat(parsedDocument.chunks().getFirst().embedText())
			.contains("[Overridden Song · F Major]")
			.contains("질문: 어떤 스케일을 쓰면 돼?")
			.contains("답변: 메이저 중심으로 접근합니다.");
	}
}


