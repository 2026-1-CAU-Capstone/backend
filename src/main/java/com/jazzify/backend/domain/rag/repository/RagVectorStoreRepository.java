package com.jazzify.backend.domain.rag.repository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.jazzify.backend.domain.rag.config.RagProperties;
import com.jazzify.backend.domain.rag.model.RagChunkSearchResult;
import com.jazzify.backend.domain.rag.model.RagSourceType;
import com.jazzify.backend.domain.rag.service.implementation.RagFileChunker;
import com.jazzify.backend.shared.exception.code.RagErrorCode;

@Repository
@NullMarked
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagVectorStoreRepository {

	private static final FilterExpressionTextParser FILTER_EXPRESSION_TEXT_PARSER = new FilterExpressionTextParser();
	private static final int SEARCH_OVERSAMPLING_FACTOR = 10;
	private static final int MIN_FETCH_SIZE = 20;

	private final VectorStore vectorStore;
	private final JdbcTemplate ragJdbcTemplate;
	private final RagProperties ragProperties;

	public RagVectorStoreRepository(
		@Qualifier("ragVectorStore") VectorStore vectorStore,
		@Qualifier("ragJdbcTemplate") JdbcTemplate ragJdbcTemplate,
		RagProperties ragProperties
	) {
		this.vectorStore = vectorStore;
		this.ragJdbcTemplate = ragJdbcTemplate;
		this.ragProperties = ragProperties;
	}

	public void saveAll(UUID documentPublicId, RagSourceType sourceType, List<RagFileChunker.ParsedChunk> parsedChunks) {
		if (parsedChunks.isEmpty()) {
			return;
		}
		vectorStore.add(parsedChunks.stream()
			.map(chunk -> toDocument(documentPublicId, sourceType, chunk))
			.toList());
	}

	public void deleteByDocumentPublicId(UUID documentPublicId) {
		try {
			vectorStore.delete(FILTER_EXPRESSION_TEXT_PARSER.parse("documentPublicId == '" + documentPublicId + "'"));
		} catch (Exception e) {
			throw RagErrorCode.RAG_SEARCH_FAILED.toException("벡터 문서 삭제 실패: " + e.getMessage());
		}
	}

	public List<RagChunkSearchResult> search(
		String query,
		int limit,
		@Nullable Integer levelFilter,
		@Nullable String songFilter,
		@Nullable String tagFilter,
		@Nullable RagSourceType sourceType
	) {
		try {
			int topK = Math.max(Math.max(1, limit) * SEARCH_OVERSAMPLING_FACTOR, MIN_FETCH_SIZE);
			List<Document> documents = vectorStore.similaritySearch(
				SearchRequest.builder()
					.query(query)
					.topK(topK)
					.similarityThresholdAll()
					.build()
			);

			List<RagChunkSearchResult> filtered = new ArrayList<>();
			for (Document document : documents) {
				if (!matches(document, levelFilter, songFilter, tagFilter, sourceType)) {
					continue;
				}
				filtered.add(toSearchResult(document));
				if (filtered.size() >= limit) {
					break;
				}
			}
			return List.copyOf(filtered);
		} catch (Exception e) {
			throw RagErrorCode.RAG_SEARCH_FAILED.toException(e.getMessage());
		}
	}

	public long count() {
		try {
			Long count = ragJdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + fullyQualifiedTableName(), Long.class);
			return count != null ? count : 0L;
		} catch (Exception e) {
			return 0L;
		}
	}

	public long countByDocumentPublicId(UUID documentPublicId) {
		try {
			Long count = ragJdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM " + fullyQualifiedTableName() + " WHERE CAST(metadata AS jsonb) ->> 'documentPublicId' = ?",
				Long.class,
				documentPublicId.toString()
			);
			return count != null ? count : 0L;
		} catch (Exception e) {
			return 0L;
		}
	}

	private Document toDocument(UUID documentPublicId, RagSourceType sourceType, RagFileChunker.ParsedChunk chunk) {
		Map<String, Object> metadata = new LinkedHashMap<>();
		metadata.put("documentPublicId", documentPublicId.toString());
		metadata.put("chunkId", chunk.chunkId());
		metadata.put("sourceType", sourceType.dbValue());
		metadata.put("song", chunk.song());
		metadata.put("key", chunk.key());
		metadata.put("level", chunk.level());
		metadata.put("sectionId", chunk.sectionId());
		metadata.put("title", chunk.title());
		metadata.put("instruction", chunk.instruction());
		metadata.put("response", chunk.response());
		metadata.put("topicTags", chunk.topicTags());
		metadata.put("source", chunk.source());
		metadata.put("analyzedSongs", chunk.analyzedSongs());
		return Document.builder()
			.id(chunk.chunkId())
			.text(chunk.embedText())
			.metadata(metadata)
			.build();
	}

	private RagChunkSearchResult toSearchResult(Document document) {
		Map<String, Object> metadata = document.getMetadata();
		return new RagChunkSearchResult(
			nonBlank(document.getId(), getString(metadata, "chunkId")),
			document.getScore() != null ? document.getScore() : 0.0d,
			RagSourceType.from(getString(metadata, "sourceType")),
			getString(metadata, "title"),
			getString(metadata, "song"),
			getString(metadata, "key"),
			getInt(metadata, "level"),
			getString(metadata, "sectionId"),
			nullIfBlank(getString(metadata, "instruction")),
			getString(metadata, "response"),
			getStringList(metadata, "topicTags"),
			getString(metadata, "source"),
			getString(metadata, "analyzedSongs"),
			null,
			List.of()
		);
	}

	private boolean matches(
		Document document,
		@Nullable Integer levelFilter,
		@Nullable String songFilter,
		@Nullable String tagFilter,
		@Nullable RagSourceType sourceType
	) {
		Map<String, Object> metadata = document.getMetadata();
		if (levelFilter != null && getInt(metadata, "level") != levelFilter) {
			return false;
		}
		if (sourceType != null && !sourceType.dbValue().equalsIgnoreCase(getString(metadata, "sourceType"))) {
			return false;
		}
		if (songFilter != null && !songFilter.isBlank()) {
			String song = getString(metadata, "song").toLowerCase(Locale.ROOT);
			if (!song.contains(songFilter.trim().toLowerCase(Locale.ROOT))) {
				return false;
			}
		}
		if (tagFilter != null && !tagFilter.isBlank()) {
			String normalizedTag = tagFilter.trim().toLowerCase(Locale.ROOT);
			boolean matched = getStringList(metadata, "topicTags").stream()
				.map(tag -> tag.toLowerCase(Locale.ROOT))
				.anyMatch(normalizedTag::equals);
			if (!matched) {
				return false;
			}
		}
		return true;
	}

	private String fullyQualifiedTableName() {
		RagProperties.VectorStore vectorStore = ragProperties.vectorStore();
		return vectorStore.schemaName() + "." + vectorStore.tableName();
	}

	private static String getString(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		return value != null ? String.valueOf(value) : "";
	}

	private static int getInt(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		if (value instanceof Number number) {
			return number.intValue();
		}
		if (value != null) {
			try {
				return Integer.parseInt(String.valueOf(value));
			} catch (NumberFormatException ignored) {
				return 0;
			}
		}
		return 0;
	}

	private static List<String> getStringList(Map<String, Object> metadata, String key) {
		Object value = metadata.get(key);
		if (value instanceof List<?> list) {
			return list.stream().map(String::valueOf).toList();
		}
		if (value instanceof String string && !string.isBlank()) {
			return List.of(string);
		}
		return List.of();
	}

	private static @Nullable String nullIfBlank(String value) {
		return value.isBlank() ? null : value;
	}

	private static String nonBlank(String primary, String fallback) {
		return primary != null && !primary.isBlank() ? primary : fallback;
	}
}

