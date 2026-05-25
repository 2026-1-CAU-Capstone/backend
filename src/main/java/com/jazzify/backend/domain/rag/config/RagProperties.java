package com.jazzify.backend.domain.rag.config;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@NullMarked
@ConfigurationProperties(prefix = "rag")
public record RagProperties(
	boolean enabled,
	@Nullable Datasource datasource,
	@Nullable Bootstrap bootstrap,
	@Nullable Embedding embedding,
	@Nullable Retrieval retrieval,
	@Nullable VectorStore vectorStore
) {

	private static final Datasource DEFAULT_DATASOURCE = new Datasource(null, null, null, "org.postgresql.Driver", 4);
	private static final Bootstrap DEFAULT_BOOTSTRAP = new Bootstrap(false, false, null);
	private static final Embedding DEFAULT_EMBEDDING = new Embedding(null, "/api/v1/embeddings", null);
	private static final Retrieval DEFAULT_RETRIEVAL = new Retrieval(5, 3, 60);
	private static final VectorStore DEFAULT_VECTOR_STORE = new VectorStore("public", "rag_chunk_store", false, false, 768);

	@Override
	public Datasource datasource() {
		return datasource != null ? datasource : DEFAULT_DATASOURCE;
	}

	@Override
	public Bootstrap bootstrap() {
		return bootstrap != null ? bootstrap : DEFAULT_BOOTSTRAP;
	}

	@Override
	public Embedding embedding() {
		return embedding != null ? embedding : DEFAULT_EMBEDDING;
	}

	@Override
	public Retrieval retrieval() {
		return retrieval != null ? retrieval : DEFAULT_RETRIEVAL;
	}

	@Override
	public VectorStore vectorStore() {
		return vectorStore != null ? vectorStore : DEFAULT_VECTOR_STORE;
	}

	@NullMarked
	public record Datasource(
		@Nullable String url,
		@Nullable String username,
		@Nullable String password,
		String driverClassName,
		int maximumPoolSize
	) {
	}

	@NullMarked
	public record Bootstrap(
		boolean enabled,
		boolean createSchema,
		@Nullable String dataRoot
	) {
	}

	@NullMarked
	public record Embedding(
		@Nullable String serverUrl,
		String endpoint,
		@Nullable String apiKey
	) {
	}

	@NullMarked
	public record Retrieval(
		int topK,
		int nPerQuery,
		int rrfK
	) {
	}

	@NullMarked
	public record VectorStore(
		String schemaName,
		String tableName,
		boolean initializeSchema,
		boolean schemaValidation,
		int dimensions
	) {
	}
}




