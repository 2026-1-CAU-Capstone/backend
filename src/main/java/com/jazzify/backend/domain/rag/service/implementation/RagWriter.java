package com.jazzify.backend.domain.rag.service.implementation;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.jazzify.backend.domain.rag.config.RagProperties;
import com.jazzify.backend.domain.rag.model.RagDocument;
import com.jazzify.backend.domain.rag.model.RagDocumentDraft;
import com.jazzify.backend.domain.rag.repository.RagDocumentRepository;
import com.jazzify.backend.domain.rag.repository.RagVectorStoreRepository;
import com.jazzify.backend.shared.exception.code.RagErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@NullMarked
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagWriter {

	private final RagProperties ragProperties;
	private final RagFileChunker ragFileChunker;
	private final RagDocumentRepository ragDocumentRepository;
	private final RagVectorStoreRepository ragVectorStoreRepository;
	private final JdbcTemplate ragJdbcTemplate;

	public RagWriter(
		RagProperties ragProperties,
		RagFileChunker ragFileChunker,
		RagDocumentRepository ragDocumentRepository,
		RagVectorStoreRepository ragVectorStoreRepository,
		@Qualifier("ragJdbcTemplate") JdbcTemplate ragJdbcTemplate
	) {
		this.ragProperties = ragProperties;
		this.ragFileChunker = ragFileChunker;
		this.ragDocumentRepository = ragDocumentRepository;
		this.ragVectorStoreRepository = ragVectorStoreRepository;
		this.ragJdbcTemplate = ragJdbcTemplate;
	}

	public void initializeSchemaIfEnabled() {
		RagProperties.Bootstrap bootstrap = ragProperties.bootstrap();
		if (!bootstrap.createSchema()) {
			return;
		}
		try {
			ragJdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS rag_document (
					id BIGSERIAL PRIMARY KEY,
					public_id UUID NOT NULL UNIQUE,
					slug VARCHAR(120) NOT NULL UNIQUE,
					source_type VARCHAR(20) NOT NULL,
					title VARCHAR(200) NOT NULL,
					content TEXT NOT NULL,
					metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
					topic_tags TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
					embedding_version INT NOT NULL DEFAULT 1,
					created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
					updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
				)
				""");
		} catch (Exception e) {
			throw RagErrorCode.RAG_SCHEMA_INITIALIZATION_FAILED.toException(e.getMessage());
		}
	}

	@Transactional(transactionManager = "ragTransactionManager")
	public void bootstrapFromFilesystemIfEnabled() {
		RagProperties.Bootstrap bootstrap = ragProperties.bootstrap();
		if (!bootstrap.enabled()) {
			return;
		}
		String dataRoot = bootstrap.dataRoot();
		if (dataRoot == null || dataRoot.isBlank()) {
			throw RagErrorCode.RAG_BOOTSTRAP_PATH_INVALID.toException("RAG_BOOTSTRAP_DATA_ROOT");
		}

		List<RagFileChunker.ParsedDocument> parsedDocuments = ragFileChunker.parseDocuments(Path.of(dataRoot));
		for (RagFileChunker.ParsedDocument parsedDocument : parsedDocuments) {
			syncDocument(parsedDocument);
		}
		log.info("RAG bootstrap complete. documents={}", parsedDocuments.size());
	}

	@Transactional(transactionManager = "ragTransactionManager")
	public RagDocument createDocument(RagDocumentDraft draft) {
		return writeDocument(UUID.randomUUID(), 1, draft);
	}

	@Transactional(transactionManager = "ragTransactionManager")
	public RagDocument updateDocument(RagDocument existing, RagDocumentDraft draft) {
		return writeDocument(existing.publicId(), existing.embeddingVersion() + 1, draft);
	}

	@Transactional(transactionManager = "ragTransactionManager")
	public void deleteDocument(UUID publicId) {
		ragVectorStoreRepository.deleteByDocumentPublicId(publicId);
		ragDocumentRepository.deleteByPublicId(publicId);
	}

	private void syncDocument(RagFileChunker.ParsedDocument parsedDocument) {
		RagDocument incoming = parsedDocument.document();
		Optional<RagDocument> existing = ragDocumentRepository.findBySlug(incoming.slug());
		if (existing.isPresent() && existing.get().content().equals(incoming.content())) {
			return;
		}

		UUID documentPublicId = existing.map(RagDocument::publicId).orElseGet(UUID::randomUUID);
		int embeddingVersion = existing.map(document -> document.embeddingVersion() + 1).orElse(1);
		writeDocument(documentPublicId, embeddingVersion, new RagDocumentDraft(
			incoming.slug(),
			incoming.sourceType(),
			incoming.title(),
			incoming.content(),
			incoming.metadata(),
			incoming.topicTags()
		));
	}

	private RagDocument writeDocument(UUID publicId, int embeddingVersion, RagDocumentDraft draft) {
		RagFileChunker.ParsedDocument parsedDocument = ragFileChunker.parseDocument(publicId, embeddingVersion, draft);
		if (parsedDocument.chunks().isEmpty()) {
			throw RagErrorCode.RAG_INVALID_DOCUMENT_CONTENT.toException();
		}

		RagDocument persistedDocument = new RagDocument(
			publicId,
			parsedDocument.document().slug(),
			parsedDocument.document().sourceType(),
			parsedDocument.document().title(),
			parsedDocument.document().content(),
			parsedDocument.document().metadata(),
			parsedDocument.document().topicTags(),
			embeddingVersion,
			parsedDocument.chunks().size()
		);
		ragDocumentRepository.save(persistedDocument);
		ragVectorStoreRepository.deleteByDocumentPublicId(publicId);
		ragVectorStoreRepository.saveAll(persistedDocument.publicId(), persistedDocument.sourceType(), parsedDocument.chunks());
		return persistedDocument;
	}
}



