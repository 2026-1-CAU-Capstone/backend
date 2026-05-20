package com.jazzify.backend.shared.omr;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.web.multipart.MultipartFile;

@NullMarked
public class InMemoryMultipartFile implements MultipartFile {

	private final String name;
	private final String originalFilename;
	private final String contentType;
	private final byte[] bytes;

	public InMemoryMultipartFile(
		String name,
		@Nullable String originalFilename,
		@Nullable String contentType,
		byte[] bytes
	) {
		this.name = name;
		this.originalFilename = originalFilename != null ? originalFilename : "upload.bin";
		this.contentType = contentType != null ? contentType : "application/octet-stream";
		this.bytes = Objects.requireNonNull(bytes);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getOriginalFilename() {
		return originalFilename;
	}

	@Override
	public String getContentType() {
		return contentType;
	}

	@Override
	public boolean isEmpty() {
		return bytes.length == 0;
	}

	@Override
	public long getSize() {
		return bytes.length;
	}

	@Override
	public byte[] getBytes() {
		return bytes.clone();
	}

	@Override
	public InputStream getInputStream() {
		return new ByteArrayInputStream(bytes);
	}

	@Override
	public void transferTo(Path dest) throws IOException, IllegalStateException {
		Files.write(dest, bytes);
	}

	@Override
	public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
		Files.write(dest.toPath(), bytes);
	}
}

