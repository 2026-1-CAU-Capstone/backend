package com.jazzify.backend.domain.lick.dto.request;

import java.util.List;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

@NullMarked
@JsonIgnoreProperties(ignoreUnknown = true)
public record SheetDataRequest(
	@Nullable @Size(max = 255) String title,

	@Nullable @Size(max = 20) String key,
	@Nullable @Size(max = 10) String timeSignature,
	@Nullable Integer tempo,

	@NotEmpty @Valid
	List<MeasureRequest> measures
) {
}

