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
 * Chord‑symbol parser.
 * Ported from Python text_parser.py.
 */
@Component
public class ChordSymbolParser {

    private static final Pattern CHORD_RE = Pattern.compile(
            "^([A-Ga-g][#b\\u266F\\u266D]*)" +  // root
            "(.*?)" +                              // quality + tensions
            "(?:/([A-Ga-g][#b\\u266F\\u266D]*))?$" // optional slash bass
    );

    private static final Pattern NO_CHORD_RE = Pattern.compile(
            "^(N\\.?C\\.?|NC|no\\s*chord)$", Pattern.CASE_INSENSITIVE
    );

    // ── public API ──

    public record ParseResult(SongInput song, List<ParsedChord> chords) {}

    /**
     * Parse a plain‑text chord progression into SongInput + ParsedChord list.
     * Format: bars separated by '|', chords within a bar separated by spaces.
     */
    public ParseResult parseProgressionText(String text, String title, String key, String timeSignature) {
        int beatsPerBar = 4;
        if (timeSignature.contains("/")) {
            beatsPerBar = Integer.parseInt(timeSignature.split("/")[0]);
        }

        List<String> barTexts = new ArrayList<>();
        for (String line : text.strip().split("\\n")) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            for (String b : line.split("\\|")) {
                String bt = b.strip();
                if (!bt.isEmpty()) barTexts.add(bt);
            }
        }

        List<ChordEntry> entries = new ArrayList<>();
        List<ParsedChord> parsed = new ArrayList<>();
        int barNum = 0;

        for (String barText : barTexts) {
            barNum++;
            String[] symbols = barText.split("\\s+");
            List<String> valid = new ArrayList<>();
            for (String s : symbols) {
                if (!NO_CHORD_RE.matcher(s).matches()) valid.add(s);
            }
            if (valid.isEmpty()) continue;

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
     * Parse a single chord symbol string into a ParsedChord.
     */
    public ParsedChord parseChordSymbol(String symbol, int bar, double beat, double durationBeats) {
        symbol = symbol.strip();
        if (symbol.isEmpty()) return null;
        if (NO_CHORD_RE.matcher(symbol).matches()) return null;

        Matcher m = CHORD_RE.matcher(symbol);
        if (!m.matches()) throw new IllegalArgumentException("Cannot parse chord symbol: '" + symbol + "'");

        String rootStr = m.group(1);
        String qualitySuffix = m.group(2) != null ? m.group(2) : "";
        String bassStr = m.group(3);

        int root = NoteUtils.parseNoteName(rootStr);
        if (root < 0) throw new IllegalArgumentException("Cannot parse root note: '" + rootStr + "'");

        Integer bass = bassStr != null ? boxedPc(NoteUtils.parseNoteName(bassStr)) : null;
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
     * Parse quality / extension suffix. Massive but faithful port of _parse_quality_and_tensions.
     */
    private QualityTensions parseQualityAndTensions(String suffix) {
        List<String> tensions = new ArrayList<>();
        if (suffix == null || suffix.isEmpty()) return new QualityTensions("maj", tensions);

        // Extract parenthesized tensions
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

        // Normalize unicode
        suffix = suffix.replace("\u0394", "maj7")
                       .replace("\u00B0", "dim")
                       .replace("\u2205", "min7b5")
                       .replace("\u00F8", "min7b5");

        String s = suffix.strip();
        String quality = null;

        // ── Pattern matching (order matters!) ──

        // minmaj7
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
        if (quality == null) quality = "maj";

        // remaining tensions
        Matcher rm = Pattern.compile("[#b\\u266F\\u266D]*\\d+").matcher(s);
        while (rm.find()) tensions.add(rm.group());
        tensions.addAll(parenTensions);

        // Deduplicate preserving order
        List<String> unique = new ArrayList<>(new LinkedHashSet<>(tensions));

        return new QualityTensions(quality, unique);
    }

    // ── regex helpers ──

    private static boolean matches(String s, String regex) {
        return Pattern.compile(regex).matcher(s).find();
    }

    private static boolean matchAny(String s, String... patterns) {
        for (String p : patterns) if (matches(s, p)) return true;
        return false;
    }

    private static String replaceFirst(String s, String regex, String replacement) {
        return Pattern.compile(regex).matcher(s).replaceFirst(replacement);
    }
}

