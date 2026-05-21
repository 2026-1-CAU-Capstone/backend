package com.jazzify.backend.domain.rag.service.implementation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.jazzify.backend.domain.rag.config.RagProperties;
import com.jazzify.backend.domain.rag.model.RagChunkSearchResult;
import com.jazzify.backend.domain.rag.model.RagDecomposedQuery;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@NullMarked
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "rag", name = "enabled", havingValue = "true")
public class RagAgent {

	private final RagReader ragReader;
	private final RagEmbeddingClient ragEmbeddingClient;
	private final RagProperties ragProperties;

	public ContextBuildResult buildContext(@Nullable Map<String, Object> chordContext, String userQuestion, @Nullable String songTitle) {
		List<RagDecomposedQuery> queries = decomposeQuery(chordContext, userQuestion, songTitle);
		List<RagChunkSearchResult> results = routeAndRetrieve(queries);
		int topK = Math.max(1, ragProperties.retrieval().topK());
		List<RagChunkSearchResult> topResults = results.stream().limit(topK).toList();

		Map<String, Object> debug = new LinkedHashMap<>();
		debug.put("queries", buildQueryDebug(queries));
		debug.put("total_retrieved", results.size());
		debug.put("top_k", topK);
		debug.put("fusion", "rrf");
		debug.put("rrf_k", ragProperties.retrieval().rrfK());
		debug.put("chunks", buildChunkDebug(topResults));

		log.info("[RAG] queries={} retrieved={} topK={}", queries.size(), results.size(), topK);
		return new ContextBuildResult(formatForLlm(topResults), debug);
	}

	static List<RagDecomposedQuery> decomposeQuery(
		@Nullable Map<String, Object> chordContext,
		String userQuestion,
		@Nullable String songTitle
	) {
		List<RagDecomposedQuery> queries = new ArrayList<>();
		Map<String, Object> safeContext = chordContext != null ? chordContext : Map.of();
		String chord = getString(safeContext, "chord");
		String key = getStringOrDefault(safeContext, "key", "C");
		String function = getString(safeContext, "function");
		String nextChord = getString(safeContext, "next_chord");
		List<String> patterns = getStringList(safeContext, "patterns_detected");
		Map<String, Object> secondaryDominant = getMap(safeContext, "secondary_dominant");
		Map<String, Object> modalInterchange = getMap(safeContext, "modal_interchange");
		String normalizedSongTitle = songTitle != null ? songTitle : "";

		if ("II7".equals(function)) {
			queries.add(new RagDecomposedQuery("II7 코드 특수성 얼터드 안 어울림 샵11 스케일", 1, null));
		} else if ("V7".equals(function) || "D".equals(function)) {
			String target = nextChord.isBlank() ? "I" : nextChord;
			queries.add(new RagDecomposedQuery("도미넌트 세븐 " + chord + " " + target + "로 해결 얼터드 스케일", 1, null));
		} else if ("T".equals(function) || "I".equals(function)) {
			queries.add(new RagDecomposedQuery("토닉 코드 " + chord + " 스케일 선택 솔로", 1, null));
		}

		if (!secondaryDominant.isEmpty()) {
			String targetDegree = getString(secondaryDominant, "targetDegree");
			queries.add(new RagDecomposedQuery("세컨더리 도미넌트 V/" + targetDegree + " 얼터드 스케일 " + chord, 1, "secondary-dominant"));
		}

		if (!modalInterchange.isEmpty()) {
			String sourceMode = getString(modalInterchange, "sourceMode");
			queries.add(new RagDecomposedQuery("모달 인터체인지 " + sourceMode + " " + chord + " 스케일", 1, "modal-interchange"));
		}

		if (patterns.stream().anyMatch(pattern -> pattern.toLowerCase().contains("tritone"))) {
			queries.add(new RagDecomposedQuery("트라이톤 서브스티튜션 " + chord + " 가이드톤 스케일", 1, "tritone-sub"));
		}

		if (patterns.stream().anyMatch(pattern -> pattern.toLowerCase().contains("dim")) || chord.toLowerCase().contains("dim")) {
			queries.add(new RagDecomposedQuery("디미니쉬드 코드 " + chord + " 모체 도미넌트 얼터드", 1, "dim7"));
		}

		if (!normalizedSongTitle.isBlank()) {
			queries.add(0, new RagDecomposedQuery(normalizedSongTitle + " 코드 분석 화성", null, null));
			queries.add(1, new RagDecomposedQuery(normalizedSongTitle + " 솔로 연주 판단", 3, null));
		}

		if (!userQuestion.isBlank() && !"이 코드 진행 분석해줘".equals(userQuestion)) {
			queries.add(new RagDecomposedQuery(userQuestion + " " + chord + " " + key + " 키", 3, null));
		}

		if (queries.isEmpty()) {
			String fallback = (!normalizedSongTitle.isBlank() ? normalizedSongTitle : chord) + " " + key + " 키 코드 분석";
			queries.add(new RagDecomposedQuery(fallback, null, null));
		}

		return List.copyOf(queries);
	}

	private List<RagChunkSearchResult> routeAndRetrieve(List<RagDecomposedQuery> queries) {
		int nPerQuery = Math.max(1, ragProperties.retrieval().nPerQuery());
		int rrfK = Math.max(1, ragProperties.retrieval().rrfK());
		Map<String, Double> rrfScores = new LinkedHashMap<>();
		Map<String, RagChunkSearchResult> chunkData = new LinkedHashMap<>();
		Map<String, List<String>> matchedQueries = new LinkedHashMap<>();
		List<List<Double>> embeddings = ragEmbeddingClient.embed(queries.stream().map(RagDecomposedQuery::query).toList());

		for (int i = 0; i < queries.size(); i++) {
			RagDecomposedQuery query = queries.get(i);
			List<RagChunkSearchResult> results = ragReader.searchByEmbedding(
				embeddings.get(i),
				nPerQuery,
				query.level(),
				null,
				query.tag(),
				null
			);
			for (int rank = 0; rank < results.size(); rank++) {
				RagChunkSearchResult result = results.get(rank);
				rrfScores.merge(result.id(), 1.0 / (rrfK + rank + 1), Double::sum);
				chunkData.putIfAbsent(result.id(), result);
				matchedQueries.computeIfAbsent(result.id(), key -> new ArrayList<>()).add(query.query());
			}
		}

		return rrfScores.entrySet().stream()
			.sorted(Map.Entry.<String, Double>comparingByValue().reversed())
			.map(entry -> chunkData.get(entry.getKey()).withFusion(round(entry.getValue()), List.copyOf(matchedQueries.get(entry.getKey()))))
			.toList();
	}

	private List<Map<String, Object>> buildQueryDebug(List<RagDecomposedQuery> queries) {
		List<Map<String, Object>> debug = new ArrayList<>();
		for (RagDecomposedQuery query : queries) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("query", query.query());
			item.put("level", query.level());
			item.put("tag", query.tag());
			debug.add(item);
		}
		return debug;
	}

	private List<Map<String, Object>> buildChunkDebug(List<RagChunkSearchResult> results) {
		List<Map<String, Object>> debug = new ArrayList<>();
		for (RagChunkSearchResult result : results) {
			Map<String, Object> item = new LinkedHashMap<>();
			item.put("id", result.id());
			item.put("score", round(result.score()));
			item.put("rrf_score", result.rrfScore());
			item.put("title", result.title());
			item.put("song", result.song());
			item.put("level", result.level());
			item.put("matched_query", result.matchedQueries().isEmpty() ? "" : result.matchedQueries().getFirst());
			item.put("matched_queries", result.matchedQueries());
			item.put("response", preview(result.response(), 400));
			debug.add(item);
		}
		return debug;
	}

	private String formatForLlm(List<RagChunkSearchResult> chunks) {
		if (chunks.isEmpty()) {
			return "관련 강의 내용을 찾지 못했습니다.";
		}

		List<String> lines = new ArrayList<>();
		lines.add("[관련 강의 내용 (HarmoRAG)]");
		lines.add("");
		for (int i = 0; i < chunks.size(); i++) {
			RagChunkSearchResult chunk = chunks.get(i);
			lines.add("[" + (i + 1) + "] " + chunk.song() + " — " + chunk.title() + " (Lv" + chunk.level() + ", 유사도: " + round(chunk.score()) + ")");
			if (chunk.instruction() != null && !chunk.instruction().isBlank()) {
				lines.add("Q: " + chunk.instruction());
			}
			lines.add("A: " + chunk.response());
			lines.add("");
		}
		return String.join("\n", lines);
	}

	private static Map<String, Object> getMap(Map<String, Object> source, String key) {
		Object value = source.get(key);
		if (value instanceof Map<?, ?> map) {
			Map<String, Object> converted = new LinkedHashMap<>();
			for (Map.Entry<?, ?> entry : map.entrySet()) {
				if (entry.getKey() != null) {
					converted.put(String.valueOf(entry.getKey()), entry.getValue());
				}
			}
			return converted;
		}
		return Map.of();
	}

	private static List<String> getStringList(Map<String, Object> source, String key) {
		Object value = source.get(key);
		if (value instanceof List<?> list) {
			return list.stream().map(String::valueOf).toList();
		}
		return List.of();
	}

	private static String getString(Map<String, Object> source, String key) {
		Object value = source.get(key);
		return value != null ? String.valueOf(value) : "";
	}

	private static String getStringOrDefault(Map<String, Object> source, String key, String defaultValue) {
		String value = getString(source, key);
		return value.isBlank() ? defaultValue : value;
	}

	private static double round(double value) {
		return Math.round(value * 1_000_000d) / 1_000_000d;
	}

	private static String preview(String value, int maxLength) {
		return value.length() <= maxLength ? value : value.substring(0, maxLength);
	}

	public record ContextBuildResult(
		String llmContext,
		Map<String, Object> debugInfo
	) {
	}
}


