package com.jazzify.backend.domain.rag.service.implementation;

import org.jspecify.annotations.NullMarked;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@NullMarked
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagBootstrapRunner implements ApplicationRunner {

	private final RagWriter ragWriter;

	@Override
	public void run(ApplicationArguments args) {
		ragWriter.initializeSchemaIfEnabled();
		ragWriter.bootstrapFromFilesystemIfEnabled();
	}
}

