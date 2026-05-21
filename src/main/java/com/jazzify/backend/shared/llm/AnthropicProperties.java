package com.jazzify.backend.shared.llm;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;

@NullMarked
@ConfigurationProperties(prefix = "anthropic")
public record AnthropicProperties(
	@Nullable String apiKey,
	@Nullable String baseUrl,
	@Nullable String model,
	@Nullable Integer maxTokens
) {

	private static final String DEFAULT_BASE_URL = "https://api.anthropic.com";
	private static final String DEFAULT_MODEL = "claude-sonnet-4-6";
	private static final int DEFAULT_MAX_TOKENS = 16_384;

	@Override
	public @Nullable String apiKey() {
		return apiKey != null ? apiKey : null;
	}

	@Override
	public String baseUrl() {
		return baseUrl != null && !baseUrl.isBlank() ? baseUrl : DEFAULT_BASE_URL;
	}

	@Override
	public String model() {
		return model != null && !model.isBlank() ? model : DEFAULT_MODEL;
	}

	@Override
	public Integer maxTokens() {
		return maxTokens != null && maxTokens > 0 ? maxTokens : DEFAULT_MAX_TOKENS;
	}
}

