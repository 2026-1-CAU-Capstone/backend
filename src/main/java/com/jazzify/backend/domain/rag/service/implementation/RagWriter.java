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
import com.jazzify.backend.domain.rag.model.RagChunk;
import com.jazzify.backend.domain.rag.model.RagDocument;
import com.jazzify.backend.domain.rag.model.RagDocumentDraft;
import com.jazzify.backend.domain.rag.repository.RagChunkRepository;
import com.jazzify.backend.domain.rag.repository.RagDocumentRepository;
import com.jazzify.backend.shared.exception.code.RagErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@NullMarked
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagWriter {

	private final RagProperties ragProperties;
	private final RagFileChunker ragFileChunker;
	private final RagEmbeddingClient ragEmbeddingClient;
	private final RagDocumentRepository ragDocumentRepository;
	private final RagChunkRepository ragChunkRepository;
	private final JdbcTemplate ragJdbcTemplate;

	public RagWriter(
		RagProperties ragProperties,
		RagFileChunker ragFileChunker,
		RagEmbeddingClient ragEmbeddingClient,
		RagDocumentRepository ragDocumentRepository,
		RagChunkRepository ragChunkRepository,
		@Qualifier("ragJdbcTemplate") JdbcTemplate ragJdbcTemplate
	) {
		this.ragProperties = ragProperties;
		this.ragFileChunker = ragFileChunker;
		this.ragEmbeddingClient = ragEmbeddingClient;
		this.ragDocumentRepository = ragDocumentRepository;
		this.ragChunkRepository = ragChunkRepository;
		this.ragJdbcTemplate = ragJdbcTemplate;
	}

	public void initializeSchemaIfEnabled() {
		RagProperties.Bootstrap bootstrap = ragProperties.bootstrap();
		if (!bootstrap.createSchema()) {
			return;
		}
		try {
			ragJdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
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
			ragJdbcTemplate.execute("""
				CREATE TABLE IF NOT EXISTS rag_chunk (
					id BIGSERIAL PRIMARY KEY,
					public_id UUID NOT NULL UNIQUE,
					document_public_id UUID NOT NULL REFERENCES rag_document(public_id) ON DELETE CASCADE,
					chunk_id VARCHAR(160) NOT NULL UNIQUE,
					section_id VARCHAR(30) NOT NULL,
					level INT NOT NULL,
					title VARCHAR(200) NOT NULL,
					instruction TEXT,
					response TEXT NOT NULL,
					embed_text TEXT NOT NULL,
					source_type VARCHAR(20) NOT NULL,
					song VARCHAR(200) NOT NULL DEFAULT '',
					song_key VARCHAR(120) NOT NULL DEFAULT '',
					source VARCHAR(200) NOT NULL DEFAULT '',
					analyzed_songs TEXT NOT NULL DEFAULT '',
					topic_tags TEXT[] NOT NULL DEFAULT ARRAY[]::TEXT[],
					embedding vector NOT NULL,
					created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
					updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
				)
				""");
			ragJdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunk_document_public_id ON rag_chunk(document_public_id)");
			ragJdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunk_level ON rag_chunk(level)");
			ragJdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunk_source_type ON rag_chunk(source_type)");
			ragJdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_rag_chunk_topic_tags ON rag_chunk USING GIN(topic_tags)");
			ragJdbcTemplate.execute(
				"CREATE INDEX IF NOT EXISTS idx_rag_chunk_embedding ON rag_chunk USING ivfflat (embedding vector_cosine_ops) WITH (lists = 100)"
			);
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
		ragChunkRepository.deleteByDocumentPublicId(publicId);
		ragChunkRepository.saveAll(buildChunks(persistedDocument, parsedDocument.chunks()));
		return persistedDocument;
	}

	private List<RagChunk> buildChunks(RagDocument document, List<RagFileChunker.ParsedChunk> parsedChunks) {
		List<String> texts = parsedChunks.stream().map(RagFileChunker.ParsedChunk::embedText).toList();
		List<List<Double>> embeddings = ragEmbeddingClient.embed(texts);
		List<RagChunk> chunks = new ArrayList<>();
		for (int i = 0; i < parsedChunks.size(); i++) {
			RagFileChunker.ParsedChunk parsedChunk = parsedChunks.get(i);
			chunks.add(new RagChunk(
				UUID.randomUUID(),
				document.publicId(),
				parsedChunk.chunkId(),
				parsedChunk.sectionId(),
				parsedChunk.level(),
				parsedChunk.title(),
				nullIfBlank(parsedChunk.instruction()),
				parsedChunk.response(),
				parsedChunk.embedText(),
				document.sourceType(),
				parsedChunk.song(),
				parsedChunk.key(),
				parsedChunk.source(),
				parsedChunk.analyzedSongs(),
				parsedChunk.topicTags(),
				embeddings.get(i)
			));
		}
		return chunks;
	}

	private @Nullable String nullIfBlank(String value) {
		return value.isBlank() ? null : value;
	}
}



