package com.jazzify.backend.shared.omr;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.NullMarked;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.jazzify.backend.shared.exception.CustomException;

@NullMarked
class OmrFileValidatorTest {

	@Test
	void validate_acceptsPdfFile() {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"score.pdf",
			"application/pdf",
			"dummy".getBytes(StandardCharsets.UTF_8)
		);

		assertThatCode(() -> OmrFileValidator.validate(file)).doesNotThrowAnyException();
	}

	@Test
	void validate_rejectsUnsupportedFile() {
		MockMultipartFile file = new MockMultipartFile(
			"file",
			"score.txt",
			"text/plain",
			"dummy".getBytes(StandardCharsets.UTF_8)
		);

		CustomException exception = assertThrows(CustomException.class, () -> OmrFileValidator.validate(file));
		assertThatCode(() -> {
			if (!"OMR_004".equals(exception.getCode())) {
				throw new AssertionError("unexpected code: " + exception.getCode());
			}
		}).doesNotThrowAnyException();
	}
}

