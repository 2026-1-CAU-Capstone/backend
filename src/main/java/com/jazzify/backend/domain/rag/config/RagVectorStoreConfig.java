package com.jazzify.backend.domain.rag.config;

import org.jspecify.annotations.NullMarked;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.jazzify.backend.domain.rag.service.implementation.RagEmbeddingModel;

@Configuration
@NullMarked
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagVectorStoreConfig {

	@Bean(name = "ragVectorStore")
	public VectorStore ragVectorStore(
		@Qualifier("ragJdbcTemplate") JdbcTemplate ragJdbcTemplate,
		RagEmbeddingModel ragEmbeddingModel,
		RagProperties ragProperties
	) {
		RagProperties.VectorStore vectorStore = ragProperties.vectorStore();
		return PgVectorStore.builder(ragJdbcTemplate, ragEmbeddingModel)
			.schemaName(vectorStore.schemaName())
			.vectorTableName(vectorStore.tableName())
			.idType(PgVectorStore.PgIdType.TEXT)
			.dimensions(vectorStore.dimensions())
			.distanceType(PgVectorStore.PgDistanceType.COSINE_DISTANCE)
			.indexType(PgVectorStore.PgIndexType.HNSW)
			.initializeSchema(vectorStore.initializeSchema())
			.vectorTableValidationsEnabled(vectorStore.schemaValidation())
			.build();
	}
}

