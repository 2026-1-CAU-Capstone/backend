package com.jazzify.backend.domain.rag.dto.request;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@NullMarked
public record RagDocumentUpdateRequest(
	@NotBlank(message = "slug는 필수입니다.")
	@Size(max = 120, message = "slug는 120자를 넘을 수 없습니다.")
	String slug,

	@NotBlank(message = "sourceType은 필수입니다.")
	String sourceType,

	@NotBlank(message = "title은 필수입니다.")
	@Size(max = 200, message = "title은 200자를 넘을 수 없습니다.")
	String title,

	@NotBlank(message = "content는 필수입니다.")
	String content,

	@NotNull(message = "metadata는 null일 수 없습니다.")
	Map<String, String> metadata,

	@NotNull(message = "topicTags는 null일 수 없습니다.")
	List<@NotBlank(message = "topicTag는 비어 있을 수 없습니다.") String> topicTags
) {

	public RagDocumentUpdateRequest {
		metadata = Map.copyOf(metadata);
		topicTags = List.copyOf(topicTags);
	}
}


