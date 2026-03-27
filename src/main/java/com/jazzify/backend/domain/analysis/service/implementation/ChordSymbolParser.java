package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.ChordEntry;
import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.model.SongInput;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 코드 기호 파서.
 * 텍스트 코드 진행 문자열("Dm7 G7 | Cmaj7")을 파싱하여 ParsedChord 객체 리스트로 변환한다.
 * Python text_parser.py에서 포팅됨.
 */
@Component
public class ChordSymbolParser {

    /** 코드 기호 정규식: (근음)(품질+텐션)(슬래시 베이스) 3그룹으로 분해 */
    private static final Pattern CHORD_RE = Pattern.compile(
            "^([A-Ga-g][#b\\u266F\\u266D]*)" +  // root
            "(.*?)" +                              // quality + tensions
            "(?:/([A-Ga-g][#b\\u266F\\u266D]*))?$" // optional slash bass
    );

    /** "N.C.", "NC", "no chord" 등 코드 없음 표기를 인식하는 정규식 */
    private static final Pattern NO_CHORD_RE = Pattern.compile(
            "^(N\\.?C\\.?|NC|no\\s*chord)$", Pattern.CASE_INSENSITIVE
    );

    // ── public API ──

    public record ParseResult(SongInput song, List<ParsedChord> chords) {}

    /**
     * 텍스트 코드 진행 전체를 파싱한다.
     * "|"로 마디를 나누고, 공백으로 코드를 나눈 뒤, 각 코드를 ParsedChord로 변환한다.
     *
     * 알고리즘:
     * 1) 박자에서 한 마디당 박 수를 계산 (예: "4/4" → 4박)
     * 2) 텍스트를 줄 단위 → "|" 단위로 분할하여 마디 텍스트 리스트 생성
     * 3) 각 마디 내 코드 수로 박 지속시간을 균등 분배 (beatsPerBar / nChords)
     * 4) 개별 코드를 parseChordSymbol()로 변환
     */
    public ParseResult parseProgressionText(String text, String title, String key, String timeSignature) {
        // 박자 분자 추출 (예: "3/4" → 3)
        int beatsPerBar = 4;
        if (timeSignature.contains("/")) {
            beatsPerBar = Integer.parseInt(timeSignature.split("/")[0]);
        }

        // 텍스트를 줄 → "|" 단위로 분할하여 마디별 텍스트 리스트 생성
        List<String> barTexts = new ArrayList<>();
        for (String line : text.strip().split("\\n")) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) continue; // 빈 줄·주석 무시
            for (String b : line.split("\\|")) {
                String bt = b.strip();
                if (!bt.isEmpty()) barTexts.add(bt);
            }
        }

        List<ChordEntry> entries = new ArrayList<>();
        List<ParsedChord> parsed = new ArrayList<>();
        int barNum = 0;

        // 마디별로 코드를 파싱하고 박 위치·지속시간을 계산
        for (String barText : barTexts) {
            barNum++;
            // 마디 내 코드 기호를 공백으로 분할하고 N.C. 필터링
            String[] symbols = barText.split("\\s+");
            List<String> valid = new ArrayList<>();
            for (String s : symbols) {
                if (!NO_CHORD_RE.matcher(s).matches()) valid.add(s);
            }
            if (valid.isEmpty()) continue;

            // 마디 내 코드 수에 따라 박 지속시간을 균등 분배
            int nChords = valid.size();
            double beatDuration = (double) beatsPerBar / nChords;

            for (int i = 0; i < nChords; i++) {
                String sym = valid.get(i);
                double beat = 1.0 + i * beatDuration;

                entries.add(new ChordEntry(barNum, beat, sym, beatDuration));

                ParsedChord pc = parseChordSymbol(sym, barNum, beat, beatDuration);
                if (pc != null) parsed.add(pc);
            }
        }

        SongInput song = new SongInput(title, key, timeSignature, entries, null);
        return new ParseResult(song, parsed);
    }

    /**
     * 단일 코드 기호 문자열을 ParsedChord 객체로 변환한다.
     * 정규식으로 근음/품질/베이스를 분리한 뒤, 각각을 피치클래스와 내부 품질명으로 변환한다.
     */
    public ParsedChord parseChordSymbol(String symbol, int bar, double beat, double durationBeats) {
        symbol = symbol.strip();
        if (symbol.isEmpty()) return null;
        if (NO_CHORD_RE.matcher(symbol).matches()) return null;

        // 정규식으로 근음 / 품질+텐션 / 슬래시 베이스 3그룹 분리
        Matcher m = CHORD_RE.matcher(symbol);
        if (!m.matches()) throw new IllegalArgumentException("Cannot parse chord symbol: '" + symbol + "'");

        String rootStr = m.group(1);        // 근음 문자열 (예: "D", "Bb")
        String qualitySuffix = m.group(2) != null ? m.group(2) : ""; // 품질+텐션 부분
        String bassStr = m.group(3);        // 슬래시 베이스 (null 가능)

        // 근음·베이스를 피치클래스로 변환
        int root = NoteUtils.parseNoteName(rootStr);
        if (root < 0) throw new IllegalArgumentException("Cannot parse root note: '" + rootStr + "'");

        Integer bass = bassStr != null ? boxedPc(NoteUtils.parseNoteName(bassStr)) : null;
        // 품질 접미사를 내부 품질명 + 텐션 리스트로 파싱
        QualityTensions qt = parseQualityAndTensions(qualitySuffix);

        return ParsedChord.builder()
                .originalSymbol(symbol)
                .root(root)
                .quality(qt.quality)
                .tensions(qt.tensions)
                .bass(bass)
                .bar(bar)
                .beat(beat)
                .durationBeats(durationBeats)
                .build();
    }

    // ── private helpers ──

    private static Integer boxedPc(int pc) {
        return pc < 0 ? null : pc;
    }

    private record QualityTensions(String quality, List<String> tensions) {}

    /**
     * 품질/확장 접미사를 파싱하여 내부 품질명(quality)과 텐션 리스트를 반환한다.
     *
     * 알고리즘:
     * 1) 괄호 안의 텐션을 먼저 추출 (예: "(11)" → tensions에 "11" 추가)
     * 2) 유니코드 특수문자를 표준 표기로 변환 (Δ→maj7, °→dim, ø→min7b5)
     * 3) 우선순위 순으로 정규식 패턴 매칭 → quality 결정 (minmaj7이 가장 먼저, maj가 마지막)
     * 4) 확장형(9, 11, 13)은 하위 텐션도 함께 추가 (예: 13 → 9,11,13)
     * 5) 남은 문자열에서 추가 텐션 추출 (#11, b9 등)
     * 6) 중복 제거 후 반환
     */
    private QualityTensions parseQualityAndTensions(String suffix) {
        List<String> tensions = new ArrayList<>();
        if (suffix == null || suffix.isEmpty()) return new QualityTensions("maj", tensions);

        // 1) 괄호 안 텐션 추출 – 단, (maj7)/(m7) 등 품질 표시는 제외
        List<String> parenTensions = new ArrayList<>();
        Pattern parenPat = Pattern.compile("\\(([^)]+)\\)");
        Matcher pm = parenPat.matcher(suffix);
        if (pm.find()) {
            String content = pm.group(1);
            String lower = content.toLowerCase();
            if (!(lower.equals("maj7") || lower.equals("m7") || lower.equals("maj9"))) {
                for (String t : content.split("[,\\s]+")) {
                    t = t.strip();
                    if (!t.isEmpty()) parenTensions.add(t);
                }
                suffix = suffix.substring(0, pm.start()) + suffix.substring(pm.end());
            }
        }

        // 2) 유니코드 특수문자를 표준 표기로 치환
        suffix = suffix.replace("\u0394", "maj7")    // Δ → maj7
                       .replace("\u00B0", "dim")      // ° → dim
                       .replace("\u2205", "min7b5")   // ∅ → min7b5
                       .replace("\u00F8", "min7b5");  // ø → min7b5

        String s = suffix.strip();
        String quality = null;

        // 3) 우선순위 순 정규식 패턴 매칭으로 quality 결정
        //    순서 중요: 더 구체적인 패턴(minmaj7, m7b5)을 먼저 매칭해야 함

        // minmaj7: mMaj7, m(maj7), min(maj7), -(maj7)
        if (quality == null && matchAny(s, "(?i)^m\\s*\\(?\\s*maj\\s*7\\s*\\)?",
                "(?i)^min\\s*\\(?\\s*maj\\s*7\\s*\\)?",
                "(?i)^-\\s*\\(?\\s*maj\\s*7\\s*\\)?",
                "^mM7", "(?i)^mMaj7")) {
            quality = "minmaj7";
            s = replaceFirst(s, "(?i)^(m|min|-)\\s*\\(?\\s*(maj|M)\\s*7\\s*\\)?", "");
        }
        // madd
        if (quality == null && matches(s, "(?i)^m\\s*add")) {
            quality = "min";
            s = replaceFirst(s, "(?i)^m\\s*add", "");
            Matcher am = Pattern.compile("^(\\d+)").matcher(s);
            if (am.find()) { tensions.add(am.group(1)); s = s.substring(am.end()); }
        }
        // add
        if (quality == null && matches(s, "(?i)^add")) {
            quality = "maj";
            s = replaceFirst(s, "(?i)^add", "");
            Matcher am = Pattern.compile("^(\\d+)").matcher(s);
            if (am.find()) { tensions.add(am.group(1)); s = s.substring(am.end()); }
        }
        // min7b5 / half-dim
        if (quality == null && matches(s, "(?i)^(m7b5|min7b5|-7b5|m7\\u266D5)")) {
            quality = "min7b5";
            s = replaceFirst(s, "(?i)^(m7b5|min7b5|-7b5|m7\\u266D5)", "");
        }
        // dim7
        if (quality == null && matches(s, "(?i)^(dim7|o7)")) {
            quality = "dim7";
            s = replaceFirst(s, "(?i)^(dim7|o7)", "");
        }
        // dim (triad)
        if (quality == null && matches(s, "(?i)^(dim|o)(?!7)")) {
            quality = "dim";
            s = replaceFirst(s, "(?i)^(dim|o)", "");
        }
        // augmaj7
        if (quality == null && matches(s, "(?i)^(aug\\s*maj7|\\+\\s*maj7|maj7#5|M7#5)")) {
            quality = "augmaj7";
            s = replaceFirst(s, "(?i)^(aug\\s*maj7|\\+\\s*maj7|maj7#5|M7#5)", "");
        }
        // aug7
        if (quality == null && matches(s, "(?i)^(aug7|\\+7|7#5|7\\+)")) {
            quality = "aug7";
            s = replaceFirst(s, "(?i)^(aug7|\\+7|7#5|7\\+)", "");
        }
        // aug (triad)
        if (quality == null && matches(s, "(?i)^(aug|\\+)(?![\\d])")) {
            quality = "aug";
            s = replaceFirst(s, "(?i)^(aug|\\+)", "");
        }
        // 7sus4
        if (quality == null && matches(s, "(?i)^7sus4?")) {
            quality = "dom7sus4";
            s = replaceFirst(s, "(?i)^7sus4?", "");
        }
        // sus4
        if (quality == null && matches(s, "(?i)^sus4?(?!2)")) {
            quality = "sus4";
            s = replaceFirst(s, "(?i)^sus4?", "");
        }
        // sus2
        if (quality == null && matches(s, "(?i)^sus2")) {
            quality = "sus2";
            s = replaceFirst(s, "(?i)^sus2", "");
        }
        // Extended minor: m13, m11, m9
        if (quality == null && matches(s, "(?i)^(m|min|-)13")) {
            quality = "min7"; s = replaceFirst(s, "(?i)^(m|min|-)13", "");
            tensions.addAll(List.of("9", "11", "13"));
        }
        if (quality == null && matches(s, "(?i)^(m|min|-)11")) {
            quality = "min7"; s = replaceFirst(s, "(?i)^(m|min|-)11", "");
            tensions.addAll(List.of("9", "11"));
        }
        if (quality == null && matches(s, "(?i)^(m|min|-)9")) {
            quality = "min7"; s = replaceFirst(s, "(?i)^(m|min|-)9", "");
            tensions.add("9");
        }
        // min7
        if (quality == null && matches(s, "(?i)^(m7|min7|-7|mi7)")) {
            quality = "min7";
            s = replaceFirst(s, "(?i)^(m7|min7|-7|mi7)", "");
        }
        // min6
        if (quality == null && matches(s, "(?i)^(m6|min6|-6)")) {
            quality = "min6";
            s = replaceFirst(s, "(?i)^(m6|min6|-6)", "");
        }
        // min (triad)
        if (quality == null && matches(s, "(?i)^(m(?!aj)|min|-(?!\\d))(?!7|9|11|13|6|M)")) {
            quality = "min";
            s = replaceFirst(s, "(?i)^(m|min|-)", "");
        }
        // maj13, maj11, maj9
        if (quality == null && matches(s, "^(maj13|ma13|M13|Maj13)")) {
            quality = "maj7"; s = replaceFirst(s, "^(maj13|ma13|M13|Maj13)", "");
            tensions.addAll(List.of("9", "11", "13"));
        }
        if (quality == null && matches(s, "^(maj11|ma11|M11|Maj11)")) {
            quality = "maj7"; s = replaceFirst(s, "^(maj11|ma11|M11|Maj11)", "");
            tensions.addAll(List.of("9", "11"));
        }
        if (quality == null && matches(s, "^(maj9|ma9|M9|Maj9)")) {
            quality = "maj7"; s = replaceFirst(s, "^(maj9|ma9|M9|Maj9)", "");
            tensions.add("9");
        }
        // maj7
        if (quality == null && matches(s, "^(maj7|ma7|M7|Maj7|\\^7|\\^)")) {
            quality = "maj7";
            s = replaceFirst(s, "^(maj7|ma7|M7|Maj7|\\^7|\\^)", "");
        }
        // maj6
        if (quality == null && matches(s, "^(maj6|M6)")) {
            quality = "maj6";
            s = replaceFirst(s, "^(maj6|M6)", "");
        }
        // 6
        if (quality == null && matches(s, "^6(?!/)")) {
            quality = "maj6";
            s = replaceFirst(s, "^6", "");
        }
        // 7alt
        if (quality == null && matches(s, "(?i)^7alt")) {
            quality = "dom7"; s = replaceFirst(s, "(?i)^7alt", "");
            tensions.addAll(List.of("b9", "#9", "#11", "b13"));
        }
        // Extended dominant: 13, 11, 9
        if (quality == null && matches(s, "^13")) {
            quality = "dom7"; s = replaceFirst(s, "^13", "");
            tensions.addAll(List.of("9", "11", "13"));
        }
        if (quality == null && matches(s, "^11")) {
            quality = "dom7"; s = replaceFirst(s, "^11", "");
            tensions.addAll(List.of("9", "11"));
        }
        if (quality == null && matches(s, "^9")) {
            quality = "dom7"; s = replaceFirst(s, "^9", "");
            tensions.add("9");
        }
        // dom7
        if (quality == null && matches(s, "^7")) {
            quality = "dom7";
            s = replaceFirst(s, "^7", "");
        }
        // maj (triad explicit)
        if (quality == null && matches(s, "^(maj|M(?!7))(?!7|9|11|13)")) {
            quality = "maj";
            s = replaceFirst(s, "^(maj|M)", "");
        }
        // power chord
        if (quality == null && matches(s, "^5$")) {
            quality = "power"; s = "";
        }
        if (quality == null) quality = "maj"; // 아무 패턴에도 안 걸리면 메이저 트라이어드

        // 5) 남은 문자열에서 추가 텐션 추출 (#11, b9, 13 등)
        Matcher rm = Pattern.compile("[#b\\u266F\\u266D]*\\d+").matcher(s);
        while (rm.find()) tensions.add(rm.group());
        tensions.addAll(parenTensions);

        // 6) 순서를 유지하면서 중복 제거
        List<String> unique = new ArrayList<>(new LinkedHashSet<>(tensions));

        return new QualityTensions(quality, unique);
    }

    // ── 정규식 헬퍼 메서드 ──

    /** 문자열에서 정규식 패턴이 존재하는지 확인 */
    private static boolean matches(String s, String regex) {
        return Pattern.compile(regex).matcher(s).find();
    }

    /** 여러 정규식 패턴 중 하나라도 매칭되는지 확인 */
    private static boolean matchAny(String s, String... patterns) {
        for (String p : patterns) if (matches(s, p)) return true;
        return false;
    }

    /** 문자열에서 첫 번째 정규식 매칭을 치환 */
    private static String replaceFirst(String s, String regex, String replacement) {
        return Pattern.compile(regex).matcher(s).replaceFirst(replacement);
    }
}

