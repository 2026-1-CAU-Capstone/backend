package com.jazzify.backend.domain.rag.service;

import java.io.OutputStream;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.core.security.CustomPrincipal;
import com.jazzify.backend.domain.chat.service.ChatService;
import com.jazzify.backend.domain.rag.config.RagProperties;
import com.jazzify.backend.domain.rag.dto.request.RagChatRequest;
import com.jazzify.backend.domain.rag.dto.request.RagDocumentCreateRequest;
import com.jazzify.backend.domain.rag.dto.request.RagDocumentUpdateRequest;
import com.jazzify.backend.domain.rag.dto.response.RagDocumentResponse;
import com.jazzify.backend.domain.rag.dto.response.RagDocumentSummaryResponse;
import com.jazzify.backend.domain.rag.dto.response.RagHealthResponse;
import com.jazzify.backend.domain.rag.dto.response.RagSearchResponse;
import com.jazzify.backend.domain.rag.model.RagDocument;
import com.jazzify.backend.domain.rag.model.RagDocumentDraft;
import com.jazzify.backend.domain.rag.model.RagSourceType;
import com.jazzify.backend.domain.rag.service.implementation.RagChatStreamer;
import com.jazzify.backend.domain.rag.service.implementation.RagEmbeddingModel;
import com.jazzify.backend.domain.rag.service.implementation.RagReader;
import com.jazzify.backend.domain.rag.service.implementation.RagWriter;
import com.jazzify.backend.domain.rag.util.RagMapper;
import com.jazzify.backend.shared.llm.AnthropicStreamingClient;
import com.jazzify.backend.shared.exception.code.RagErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@NullMarked
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagService {

	private final RagReader ragReader;
	private final RagWriter ragWriter;
	private final RagChatStreamer ragChatStreamer;
	private final RagEmbeddingModel ragEmbeddingModel;
	private final AnthropicStreamingClient anthropicStreamingClient;
	private final RagProperties ragProperties;
	private final ChatService chatService;

	@Transactional(readOnly = true, transactionManager = "ragTransactionManager")
	public Page<RagDocumentSummaryResponse> getDocuments(Pageable pageable, @Nullable String sourceType, @Nullable String query) {
		RagSourceType filter = parseSourceType(sourceType);
		return ragReader.getDocuments(filter, query, pageable)
			.map(RagMapper::toDocumentSummaryResponse);
	}

	@Transactional(readOnly = true, transactionManager = "ragTransactionManager")
	public RagDocumentResponse getDocumentByPublicId(UUID publicId) {
		return RagMapper.toDocumentResponse(ragReader.getDocumentByPublicId(publicId));
	}

	@Transactional(transactionManager = "ragTransactionManager")
	public RagDocumentResponse createDocument(RagDocumentCreateRequest request) {
		validateSlugAvailable(request.slug(), null);
		RagDocument document = ragWriter.createDocument(toDraft(request));
		return RagMapper.toDocumentResponse(document);
	}

	@Transactional(transactionManager = "ragTransactionManager")
	public RagDocumentResponse updateDocument(UUID publicId, RagDocumentUpdateRequest request) {
		RagDocument existing = ragReader.getDocumentByPublicId(publicId);
		validateSlugAvailable(request.slug(), existing.publicId());
		RagDocument document = ragWriter.updateDocument(existing, toDraft(request));
		return RagMapper.toDocumentResponse(document);
	}

	@Transactional(transactionManager = "ragTransactionManager")
	public void deleteDocument(UUID publicId) {
		ragReader.getDocumentByPublicId(publicId);
		ragWriter.deleteDocument(publicId);
	}

	@Transactional
	public ChatService.PreparedChatStream prepareChat(CustomPrincipal principal, RagChatRequest request) {
		return chatService.prepareRagStream(principal, request);
	}

	public void streamChat(ChatService.PreparedChatStream preparedChatStream, RagChatRequest request, OutputStream outputStream) {
		String assistantMessage = null;
		try {
			assistantMessage = ragChatStreamer.stream(request, preparedChatStream.history(), outputStream);
		} finally {
			chatService.persistTurn(preparedChatStream.chatPublicId(), request.message(), assistantMessage);
		}
	}

	@Transactional(readOnly = true, transactionManager = "ragTransactionManager")
	public RagSearchResponse search(
		String query,
		@Nullable Integer level,
		int limit,
		@Nullable String song,
		@Nullable String tag,
		@Nullable String sourceType
	) {
		RagSourceType filter = parseSourceType(sourceType);
		return new RagSearchResponse(
			query,
			ragReader.search(query, Math.max(1, limit), level, song, tag, filter)
				.stream()
				.map(RagMapper::toChunkResponse)
				.toList()
		);
	}

	@Transactional(readOnly = true, transactionManager = "ragTransactionManager")
	public RagHealthResponse health() {
		return new RagHealthResponse(
			"ok",
			ragProperties.enabled(),
			ragEmbeddingModel.isConfigured(),
			anthropicStreamingClient.isConfigured(),
			ragReader.countDocuments(),
			ragReader.countChunks()
		);
	}

	private RagDocumentDraft toDraft(RagDocumentCreateRequest request) {
		return new RagDocumentDraft(
			normalizeSlug(request.slug()),
			parseRequiredSourceType(request.sourceType()),
			request.title().trim(),
			request.content(),
			request.metadata(),
			request.topicTags().stream().map(String::trim).filter(tag -> !tag.isBlank()).distinct().toList()
		);
	}

	private RagDocumentDraft toDraft(RagDocumentUpdateRequest request) {
		return new RagDocumentDraft(
			normalizeSlug(request.slug()),
			parseRequiredSourceType(request.sourceType()),
			request.title().trim(),
			request.content(),
			request.metadata(),
			request.topicTags().stream().map(String::trim).filter(tag -> !tag.isBlank()).distinct().toList()
		);
	}

	private void validateSlugAvailable(String slug, @Nullable UUID currentPublicId) {
		ragReader.findDocumentBySlug(normalizeSlug(slug)).ifPresent(found -> {
			if (currentPublicId == null || !found.publicId().equals(currentPublicId)) {
				throw RagErrorCode.RAG_DUPLICATE_DOCUMENT_SLUG.toException();
			}
		});
	}

	private RagSourceType parseRequiredSourceType(String rawSourceType) {
		try {
			return RagSourceType.from(rawSourceType);
		} catch (IllegalArgumentException e) {
			throw RagErrorCode.RAG_INVALID_SOURCE_TYPE.toException(rawSourceType);
		}
	}

	private @Nullable RagSourceType parseSourceType(@Nullable String rawSourceType) {
		if (rawSourceType == null || rawSourceType.isBlank()) {
			return null;
		}
		return parseRequiredSourceType(rawSourceType);
	}

	private String normalizeSlug(String slug) {
		return slug.trim().toLowerCase(java.util.Locale.ROOT);
	}

}

