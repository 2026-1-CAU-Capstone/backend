package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import com.jazzify.backend.domain.analysis.util.NoteUtils.KeyInfo;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.jazzify.backend.domain.analysis.util.NoteUtils.mod12;

/**
 * Layer 3: 모드 세그먼트 감지기.
 * 4마디 슬라이딩 윈도우로 곡의 각 구간이 어떤 모드(이오니안, 도리안, 리디안 등) 색채를 띠는지 감지한다.
 * 윈도우 내 코드 구성음과 각 모드 음계의 일치도를 점수화하여 가장 높은 모드를 선택한다.
 */
@Component
public class ModeSegmentDetector {

    /** 7개 교회선법의 음계 (근음으로부터의 반음 간격) */
    private static final Map<String, int[]> MODE_SCALES = Map.of(
            "ionian", new int[]{0, 2, 4, 5, 7, 9, 11},
            "dorian", new int[]{0, 2, 3, 5, 7, 9, 10},
            "phrygian", new int[]{0, 1, 3, 5, 7, 8, 10},
            "lydian", new int[]{0, 2, 4, 6, 7, 9, 11},
            "mixolydian", new int[]{0, 2, 4, 5, 7, 9, 10},
            "aeolian", new int[]{0, 2, 3, 5, 7, 8, 10},
            "locrian", new int[]{0, 1, 3, 5, 6, 8, 10}
    );

    private static final int WINDOW_BARS = 4;      // 슬라이딩 윈도우 크기 (마디)
    private static final double THRESHOLD = 0.55;   // 모드 선택 최소 점수 임계값

    /**
     * 각 코드에 modeSegment(해당 구간의 모드)를 할당한다.
     *
     * 알고리즘:
     * 1) 코드를 마디별로 그룹화
     * 2) 4마디 슬라이딩 윈도우로 순회하며:
     *    a) 윈도우 내 모든 코드의 피치클래스(구성음)를 수집
     *    b) 로컬 키 근음 결정 (전조/ii-V-I 정보 활용)
     *    c) 각 모드의 음계와 일치도를 스코어링
     *    d) 가장 높은 점수의 모드가 임계값(0.55) 이상이면 할당
     * 3) 아직 모드가 없는 코드에는 기본 모드(장조→ionian, 단조→aeolian) 할당
     */
    public List<ParsedChord> detect(List<ParsedChord> chords, String key) {
        if (chords.isEmpty()) return chords;
        KeyInfo ki = NoteUtils.parseKey(key);
        int keyRoot = ki.root();
        String defaultMode = ki.isMinor() ? "aeolian" : "ionian";

        int maxBar = chords.stream().mapToInt(ParsedChord::getBar).max().orElse(1);

        // 코드 인덱스를 마디별로 그룹화
        Map<Integer, List<Integer>> barChords = new HashMap<>();
        for (int idx = 0; idx < chords.size(); idx++) {
            barChords.computeIfAbsent(chords.get(idx).getBar(), k -> new ArrayList<>()).add(idx);
        }

        // 4마디 슬라이딩 윈도우로 순회
        for (int startBar = 1; startBar <= maxBar; startBar++) {
            int endBar = Math.min(startBar + WINDOW_BARS - 1, maxBar);

            // 윈도우 내 모든 코드의 피치클래스 수집
            Set<Integer> windowPcs = new HashSet<>();
            List<Integer> windowIndices = new ArrayList<>();
            List<ParsedChord> windowChords = new ArrayList<>();

            for (int bar = startBar; bar <= endBar; bar++) {
                for (int idx : barChords.getOrDefault(bar, List.of())) {
                    windowPcs.addAll(getPitchClasses(chords.get(idx)));
                    windowIndices.add(idx);
                    windowChords.add(chords.get(idx));
                }
            }
            if (windowPcs.isEmpty()) continue;

            // 로컬 키 근음 결정 (전조나 ii-V-I의 I 정보 활용)
            int localRoot = determineLocalKeyRoot(windowChords, keyRoot);

            // 각 모드별 스코어링: 로컬 근음 기준
            String bestMode = null;
            double bestScore = -999.0;
            for (var entry : MODE_SCALES.entrySet()) {
                double score = scoreMode(windowPcs, localRoot, entry.getValue());
                if (score > bestScore) { bestScore = score; bestMode = entry.getKey(); }
            }

            // 로컬 근음이 곡 키와 다르면, 곡 키 기준으로도 스코어링하여 더 높은 쪽 선택
            if (localRoot != keyRoot) {
                for (var entry : MODE_SCALES.entrySet()) {
                    double score = scoreMode(windowPcs, keyRoot, entry.getValue());
                    if (score > bestScore) { bestScore = score; bestMode = entry.getKey(); }
                }
            }

            // 임계값 이상이면 윈도우 내 (아직 모드 미지정) 코드에 할당
            if (bestMode != null && bestScore >= THRESHOLD) {
                for (int idx : windowIndices) {
                    if (chords.get(idx).getModeSegment() == null) {
                        chords.get(idx).setModeSegment(bestMode);
                    }
                }
            }
        }

        // 아직 모드가 없는 코드에 기본 모드 할당
        for (ParsedChord c : chords) {
            if (c.getModeSegment() == null) c.setModeSegment(defaultMode);
        }
        return chords;
    }

    // ── helpers ──

    /** 코드의 구성음(근음 + 품질 인터벌)을 피치클래스 집합으로 반환 */
    private Set<Integer> getPitchClasses(ParsedChord chord) {
        String q = chord.getNormalizedQuality() != null ? chord.getNormalizedQuality() : chord.getQuality();
        int[] intervals = DiatonicClassifier.QUALITY_INTERVALS.getOrDefault(q, new int[]{0, 4, 7});
        Set<Integer> pcs = new HashSet<>();
        for (int iv : intervals) pcs.add(mod12(chord.getRoot() + iv));
        return pcs;
    }

    /**
     * 피치클래스 집합과 모드 음계의 일치도를 점수화한다.
     * overlap(일치) - outside(불일치)×2 로 계산, 불일치에 2배 페널티.
     */
    private double scoreMode(Set<Integer> pitchClasses, int root, int[] scale) {
        if (pitchClasses.isEmpty()) return 0.0;
        Set<Integer> scalePcs = new HashSet<>();
        for (int s : scale) scalePcs.add(mod12(root + s));

        int overlap = 0, outside = 0;
        for (int pc : pitchClasses) {
            if (scalePcs.contains(pc)) overlap++;
            else outside++;
        }
        return (overlap - outside * 2.0) / Math.max(pitchClasses.size(), 1);
    }

    /**
     * 윈도우 내 코드에서 로컬 키 근음을 결정한다.
     * 뒤에서부터 탐색하여: 전조 정보가 있으면 그 키의 근음, ii-V-I의 I이면 그 근음 사용.
     * 아무 정보도 없으면 곡의 키 근음을 그대로 사용.
     */
    private int determineLocalKeyRoot(List<ParsedChord> window, int songKeyRoot) {
        for (int i = window.size() - 1; i >= 0; i--) {
            ParsedChord c = window.get(i);
            if (c.getTonicization() != null) {
                String tk = c.getTonicization().getTemporaryKey();
                if (tk != null) {
                    int pc = NoteUtils.parseNoteName(tk);
                    if (pc >= 0) return pc;
                }
            }
            for (var gm : c.getGroupMemberships()) {
                if ("ii-V-I".equals(gm.getGroupType())
                        && ("I".equals(gm.getRole()) || "I (iii substitute)".equals(gm.getRole()))) {
                    return c.getRoot();
                }
            }
        }
        return songKeyRoot;
    }
}
