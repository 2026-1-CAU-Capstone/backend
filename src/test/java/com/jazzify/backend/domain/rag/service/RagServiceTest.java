package com.jazzify.backend.domain.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.jazzify.backend.domain.chat.service.ChatService;
import com.jazzify.backend.domain.rag.config.RagProperties;
import com.jazzify.backend.domain.rag.dto.request.RagDocumentCreateRequest;
import com.jazzify.backend.domain.rag.model.RagDocument;
import com.jazzify.backend.domain.rag.model.RagDocumentDraft;
import com.jazzify.backend.domain.rag.model.RagSourceType;
import com.jazzify.backend.domain.rag.service.implementation.RagChatStreamer;
import com.jazzify.backend.domain.rag.service.implementation.RagEmbeddingModel;
import com.jazzify.backend.domain.rag.service.implementation.RagReader;
import com.jazzify.backend.domain.rag.service.implementation.RagWriter;
import com.jazzify.backend.shared.exception.CustomException;
import com.jazzify.backend.shared.llm.AnthropicStreamingClient;

@NullMarked
@ExtendWith(MockitoExtension.class)
class RagServiceTest {

	@Mock
	private RagReader ragReader;

	@Mock
	private RagWriter ragWriter;

	@Mock
	private RagEmbeddingModel ragEmbeddingModel;

	@Mock
	private RagChatStreamer ragChatStreamer;

	@Mock
	private AnthropicStreamingClient anthropicStreamingClient;

	@Mock
	private ChatService chatService;

	private RagService ragService;

	@BeforeEach
	void setUp() {
		ragService = new RagService(
			ragReader,
			ragWriter,
			ragChatStreamer,
			ragEmbeddingModel,
			anthropicStreamingClient,
			new RagProperties(
				true,
				new RagProperties.Datasource("jdbc:postgresql://localhost:5432/rag", "user", "pw", "org.postgresql.Driver", 4),
				new RagProperties.Bootstrap(false, false, null),
				new RagProperties.Retrieval(5, 3, 60),
				new RagProperties.VectorStore("public", "rag_chunk_store", false, false, 768)
			),
			chatService
		);
	}

	@Test
	void createDocument_normalizesSlugAndDelegatesToWriter() {
		RagDocumentCreateRequest request = new RagDocumentCreateRequest(
			" AllOfMe ",
			"standard",
			"All of Me",
			"""
			### 1-1. 곡의 키 센터 확인법
			**instruction:** 키 센터는 어디야?
			**response:** C 메이저를 기준으로 접근합니다.
			""",
			Map.of("song", "All of Me"),
			List.of("secondary-dominant", "secondary-dominant", "  modal-interchange  ")
		);
		RagDocument saved = new RagDocument(
			UUID.randomUUID(),
			"allofme",
			RagSourceType.STANDARD,
			"All of Me",
			request.content(),
			Map.of("song", "All of Me"),
			List.of("secondary-dominant", "modal-interchange"),
			1,
			1
		);
		when(ragReader.findDocumentBySlug("allofme")).thenReturn(Optional.empty());
		when(ragWriter.createDocument(any(RagDocumentDraft.class))).thenReturn(saved);

		var response = ragService.createDocument(request);
		ArgumentCaptor<RagDocumentDraft> captor = ArgumentCaptor.forClass(RagDocumentDraft.class);
		verify(ragWriter).createDocument(captor.capture());

		assertThat(captor.getValue().slug()).isEqualTo("allofme");
		assertThat(captor.getValue().sourceType()).isEqualTo(RagSourceType.STANDARD);
		assertThat(captor.getValue().topicTags()).containsExactly("secondary-dominant", "modal-interchange");
		assertThat(response.slug()).isEqualTo("allofme");
	}

	@Test
	void createDocument_throwsWhenSlugAlreadyExists() {
		RagDocumentCreateRequest request = new RagDocumentCreateRequest(
			"allofme",
			"standard",
			"All of Me",
			"content",
			Map.of(),
			List.of()
		);
		when(ragReader.findDocumentBySlug("allofme")).thenReturn(Optional.of(
			new RagDocument(UUID.randomUUID(), "allofme", RagSourceType.STANDARD, "All of Me", "content", Map.of(), List.of(), 1, 0)
		));

		assertThatThrownBy(() -> ragService.createDocument(request))
			.isInstanceOf(CustomException.class)
			.hasMessageContaining("이미 사용 중인 RAG 문서 slug");
	}

	@Test
	void health_returnsDocumentAndChunkCounts() {
		when(ragReader.countDocuments()).thenReturn(3L);
		when(ragReader.countChunks()).thenReturn(14L);
		when(ragEmbeddingModel.isConfigured()).thenReturn(true);
		when(anthropicStreamingClient.isConfigured()).thenReturn(true);

		var response = ragService.health();

		assertThat(response.enabled()).isTrue();
		assertThat(response.embeddingConfigured()).isTrue();
		assertThat(response.llmConfigured()).isTrue();
		assertThat(response.documentCount()).isEqualTo(3L);
		assertThat(response.chunkCount()).isEqualTo(14L);
	}
}




