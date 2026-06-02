package com.jazzify.backend.domain.rag.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jazzify.backend.domain.rag.config.RagProperties;
import com.jazzify.backend.domain.rag.model.RagChunkSearchResult;
import com.jazzify.backend.domain.rag.model.RagSourceType;

@NullMarked
@ExtendWith(MockitoExtension.class)
class RagVectorStoreRepositoryTest {

	@Mock
	private VectorStore vectorStore;

	@Mock
	private JdbcTemplate ragJdbcTemplate;

	private RagVectorStoreRepository ragVectorStoreRepository;

	@BeforeEach
	void setUp() {
		ragVectorStoreRepository = new RagVectorStoreRepository(
			vectorStore,
			ragJdbcTemplate,
			new RagProperties(
				true,
				null,
				null,
				new RagProperties.Retrieval(5, 3, 60),
				new RagProperties.VectorStore("public", "rag_chunk_store", false, false, 768)
			)
		);
	}

	@Test
	void search_filtersAndMapsSpringAiDocuments() {
		Document matched = Document.builder()
			.id("standard__allofme__1-1")
			.text("ignored")
			.score(0.91)
			.metadata(Map.ofEntries(
				Map.entry("chunkId", "standard__allofme__1-1"),
				Map.entry("sourceType", "standard"),
				Map.entry("title", "세컨더리 도미넌트 가이드"),
				Map.entry("song", "All of Me"),
				Map.entry("key", "C"),
				Map.entry("level", 1),
				Map.entry("sectionId", "1-1"),
				Map.entry("instruction", "질문"),
				Map.entry("response", "응답"),
				Map.entry("topicTags", List.of("secondary-dominant", "dim7")),
				Map.entry("source", "준킴뮤직"),
				Map.entry("analyzedSongs", "All of Me")
			))
			.build();
		Document filteredOut = Document.builder()
			.id("lesson__guide__2-1")
			.text("ignored")
			.score(0.95)
			.metadata(Map.ofEntries(
				Map.entry("chunkId", "lesson__guide__2-1"),
				Map.entry("sourceType", "lesson"),
				Map.entry("title", "다른 청크"),
				Map.entry("song", "Blue Bossa"),
				Map.entry("key", "Cm"),
				Map.entry("level", 2),
				Map.entry("sectionId", "2-1"),
				Map.entry("instruction", ""),
				Map.entry("response", "다른 응답"),
				Map.entry("topicTags", List.of("modal-interchange")),
				Map.entry("source", "lesson"),
				Map.entry("analyzedSongs", "Blue Bossa")
			))
			.build();
		when(vectorStore.similaritySearch(any(org.springframework.ai.vectorstore.SearchRequest.class)))
			.thenReturn(List.of(matched, filteredOut));

		List<RagChunkSearchResult> results = ragVectorStoreRepository.search(
			"세컨더리 도미넌트",
			1,
			1,
			"All of Me",
			"secondary-dominant",
			RagSourceType.STANDARD
		);

		assertThat(results).hasSize(1);
		assertThat(results.getFirst())
			.extracting(
				RagChunkSearchResult::id,
				RagChunkSearchResult::score,
				RagChunkSearchResult::sourceType,
				RagChunkSearchResult::song,
				RagChunkSearchResult::level
			)
			.containsExactly("standard__allofme__1-1", 0.91d, RagSourceType.STANDARD, "All of Me", 1);
	}

	@Test
	void deleteByDocumentPublicId_delegatesToVectorStoreDelete() {
		ragVectorStoreRepository.deleteByDocumentPublicId(UUID.randomUUID());
		verify(vectorStore).delete(any(org.springframework.ai.vectorstore.filter.Filter.Expression.class));
	}
}



