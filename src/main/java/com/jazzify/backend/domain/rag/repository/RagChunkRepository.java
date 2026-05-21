package com.jazzify.backend.domain.rag.repository;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.jazzify.backend.domain.rag.model.RagChunk;
import com.jazzify.backend.domain.rag.model.RagChunkSearchResult;
import com.jazzify.backend.domain.rag.model.RagSourceType;
import com.jazzify.backend.shared.exception.code.RagErrorCode;

@Repository
@NullMarked
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagChunkRepository {

	private final JdbcTemplate jdbcTemplate;

	public RagChunkRepository(@Qualifier("ragJdbcTemplate") JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

	public void deleteByDocumentPublicId(UUID documentPublicId) {
		jdbcTemplate.update("DELETE FROM rag_chunk WHERE document_public_id = ?", documentPublicId);
	}

	public void saveAll(List<RagChunk> chunks) {
		if (chunks.isEmpty()) {
			return;
		}

		OffsetDateTime now = OffsetDateTime.now();
		jdbcTemplate.execute((java.sql.Connection connection) -> {
			try (PreparedStatement statement = connection.prepareStatement(
				"""
					INSERT INTO rag_chunk (
						public_id, document_public_id, chunk_id, section_id, level, title, instruction, response, embed_text,
						source_type, song, song_key, source, analyzed_songs, topic_tags, embedding, created_at, updated_at
					) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS vector), ?, ?)
					"""
			)) {
				for (RagChunk chunk : chunks) {
					statement.setObject(1, chunk.publicId());
					statement.setObject(2, chunk.documentPublicId());
					statement.setString(3, chunk.chunkId());
					statement.setString(4, chunk.sectionId());
					statement.setInt(5, chunk.level());
					statement.setString(6, chunk.title());
					statement.setString(7, chunk.instruction());
					statement.setString(8, chunk.response());
					statement.setString(9, chunk.embedText());
					statement.setString(10, chunk.sourceType().dbValue());
					statement.setString(11, chunk.song());
					statement.setString(12, chunk.key());
					statement.setString(13, chunk.source());
					statement.setString(14, chunk.analyzedSongs());
					statement.setArray(15, createTextArray(connection, chunk.topicTags()));
					statement.setString(16, toVectorLiteral(chunk.embedding()));
					statement.setTimestamp(17, Timestamp.from(now.toInstant()));
					statement.setTimestamp(18, Timestamp.from(now.toInstant()));
					statement.addBatch();
				}
				statement.executeBatch();
			}
			return null;
		});
	}

	public List<RagChunkSearchResult> searchByEmbedding(
		List<Double> embedding,
		int limit,
		@Nullable Integer levelFilter,
		@Nullable String songFilter,
		@Nullable String tagFilter,
		@Nullable RagSourceType sourceType
	) {
		String vectorLiteral = toVectorLiteral(embedding);
		StringBuilder sql = new StringBuilder("""
			SELECT
				chunk_id,
				source_type,
				title,
				song,
				song_key,
				level,
				section_id,
				instruction,
				response,
				topic_tags,
				source,
				analyzed_songs,
				1 - (embedding <=> CAST(? AS vector)) AS score
			FROM rag_chunk
			WHERE 1 = 1
			""");
		List<Object> params = new ArrayList<>();
		params.add(vectorLiteral);

		if (levelFilter != null) {
			sql.append(" AND level = ?");
			params.add(levelFilter);
		}
		if (songFilter != null && !songFilter.isBlank()) {
			sql.append(" AND song ILIKE ?");
			params.add("%" + songFilter.trim() + "%");
		}
		if (tagFilter != null && !tagFilter.isBlank()) {
			sql.append(" AND topic_tags @> ARRAY[?]::text[]");
			params.add(tagFilter.trim());
		}
		if (sourceType != null) {
			sql.append(" AND source_type = ?");
			params.add(sourceType.dbValue());
		}

		sql.append(" ORDER BY embedding <=> CAST(? AS vector) LIMIT ?");
		params.add(vectorLiteral);
		params.add(limit);

		try {
			return jdbcTemplate.query(sql.toString(),
				(rs, rowNum) -> new RagChunkSearchResult(
					rs.getString("chunk_id"),
					rs.getDouble("score"),
					RagSourceType.from(rs.getString("source_type")),
					rs.getString("title"),
					rs.getString("song"),
					rs.getString("song_key"),
					rs.getInt("level"),
					rs.getString("section_id"),
					rs.getString("instruction"),
					rs.getString("response"),
					readTextArray(rs.getArray("topic_tags")),
					rs.getString("source"),
					rs.getString("analyzed_songs"),
					null,
					List.of()
				),
				params.toArray()
			);
		} catch (Exception e) {
			throw RagErrorCode.RAG_SEARCH_FAILED.toException(e.getMessage());
		}
	}

	public long count() {
		Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM rag_chunk", Long.class);
		return count != null ? count : 0L;
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

	private String toVectorLiteral(List<Double> embedding) {
		if (embedding.isEmpty()) {
			throw RagErrorCode.RAG_SEARCH_FAILED.toException("빈 임베딩은 검색/저장할 수 없습니다.");
		}
		StringBuilder builder = new StringBuilder("[");
		for (int i = 0; i < embedding.size(); i++) {
			if (i > 0) {
				builder.append(',');
			}
			builder.append(embedding.get(i));
		}
		builder.append(']');
		return builder.toString();
	}
}



