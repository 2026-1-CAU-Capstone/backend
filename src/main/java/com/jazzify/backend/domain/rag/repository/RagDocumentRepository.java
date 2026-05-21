package com.jazzify.backend.domain.rag.repository;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jazzify.backend.domain.rag.model.RagDocument;
import com.jazzify.backend.domain.rag.model.RagSourceType;
import com.jazzify.backend.shared.exception.code.RagErrorCode;

@Repository
@NullMarked
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagDocumentRepository {

	private static final TypeReference<Map<String, String>> METADATA_TYPE = new TypeReference<>() {
	};
	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private final JdbcTemplate jdbcTemplate;

	public RagDocumentRepository(
		@Qualifier("ragJdbcTemplate") JdbcTemplate jdbcTemplate
	) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public Optional<RagDocument> findBySlug(String slug) {
		List<RagDocument> documents = jdbcTemplate.query(
			"""
				SELECT public_id, slug, source_type, title, content, metadata, topic_tags, embedding_version,
					(SELECT COUNT(*) FROM rag_chunk rc WHERE rc.document_public_id = rd.public_id) AS chunk_count
				FROM rag_document
				AS rd
				WHERE slug = ?
				""",
			(rs, rowNum) -> mapDocument(rs),
			slug
		);
		return documents.stream().findFirst();
	}

	public Optional<RagDocument> findByPublicId(UUID publicId) {
		List<RagDocument> documents = jdbcTemplate.query(
			"""
				SELECT public_id, slug, source_type, title, content, metadata, topic_tags, embedding_version,
					(SELECT COUNT(*) FROM rag_chunk rc WHERE rc.document_public_id = rd.public_id) AS chunk_count
				FROM rag_document AS rd
				WHERE public_id = ?
				""",
			(rs, rowNum) -> mapDocument(rs),
			publicId
		);
		return documents.stream().findFirst();
	}

	public Page<RagDocument> findAll(@Nullable RagSourceType sourceType, @Nullable String query, Pageable pageable) {
		StringBuilder whereClause = new StringBuilder(" WHERE 1 = 1");
		List<Object> params = new java.util.ArrayList<>();

		if (sourceType != null) {
			whereClause.append(" AND source_type = ?");
			params.add(sourceType.dbValue());
		}
		if (query != null && !query.isBlank()) {
			whereClause.append(" AND (slug ILIKE ? OR title ILIKE ?)");
			String likeQuery = "%" + query.trim() + "%";
			params.add(likeQuery);
			params.add(likeQuery);
		}

		Long total = jdbcTemplate.queryForObject(
			"SELECT COUNT(*) FROM rag_document" + whereClause,
			Long.class,
			params.toArray()
		);

		String sql = """
			SELECT public_id, slug, source_type, title, content, metadata, topic_tags, embedding_version,
				(SELECT COUNT(*) FROM rag_chunk rc WHERE rc.document_public_id = rd.public_id) AS chunk_count
			FROM rag_document AS rd
			""" + whereClause + " ORDER BY updated_at DESC LIMIT ? OFFSET ?";
		List<Object> pageParams = new java.util.ArrayList<>(params);
		pageParams.add(pageable.getPageSize());
		pageParams.add(pageable.getOffset());

		List<RagDocument> content = jdbcTemplate.query(
			sql,
			(rs, rowNum) -> mapDocument(rs),
			pageParams.toArray()
		);
		return new PageImpl<>(content, pageable, total != null ? total : 0L);
	}

	public void save(RagDocument document) {
		OffsetDateTime now = OffsetDateTime.now();
		jdbcTemplate.execute((java.sql.Connection connection) -> {
			try (PreparedStatement statement = connection.prepareStatement(
				"""
					INSERT INTO rag_document (
						public_id, slug, source_type, title, content, metadata, topic_tags, embedding_version, created_at, updated_at
					) VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, ?)
					ON CONFLICT (public_id) DO UPDATE SET
						slug = EXCLUDED.slug,
						source_type = EXCLUDED.source_type,
						title = EXCLUDED.title,
						content = EXCLUDED.content,
						metadata = EXCLUDED.metadata,
						topic_tags = EXCLUDED.topic_tags,
						embedding_version = EXCLUDED.embedding_version,
						updated_at = EXCLUDED.updated_at
					"""
			)) {
				statement.setObject(1, document.publicId());
				statement.setString(2, document.slug());
				statement.setString(3, document.sourceType().dbValue());
				statement.setString(4, document.title());
				statement.setString(5, document.content());
				statement.setString(6, writeMetadata(document.metadata()));
				statement.setArray(7, createTextArray(connection, document.topicTags()));
				statement.setInt(8, document.embeddingVersion());
				statement.setTimestamp(9, Timestamp.from(now.toInstant()));
				statement.setTimestamp(10, Timestamp.from(now.toInstant()));
				statement.executeUpdate();
			}
			return null;
		});
	}

	public void deleteByPublicId(UUID publicId) {
		jdbcTemplate.update("DELETE FROM rag_document WHERE public_id = ?", publicId);
	}

	public long count() {
		Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rag_document", Long.class);
		return count != null ? count : 0L;
	}

	private RagDocument mapDocument(java.sql.ResultSet rs) throws java.sql.SQLException {
		return new RagDocument(
			UUID.fromString(rs.getString("public_id")),
			rs.getString("slug"),
			RagSourceType.from(rs.getString("source_type")),
			rs.getString("title"),
			rs.getString("content"),
			readMetadata(rs.getString("metadata")),
			readTextArray(rs.getArray("topic_tags")),
			rs.getInt("embedding_version"),
			rs.getLong("chunk_count")
		);
	}

	private Map<String, String> readMetadata(@Nullable String rawMetadata) {
		if (rawMetadata == null || rawMetadata.isBlank()) {
			return Map.of();
		}
		try {
			return OBJECT_MAPPER.readValue(rawMetadata, METADATA_TYPE);
		} catch (Exception e) {
			throw RagErrorCode.RAG_SEARCH_FAILED.toException("metadata 파싱 실패: " + e.getMessage());
		}
	}

	private String writeMetadata(Map<String, String> metadata) {
		try {
			return OBJECT_MAPPER.writeValueAsString(metadata);
		} catch (Exception e) {
			throw RagErrorCode.RAG_BOOTSTRAP_FAILED.toException("metadata 직렬화 실패: " + e.getMessage());
		}
	}

	private List<String> readTextArray(@Nullable Array array) {
		if (array == null) {
			return List.of();
		}
		try {
			String[] values = (String[]) array.getArray();
			return values != null ? Arrays.asList(values) : List.of();
		} catch (Exception e) {
			throw RagErrorCode.RAG_SEARCH_FAILED.toException("topic_tags 파싱 실패: " + e.getMessage());
		}
	}

	private Array createTextArray(java.sql.Connection connection, List<String> values) throws java.sql.SQLException {
		return connection.createArrayOf("text", values.toArray(String[]::new));
	}
}






