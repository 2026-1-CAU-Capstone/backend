package com.jazzify.backend.shared.musicxml;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * MusicXML 문자열을 {@link ParsedSheetData}로 변환하는 유틸리티.
 * <p>
 * TypeScript {@code xmlMelodyParser.ts} 로직을 Java로 포팅한 구현이다.
 * XXE 방지 설정이 적용된 JAXP DOM 파서를 사용한다.
 */
@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MusicXmlParser {

	// ─── Constants ──────────────────────────────────────────────────────

	private static final String[] KEY_NAMES = {
		"Cb", "Gb", "Db", "Ab", "Eb", "Bb", "F",
		"C", "G", "D", "A", "E", "B", "F#", "C#"
	};

	private static final String[] MINOR_KEY_NAMES = {
		"Abm", "Ebm", "Bbm", "Fm", "Cm", "Gm", "Dm",
		"Am", "Em", "Bm", "F#m", "C#m", "G#m", "D#m", "A#m"
	};

	private static final Map<String, String> TYPE_TO_VF;
	private static final Map<String, String> ACC_MAP;
	private static final Map<String, Double> DUR_DIVS;
	private static final List<BeatEntry> BEAT_TO_VF;

	static {
		TYPE_TO_VF = new HashMap<>();
		TYPE_TO_VF.put("whole", "w");
		TYPE_TO_VF.put("half", "h");
		TYPE_TO_VF.put("quarter", "q");
		TYPE_TO_VF.put("eighth", "8");
		TYPE_TO_VF.put("16th", "16");
		TYPE_TO_VF.put("32nd", "32");

		ACC_MAP = new HashMap<>();
		ACC_MAP.put("sharp", "#");
		ACC_MAP.put("flat", "b");
		ACC_MAP.put("natural", "n");
		ACC_MAP.put("double-sharp", "#");
		ACC_MAP.put("sharp-sharp", "#");
		ACC_MAP.put("double-flat", "b");
		ACC_MAP.put("flat-flat", "b");

		DUR_DIVS = new HashMap<>();
		DUR_DIVS.put("whole", 4.0);
		DUR_DIVS.put("half", 2.0);
		DUR_DIVS.put("quarter", 1.0);
		DUR_DIVS.put("eighth", 0.5);
		DUR_DIVS.put("16th", 0.25);
		DUR_DIVS.put("32nd", 0.125);

		BEAT_TO_VF = new ArrayList<>();
		BEAT_TO_VF.add(new BeatEntry(4.0, "w", false));
		BEAT_TO_VF.add(new BeatEntry(3.0, "h", true));
		BEAT_TO_VF.add(new BeatEntry(2.0, "h", false));
		BEAT_TO_VF.add(new BeatEntry(1.5, "q", true));
		BEAT_TO_VF.add(new BeatEntry(1.0, "q", false));
		BEAT_TO_VF.add(new BeatEntry(0.75, "8", true));
		BEAT_TO_VF.add(new BeatEntry(0.5, "8", false));
		BEAT_TO_VF.add(new BeatEntry(0.25, "16", false));
	}

	// ─── Public API ─────────────────────────────────────────────────────

	/**
	 * MusicXML 문자열을 파싱하여 {@link ParsedSheetData}를 반환한다.
	 *
	 * @param xml MusicXML 전체 문자열
	 * @return 파싱 결과
	 * @throws IllegalArgumentException XML 파싱 또는 구조 오류 시
	 */
	public static ParsedSheetData parse(String xml) {
		return parse(xml, Map.of());
	}

	/**
	 * MusicXML 문자열을 파싱하되, 외부 chord assignments 결과가 있으면
	 * {@code musicxml_measure_number} 기준으로 마디 코드 심볼을 덮어쓴다.
	 *
	 * @param xml MusicXML 전체 문자열
	 * @param chordsByMeasureNumber 마디 번호별 코드 심볼 매핑
	 * @return 파싱 결과
	 * @throws IllegalArgumentException XML 파싱 또는 구조 오류 시
	 */
	public static ParsedSheetData parse(String xml, Map<String, String> chordsByMeasureNumber) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			// XXE 방지
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			factory.setNamespaceAware(false);

			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(xml)));
			doc.getDocumentElement().normalize();
			return parseDocument(doc, chordsByMeasureNumber);
		} catch (IllegalArgumentException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalArgumentException("MusicXML 파싱 실패: " + e.getMessage(), e);
		}
	}

	// ─── Document ───────────────────────────────────────────────────────

	private static ParsedSheetData parseDocument(Document doc, Map<String, String> chordsByMeasureNumber) {
		Element root = doc.getDocumentElement();

		// ── Title ──
		String title = firstDescendantText(root, "work-title");
		if (title == null) title = firstDescendantText(root, "movement-title");
		if (title == null) title = "Untitled";

		// ── Composer ──
		@Nullable String composer = null;
		NodeList creators = root.getElementsByTagName("creator");
		for (int i = 0; i < creators.getLength(); i++) {
			Element c = (Element) creators.item(i);
			if ("composer".equals(c.getAttribute("type"))) {
				composer = c.getTextContent().trim();
				break;
			}
		}

		// ── Key / Time Signature ──
		Element firstAttr = firstDescendantElement(root, "attributes");
		int fifths = 0;
		String beats = "4";
		String beatType = "4";
		String mode = "major";
		if (firstAttr != null) {
			String fifthsStr = firstDescendantText(firstAttr, "fifths");
			if (fifthsStr != null) fifths = Integer.parseInt(fifthsStr.trim());
			String beatsStr = firstDescendantText(firstAttr, "beats");
			if (beatsStr != null) beats = beatsStr.trim();
			String beatTypeStr = firstDescendantText(firstAttr, "beat-type");
			if (beatTypeStr != null) beatType = beatTypeStr.trim();
			String modeStr = firstDescendantText(firstAttr, "mode");
			if (modeStr != null && !modeStr.isBlank()) mode = modeStr.trim();
		}
		String timeSig = beats + "/" + beatType;
		String key = toAnalysisKey(fifths, mode);

		// ── Key Signature Letters ──
		Set<String> keySigLetters = buildKeySigLetters(fifths);

		// ── Tempo ──
		@Nullable Integer tempo = null;
		NodeList soundEls = root.getElementsByTagName("sound");
		for (int i = 0; i < soundEls.getLength(); i++) {
			Element s = (Element) soundEls.item(i);
			String tempoAttr = s.getAttribute("tempo");
			if (!tempoAttr.isEmpty()) {
				tempo = (int) Math.round(Double.parseDouble(tempoAttr));
				break;
			}
		}

		// ── Measures ── (첫 번째 파트만 처리)
		List<ParsedMeasure> measures = new ArrayList<>();
		NodeList parts = root.getElementsByTagName("part");
		if (parts.getLength() > 0) {
			Element part = (Element) parts.item(0);
			NodeList children = part.getChildNodes();
			for (int i = 0; i < children.getLength(); i++) {
				Node child = children.item(i);
				if (child.getNodeType() == Node.ELEMENT_NODE
					&& "measure".equals(((Element) child).getTagName())) {
					measures.add(parseMeasure((Element) child, keySigLetters, chordsByMeasureNumber));
				}
			}
		}

		return new ParsedSheetData(title, composer, key, timeSig, tempo, measures);
	}

	private static String toAnalysisKey(int fifths, String mode) {
		int index = Math.min(Math.max(fifths + 7, 0), 14);
		return "minor".equalsIgnoreCase(mode) ? MINOR_KEY_NAMES[index] : KEY_NAMES[index];
	}

	// ─── Measure ────────────────────────────────────────────────────────

	private static ParsedMeasure parseMeasure(
		Element mEl,
		Set<String> keySigLetters,
		Map<String, String> chordsByMeasureNumber
	) {
		List<ParsedNote> notes = new ArrayList<>();

		// ── Chord Symbol from <harmony> ──
		String chord = resolveChordFromAssignments(mEl, chordsByMeasureNumber);
		if (chord == null) {
			Element harmEl = firstDirectChildElement(mEl, "harmony");
			if (harmEl != null) {
				String rootStep = firstDescendantText(harmEl, "root-step");
				if (rootStep == null) rootStep = "";
				String rootAlter = firstDescendantText(harmEl, "root-alter");
				String kind = firstDescendantText(harmEl, "kind");
				if (kind == null) kind = "";
				String acc = "1".equals(rootAlter != null ? rootAlter.trim() : "")
					? "#" : "-1".equals(rootAlter != null ? rootAlter.trim() : "") ? "b" : "";
				chord = rootStep.trim() + acc + kindToSymbol(kind.trim());
			}
		}

		// ── Notes ──
		NodeList children = mEl.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() != Node.ELEMENT_NODE) continue;
			Element nEl = (Element) child;
			if (!"note".equals(nEl.getTagName())) continue;

			// 코드 음(동시 화음) 제외
			if (hasDirectChild(nEl, "chord")) continue;

			// Voice 1 만 처리
			String voiceText = directChildTextContent(nEl, "voice");
			if (voiceText != null && !"1".equals(voiceText.trim())) continue;

			// 타이 연속음 제외 (stop만 있고 start는 없는 경우)
			List<Element> tieEls = directChildElements(nEl, "tie");
			boolean tieStart = false;
			boolean tieStop = false;
			for (Element t : tieEls) {
				String tt = t.getAttribute("type");
				if ("start".equals(tt)) tieStart = true;
				if ("stop".equals(tt)) tieStop = true;
			}
			if (tieStop && !tieStart) continue;

			boolean isRest = hasDirectChild(nEl, "rest");
			String typeStr = directChildTextContent(nEl, "type");
			if (typeStr == null) typeStr = "quarter";
			String vf = TYPE_TO_VF.getOrDefault(typeStr.trim(), "q");
			boolean isDotted = hasDirectChild(nEl, "dot");

			if (isRest) {
				notes.add(new ParsedNote(List.of("b/4"), vf + "r", isDotted, null));
				continue;
			}

			Element pitchEl = firstDirectChildElement(nEl, "pitch");
			if (pitchEl == null) continue;

			String step = firstDescendantText(pitchEl, "step");
			if (step == null) step = "C";
			step = step.trim().toLowerCase();
			String octave = firstDescendantText(pitchEl, "octave");
			if (octave == null) octave = "4";

			// ── 임시표 처리 ──
			@Nullable Map<String, String> accidentals = resolveAccidentals(nEl, pitchEl, step, keySigLetters);

			// ── 타이 지속 시간 처리 ──
			String finalVf = vf;
			boolean finalDotted = isDotted;
			if (tieStart) {
				BeatEntry extDur = collectTiedDuration(nEl);
				if (extDur != null) {
					finalVf = extDur.vf();
					finalDotted = extDur.dot();
				}
			}

			notes.add(new ParsedNote(
				List.of(step + "/" + octave.trim()),
				finalVf,
				finalDotted,
				accidentals
			));
		}

		if (notes.isEmpty()) {
			notes.add(new ParsedNote(List.of("b/4"), "wr", false, null));
		}

		return new ParsedMeasure(chord, notes);
	}

	@Nullable
	private static String resolveChordFromAssignments(Element measureEl, Map<String, String> chordsByMeasureNumber) {
		String measureNumber = measureEl.getAttribute("number").trim();
		if (measureNumber.isEmpty()) {
			return null;
		}

		String chord = chordsByMeasureNumber.get(measureNumber);
		if (chord == null) {
			return null;
		}

		String trimmed = chord.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}

	// ─── Accidental Resolution ──────────────────────────────────────────

	@Nullable
	private static Map<String, String> resolveAccidentals(
		Element noteEl, Element pitchEl, String step, Set<String> keySigLetters) {

		boolean isInKeySig = keySigLetters.contains(step);

		// <accidental> 요소 우선 처리
		String accText = directChildTextContent(noteEl, "accidental");
		if (accText != null) {
			String acc = ACC_MAP.get(accText.trim());
			if (acc != null) {
				if ("n".equals(acc) && isInKeySig) return Map.of("0", "n");
				if (!"n".equals(acc) && !isInKeySig) return Map.of("0", acc);
			}
			return null;
		}

		// <alter> 값으로 처리
		String alterStr = firstDescendantText(pitchEl, "alter");
		if (alterStr != null) {
			int alter;
			try {
				alter = (int) Double.parseDouble(alterStr.trim());
			} catch (NumberFormatException e) {
				return null;
			}
			if (alter == 1 && !isInKeySig) return Map.of("0", "#");
			if (alter == -1 && !isInKeySig) return Map.of("0", "b");
			if (alter == 0 && isInKeySig) return Map.of("0", "n");
		}

		return null;
	}

	// ─── Tied Duration ──────────────────────────────────────────────────

	/**
	 * 타이로 묶인 음표들의 합산 박자를 계산해 가장 가까운 표준 음가로 반환한다.
	 */
	@Nullable
	private static BeatEntry collectTiedDuration(Element startNote) {
		double totalBeats = noteDurBeats(startNote);
		Element current = startNote;

		while (true) {
			// 현재 음표에 tie start가 있는지 확인
			boolean hasStart = false;
			for (Element t : directChildElements(current, "tie")) {
				if ("start".equals(t.getAttribute("type"))) {
					hasStart = true;
					break;
				}
			}
			if (!hasStart) break;

			// 다음 <note> 형제 노드 탐색
			@Nullable Element next = findNextNoteSibling(current);
			if (next == null) break;

			// 다음 음표에 tie stop이 있는지 확인
			boolean hasStop = false;
			for (Element t : directChildElements(next, "tie")) {
				if ("stop".equals(t.getAttribute("type"))) {
					hasStop = true;
					break;
				}
			}
			if (!hasStop) break;

			totalBeats += noteDurBeats(next);
			current = next;
		}

		// 가장 가까운 표준 음가로 양자화
		BeatEntry best = BEAT_TO_VF.get(BEAT_TO_VF.size() - 1);
		double diff = Double.MAX_VALUE;
		for (BeatEntry d : BEAT_TO_VF) {
			double dd = Math.abs(d.beats() - totalBeats);
			if (dd < diff) {
				diff = dd;
				best = d;
			}
		}
		return best;
	}

	private static double noteDurBeats(Element noteEl) {
		String typeStr = directChildTextContent(noteEl, "type");
		if (typeStr == null) typeStr = "quarter";
		double b = DUR_DIVS.getOrDefault(typeStr.trim(), 1.0);
		if (hasDirectChild(noteEl, "dot")) b *= 1.5;
		return b;
	}

	// ─── kindToSymbol ───────────────────────────────────────────────────

	private static String kindToSymbol(String kind) {
		return switch (kind) {
			case "major" -> "Δ";
			case "minor" -> "-";
			case "dominant" -> "7";
			case "major-seventh" -> "Δ7";
			case "minor-seventh" -> "-7";
			case "dominant-seventh" -> "7";
			case "half-diminished" -> "ø7";
			case "diminished" -> "°7";
			case "diminished-seventh" -> "°7";
			case "augmented" -> "+";
			case "augmented-seventh" -> "+7";
			case "suspended" -> "sus";
			case "suspended-fourth" -> "sus4";
			case "major-sixth" -> "6";
			case "minor-sixth" -> "-6";
			case "major-minor" -> "-Δ7";
			case "major-ninth" -> "Δ9";
			case "dominant-ninth" -> "9";
			case "minor-ninth" -> "-9";
			default -> kind;
		};
	}

	// ─── Key Signature ──────────────────────────────────────────────────

	private static Set<String> buildKeySigLetters(int fifths) {
		Set<String> letters = new HashSet<>();
		String[] FLAT_LETTERS = {"b", "e", "a", "d", "g", "c", "f"};
		String[] SHARP_LETTERS = {"f", "c", "g", "d", "a", "e", "b"};
		if (fifths < 0) {
			for (int i = 0; i < Math.min(-fifths, 7); i++) letters.add(FLAT_LETTERS[i]);
		} else {
			for (int i = 0; i < Math.min(fifths, 7); i++) letters.add(SHARP_LETTERS[i]);
		}
		return letters;
	}

	// ─── DOM Helpers ────────────────────────────────────────────────────

	/** 요소의 직계 자식 중 첫 번째 매칭 Element를 반환한다. */
	@Nullable
	private static Element firstDirectChildElement(Element parent, String tagName) {
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE
				&& tagName.equals(((Element) child).getTagName())) {
				return (Element) child;
			}
		}
		return null;
	}

	/** 요소의 직계 자식에 해당 tagName이 존재하는지 확인한다. */
	private static boolean hasDirectChild(Element parent, String tagName) {
		return firstDirectChildElement(parent, tagName) != null;
	}

	/** 요소의 직계 자식 중 해당 tagName의 textContent를 반환한다. */
	@Nullable
	private static String directChildTextContent(Element parent, String tagName) {
		Element el = firstDirectChildElement(parent, tagName);
		return el != null ? el.getTextContent() : null;
	}

	/** 요소의 직계 자식 중 해당 tagName의 Element 목록을 반환한다. */
	private static List<Element> directChildElements(Element parent, String tagName) {
		List<Element> result = new ArrayList<>();
		NodeList children = parent.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE
				&& tagName.equals(((Element) child).getTagName())) {
				result.add((Element) child);
			}
		}
		return result;
	}

	/** 요소의 모든 하위 노드 중 첫 번째 매칭 Element를 반환한다. */
	@Nullable
	private static Element firstDescendantElement(Element parent, String tagName) {
		NodeList nl = parent.getElementsByTagName(tagName);
		return nl.getLength() > 0 ? (Element) nl.item(0) : null;
	}

	/** 요소의 모든 하위 노드 중 첫 번째 매칭 요소의 textContent를 반환한다. */
	@Nullable
	private static String firstDescendantText(Element parent, String tagName) {
		Element el = firstDescendantElement(parent, tagName);
		return el != null ? el.getTextContent().trim() : null;
	}

	/** 현재 note 요소의 다음 <note> 형제 Element를 찾는다. */
	@Nullable
	private static Element findNextNoteSibling(Element current) {
		Node sib = current.getNextSibling();
		while (sib != null) {
			if (sib.getNodeType() == Node.ELEMENT_NODE
				&& "note".equals(((Element) sib).getTagName())) {
				return (Element) sib;
			}
			sib = sib.getNextSibling();
		}
		return null;
	}

	// ─── Inner Record ───────────────────────────────────────────────────

	private record BeatEntry(double beats, String vf, boolean dot) {
	}
}

