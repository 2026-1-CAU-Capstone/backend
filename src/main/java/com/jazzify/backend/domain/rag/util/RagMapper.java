package com.jazzify.backend.domain.rag.util;

import org.jspecify.annotations.NullMarked;

import com.jazzify.backend.domain.rag.dto.response.RagChunkResponse;
import com.jazzify.backend.domain.rag.dto.response.RagDocumentResponse;
import com.jazzify.backend.domain.rag.dto.response.RagDocumentSummaryResponse;
import com.jazzify.backend.domain.rag.model.RagChunkSearchResult;
import com.jazzify.backend.domain.rag.model.RagDocument;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RagMapper {

	public static RagChunkResponse toChunkResponse(RagChunkSearchResult chunk) {
		return new RagChunkResponse(
			chunk.id(),
			chunk.score(),
			chunk.sourceType().dbValue(),
			chunk.title(),
			chunk.song(),
			chunk.key(),
			chunk.level(),
			chunk.sectionId(),
			chunk.instruction(),
			chunk.response(),
			chunk.topicTags(),
			chunk.source(),
			chunk.analyzedSongs(),
			chunk.rrfScore(),
			chunk.matchedQueries()
		);
	}

	public static RagDocumentResponse toDocumentResponse(RagDocument document) {
		return new RagDocumentResponse(
			document.publicId(),
			document.slug(),
			document.sourceType().dbValue(),
			document.title(),
			document.content(),
			document.metadata(),
			document.topicTags(),
			document.embeddingVersion(),
			document.chunkCount()
		);
	}

	public static RagDocumentSummaryResponse toDocumentSummaryResponse(RagDocument document) {
		return new RagDocumentSummaryResponse(
			document.publicId(),
			document.slug(),
			document.sourceType().dbValue(),
			document.title(),
			document.topicTags(),
			document.embeddingVersion(),
			document.chunkCount()
		);
	}
}


