package com.jazzify.backend.domain.rag.service.implementation;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@NullMarked
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagBootstrapRunner implements ApplicationRunner {

	private final RagWriter ragWriter;

	@Override
	public void run(ApplicationArguments args) {
		try {
			ragWriter.initializeSchemaIfEnabled();
		} catch (Exception e) {
			log.warn("[RAG] 스키마 초기화 중 오류가 발생했습니다. 애플리케이션은 계속 실행됩니다. error={}", e.getMessage(), e);
		}

		try {
			ragWriter.bootstrapFromFilesystemIfEnabled();
		} catch (Exception e) {
			log.warn("[RAG] 부트스트랩 중 오류가 발생했습니다. 애플리케이션은 계속 실행됩니다. error={}", e.getMessage(), e);
		}
	}
}

