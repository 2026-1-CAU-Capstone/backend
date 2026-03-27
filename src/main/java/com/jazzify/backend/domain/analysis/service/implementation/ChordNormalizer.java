package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.ParsedChord;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Layer 1: 코드 품질 정규화기.
 * 텐션을 무시하고 핵심 품질(core quality)만 추출하여 이후 패턴 매칭에 사용한다.
 * 예: "dom7"(7th, 9th, 13th 모두) → 동일한 "dom7"로 정규화
 */
@Component
public class ChordNormalizer {

    /** 지원하는 모든 코드 품질 → 핵심 품질 매핑 테이블 (총 18종) */
    private static final Map<String, String> CORE_QUALITY_MAP = Map.ofEntries(
            Map.entry("maj7", "maj7"), Map.entry("maj", "maj"),
            Map.entry("min7", "min7"), Map.entry("min", "min"),
            Map.entry("dom7", "dom7"), Map.entry("min7b5", "min7b5"),
            Map.entry("dim7", "dim7"), Map.entry("dim", "dim"),
            Map.entry("aug", "aug"), Map.entry("aug7", "aug7"),
            Map.entry("augmaj7", "augmaj7"), Map.entry("sus4", "sus4"),
            Map.entry("sus2", "sus2"), Map.entry("dom7sus4", "dom7sus4"),
            Map.entry("min6", "min6"), Map.entry("maj6", "maj6"),
            Map.entry("minmaj7", "minmaj7"), Map.entry("power", "power")
    );

    /**
     * 모든 코드의 quality를 normalizedQuality 필드에 정규화하여 기록한다.
     * 알려진 품질이면 매핑된 핵심 품질을, 미지의 품질이면 원래 값을 그대로 사용한다.
     */
    public List<ParsedChord> normalize(List<ParsedChord> chords) {
        for (ParsedChord c : chords) {
            c.setNormalizedQuality(CORE_QUALITY_MAP.getOrDefault(c.getQuality(), c.getQuality()));
        }
        return chords;
    }
}
