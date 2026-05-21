package com.jazzify.backend.domain.rag.service.implementation;

import static java.util.Map.entry;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import com.jazzify.backend.domain.rag.model.RagDocument;
import com.jazzify.backend.domain.rag.model.RagDocumentDraft;
import com.jazzify.backend.domain.rag.model.RagSourceType;
import com.jazzify.backend.shared.exception.code.RagErrorCode;

@Component
@NullMarked
public class RagFileChunker {

	private static final Map<String, List<String>> TOPIC_TAGS = Map.ofEntries(
		entry("allofme", List.of("secondary-dominant", "extended-secondary", "dim7", "modal-interchange")),
		entry("anthropology", List.of("rhythm-changes", "tritone-sub", "dual-function", "bridge")),
		entry("autumnleaves", List.of("minor-key", "relative-major", "vii-pivot")),
		entry("blueingreen", List.of("key-ambiguity", "tritone-sub", "circular-form")),
		entry("bolivia", List.of("key-center", "modal-interchange", "tritone-sub", "bass-line")),
		entry("cherokee", List.of("diatonic-analysis", "secondary-dominant", "related-to-minor", "two-scale")),
		entry("confirmation", List.of("bebop", "dominant-chain", "pivot", "blues-fourth")),
		entry("donnalee", List.of("bebop", "vii-pivot", "dim7-substitute")),
		entry("flymetothemoon", List.of("reharmonization", "minor-ii-v", "multi-version")),
		entry("ifallinlovetooeasily", List.of("deceptive-resolution", "extended-secondary", "tritone-sub")),
		entry("itcouldhappentoyou", List.of("1625", "dim7", "IMaj7-IIIm7", "II7", "tritone-sub", "augmented")),
		entry("justfriends", List.of("flat-VII7", "tritone-sub", "dual-function", "IV-opening")),
		entry("momentsnotice", List.of("coltrane", "dim-axis", "local-key", "pivot", "pattern")),
		entry("somedaymyprincewillcome", List.of("whole-tone", "augmented", "3-4-time")),
		entry("thedaysofwineandroses", List.of("chord-tone-plus-key", "lydian-b7", "mixolydian")),
		entry("theendofaloveaffair", List.of("dim7-function", "dual-function", "related-keys", "modal-interchange")),
		entry("therewillneverbeanotheryou", List.of("non-diatonic", "secondary-dominant", "II7", "backdoor"))
	);

	private static final Map<String, Pattern> META_PATTERNS = Map.ofEntries(
		entry("song", Pattern.compile("\\*{0,2}곡명\\s*(?:\\([^)]*\\))?\\s*:\\*{0,2}\\s*(.+)")),
		entry("composer", Pattern.compile("\\*{0,2}작곡\\s*(?:\\([^)]*\\))?\\s*:\\*{0,2}\\s*(.+)")),
		entry("key", Pattern.compile("\\*{0,2}센터 키\\s*(?:\\([^)]*\\))?\\s*:\\*{0,2}\\s*(.+)")),
		entry("form", Pattern.compile("\\*{0,2}형식\\s*(?:\\([^)]*\\))?\\s*:\\*{0,2}\\s*(.+)")),
		entry("source", Pattern.compile("\\*{0,2}강의 출처\\s*(?:\\([^)]*\\))?\\s*:\\*{0,2}\\s*(.+)")),
		entry("analyzed_songs", Pattern.compile("분석 대상 곡\\s*(?:\\([^)]*\\))?\\s*:\\s*(.+)"))
	);

	private static final Pattern SECTION_PATTERN = Pattern.compile(
		"^(?:###\\s+)?(\\d+-\\d+)\\.\\s+(.+?)$\\n(.*?)(?=^(?:###\\s+)?\\d+-\\d+\\.|\\Z)",
		Pattern.MULTILINE | Pattern.DOTALL
	);
	private static final Pattern INSTRUCTION_PATTERN = Pattern.compile(
		"\\*\\*instruction(?:\\s*\\(KR\\))?:\\*\\*\\s*(.+?)(?=\\n\\*\\*(?:instruction|response)\\s*(?:\\([A-Z]+\\))?:|\\Z)",
		Pattern.DOTALL
	);
	private static final Pattern RESPONSE_PATTERN = Pattern.compile(
		"\\*\\*response(?:\\s*\\(KR\\))?:\\*\\*\\s*(.+?)(?=\\n\\*\\*response\\s*\\([A-Z]+\\):|\\Z)",
		Pattern.DOTALL
	);

	public List<ParsedDocument> parseDocuments(Path dataRoot) {
		if (!Files.isDirectory(dataRoot)) {
			throw RagErrorCode.RAG_BOOTSTRAP_PATH_INVALID.toException(dataRoot.toString());
		}

		List<ParsedDocument> parsedDocuments = new ArrayList<>();
		parsedDocuments.addAll(parseDirectory(dataRoot.resolve("standards"), RagSourceType.STANDARD));
		parsedDocuments.addAll(parseDirectory(dataRoot.resolve("lessons"), RagSourceType.LESSON));
		return parsedDocuments;
	}

	public ParsedDocument parseDocument(
		UUID publicId,
		int embeddingVersion,
		RagDocumentDraft draft
	) {
		String content = draft.content().trim();
		if (content.isBlank()) {
			return new ParsedDocument(
				new RagDocument(publicId, draft.slug(), draft.sourceType(), draft.title(), draft.content(), Map.of(), draft.topicTags(), embeddingVersion, 0),
				List.of()
			);
		}

		Map<String, String> parsedMetadata = parseMeta(content);
		Map<String, String> mergedMetadata = mergeMetadata(parsedMetadata, draft.metadata());
		List<String> topicTags = !draft.topicTags().isEmpty()
			? draft.topicTags()
			: (draft.sourceType() == RagSourceType.STANDARD ? TOPIC_TAGS.getOrDefault(draft.slug(), List.of()) : List.of());

		RagDocument document = new RagDocument(
			publicId,
			draft.slug(),
			draft.sourceType(),
			draft.title(),
			draft.content(),
			mergedMetadata,
			topicTags,
			embeddingVersion,
			0
		);

		List<ParsedChunk> chunks = new ArrayList<>();
		for (Section section : parseSections(content)) {
			QaBlock qaBlock = parseQa(section.body());
			chunks.add(new ParsedChunk(
				buildChunkId(draft.sourceType(), draft.slug(), section.sectionId()),
				section.sectionId(),
				parseLevel(section.sectionId()),
				section.title(),
				qaBlock.instruction(),
				qaBlock.response(),
				buildEmbedText(draft.sourceType(), mergedMetadata, section.title(), qaBlock.instruction(), qaBlock.response()),
				mergedMetadata.getOrDefault("song", ""),
				mergedMetadata.getOrDefault("key", ""),
				mergedMetadata.getOrDefault("source", ""),
				mergedMetadata.getOrDefault("analyzed_songs", ""),
				topicTags
			));
		}

		return new ParsedDocument(document, chunks);
	}

	private List<ParsedDocument> parseDirectory(Path directory, RagSourceType sourceType) {
		if (!Files.isDirectory(directory)) {
			return List.of();
		}

		try (Stream<Path> pathStream = Files.list(directory)) {
			return pathStream
				.filter(path -> path.getFileName().toString().endsWith(".txt"))
				.sorted(Comparator.comparing(path -> path.getFileName().toString()))
				.map(path -> parseFile(path, sourceType))
				.filter(parsed -> !parsed.chunks().isEmpty())
				.toList();
		} catch (IOException e) {
			throw RagErrorCode.RAG_BOOTSTRAP_FAILED.toException(e.getMessage());
		}
	}

	private ParsedDocument parseFile(Path path, RagSourceType sourceType) {
		try {
			String content = Files.readString(path, StandardCharsets.UTF_8);
			String fileBase = fileBase(path);
			if (content.isBlank()) {
				return new ParsedDocument(
					new RagDocument(UUID.randomUUID(), fileBase, sourceType, fileBase, "", Map.of(), List.of(), 1, 0),
					List.of()
				);
			}

			Map<String, String> meta = parseMeta(content);
			List<String> topicTags = sourceType == RagSourceType.STANDARD ? TOPIC_TAGS.getOrDefault(fileBase, List.of()) : List.of();
			String title = !meta.getOrDefault("song", "").isBlank()
				? meta.get("song")
				: (!meta.getOrDefault("source", "").isBlank() ? meta.get("source") : fileBase);

			RagDocument document = new RagDocument(
				UUID.randomUUID(),
				fileBase,
				sourceType,
				title,
				content,
				meta,
				topicTags,
				1,
				0
			);

			List<ParsedChunk> chunks = new ArrayList<>();
			for (Section section : parseSections(content)) {
				QaBlock qaBlock = parseQa(section.body());
				chunks.add(new ParsedChunk(
					buildChunkId(sourceType, fileBase, section.sectionId()),
					section.sectionId(),
					parseLevel(section.sectionId()),
					section.title(),
					qaBlock.instruction(),
					qaBlock.response(),
					buildEmbedText(sourceType, meta, section.title(), qaBlock.instruction(), qaBlock.response()),
					meta.getOrDefault("song", ""),
					meta.getOrDefault("key", ""),
					meta.getOrDefault("source", ""),
					meta.getOrDefault("analyzed_songs", ""),
					topicTags
				));
			}

			return new ParsedDocument(document, chunks);
		} catch (IOException e) {
			throw RagErrorCode.RAG_BOOTSTRAP_FAILED.toException(e.getMessage());
		}
	}

	private Map<String, String> parseMeta(String content) {
		Map<String, String> metadata = new LinkedHashMap<>();
		for (Map.Entry<String, Pattern> entry : META_PATTERNS.entrySet()) {
			Matcher matcher = entry.getValue().matcher(content);
			metadata.put(entry.getKey(), matcher.find() ? matcher.group(1).trim() : "");
		}
		return metadata;
	}

	private Map<String, String> mergeMetadata(Map<String, String> parsedMetadata, Map<String, String> requestMetadata) {
		Map<String, String> merged = new LinkedHashMap<>(parsedMetadata);
		for (Map.Entry<String, String> entry : requestMetadata.entrySet()) {
			String value = entry.getValue();
			if (value != null && !value.isBlank()) {
				merged.put(entry.getKey(), value.trim());
			}
		}
		return Map.copyOf(merged);
	}

	private List<Section> parseSections(String content) {
		List<Section> sections = new ArrayList<>();
		Matcher matcher = SECTION_PATTERN.matcher(content);
		while (matcher.find()) {
			sections.add(new Section(matcher.group(1), matcher.group(2).trim(), matcher.group(3).trim()));
		}
		return sections;
	}

	private QaBlock parseQa(String body) {
		Matcher instructionMatcher = INSTRUCTION_PATTERN.matcher(body);
		Matcher responseMatcher = RESPONSE_PATTERN.matcher(body);
		String instruction = instructionMatcher.find() ? instructionMatcher.group(1).trim() : "";
		String response = responseMatcher.find() ? responseMatcher.group(1).trim() : body;
		return new QaBlock(instruction, response);
	}

	private String buildEmbedText(
		RagSourceType sourceType,
		Map<String, String> meta,
		String title,
		String instruction,
		String response
	) {
		String header;
		if (sourceType == RagSourceType.STANDARD) {
			List<String> headerParts = new ArrayList<>();
			if (!meta.getOrDefault("song", "").isBlank()) {
				headerParts.add(meta.get("song"));
			}
			if (!meta.getOrDefault("key", "").isBlank()) {
				headerParts.add(meta.get("key"));
			}
			header = headerParts.isEmpty() ? "" : "[" + String.join(" · ", headerParts) + "]";
		} else {
			List<String> bits = new ArrayList<>();
			if (!meta.getOrDefault("source", "").isBlank()) {
				bits.add("강의: " + meta.get("source"));
			}
			if (!meta.getOrDefault("analyzed_songs", "").isBlank()) {
				bits.add("분석: " + meta.get("analyzed_songs"));
			}
			header = bits.isEmpty() ? "" : "[" + String.join(" / ", bits) + "]";
		}

		String body = instruction.isBlank()
			? title + "\n" + response
			: title + "\n질문: " + instruction + "\n답변: " + response;
		return header.isBlank() ? body : header + "\n" + body;
	}

	private int parseLevel(String sectionId) {
		return Integer.parseInt(sectionId.split("-")[0]);
	}

	private String buildChunkId(RagSourceType sourceType, String fileBase, String sectionId) {
		return sourceType.dbValue() + "__" + fileBase + "__" + sectionId;
	}

	private String fileBase(Path path) {
		String filename = path.getFileName().toString();
		return filename.endsWith(".txt") ? filename.substring(0, filename.length() - 4) : filename;
	}

	public record ParsedDocument(
		RagDocument document,
		List<ParsedChunk> chunks
	) {
	}

	public record ParsedChunk(
		String chunkId,
		String sectionId,
		int level,
		String title,
		String instruction,
		String response,
		String embedText,
		String song,
		String key,
		String source,
		String analyzedSongs,
		List<String> topicTags
	) {
	}

	private record Section(
		String sectionId,
		String title,
		String body
	) {
	}

	private record QaBlock(
		String instruction,
		String response
	) {
	}
}


