package com.jazzify.backend.shared.omr;

import java.util.Locale;
import java.util.Set;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

import com.jazzify.backend.shared.exception.code.OmrErrorCode;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class OmrFileValidator {

	private static final Set<String> ALLOWED_EXTENSIONS = Set.of("png", "jpg", "jpeg");

	public static void validate(MultipartFile file) {
		if (file.isEmpty()) {
			throw OmrErrorCode.OMR_FILE_EMPTY.toException();
		}

		String originalFilename = file.getOriginalFilename();
		String extension = extractExtension(originalFilename);
		if (extension == null || !ALLOWED_EXTENSIONS.contains(extension)) {
			throw OmrErrorCode.OMR_INVALID_FILE_TYPE.toException(
				originalFilename != null ? originalFilename : "unknown"
			);
		}
	}

	public static @Nullable String extractExtension(@Nullable String originalFilename) {
		if (originalFilename == null) {
			return null;
		}

		int lastDotIndex = originalFilename.lastIndexOf('.');
		if (lastDotIndex < 0 || lastDotIndex == originalFilename.length() - 1) {
			return null;
		}

		return originalFilename.substring(lastDotIndex + 1).toLowerCase(Locale.ROOT);
	}
}

