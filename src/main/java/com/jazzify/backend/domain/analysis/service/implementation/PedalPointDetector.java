package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.model.PedalInfo;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import com.jazzify.backend.domain.analysis.util.NoteUtils.KeyInfo;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.jazzify.backend.domain.analysis.util.NoteUtils.mod12;
import static com.jazzify.backend.domain.analysis.util.NoteUtils.pcToNoteName;

/**
 * Layer 2: 페달 포인트 감지기.
 * 베이스 음이 여러 마디에 걸쳐 같은 음으로 지속되는 페달 포인트를 감지한다.
 * 최소 2마디 이상 + 2개 이상 코드가 동일 베이스를 공유해야 페달로 인정한다.
 */
@Component
public class PedalPointDetector {

    /** 페달로 인정하기 위한 최소 마디 수 */
    private static final int MIN_DURATION_BARS = 2;

    /**
     * 페달 포인트를 감지하여 해당 구간의 모든 코드에 PedalInfo를 설정한다.
     *
     * 알고리즘:
     * 1) 유효 베이스(슬래시 코드면 bass, 아니면 root)가 같은 연속 구간을 찾는다
     * 2) 구간이 2마디 이상이고 코드 2개 이상이면 페달로 인정
     * 3) 베이스와 키 근음의 간격으로 페달 유형 분류 (tonic/dominant/subdominant/기타)
     */
    public List<ParsedChord> detect(List<ParsedChord> chords, String key) {
        if (chords.isEmpty()) return chords;
        KeyInfo ki = NoteUtils.parseKey(key);
        int keyRoot = ki.root();
        int n = chords.size();
        int i = 0;

        // 같은 베이스가 연속되는 구간을 찾아 순회
        while (i < n) {
            int bass = effectiveBass(chords.get(i));
            int startIdx = i;
            int startBar = chords.get(i).getBar();
            int j = i + 1;

            // 동일 베이스가 계속되는 구간의 끝을 찾음
            while (j < n && effectiveBass(chords.get(j)) == bass) j++;

            int endIdx = j - 1;
            int endBar = chords.get(endIdx).getBar();
            int spanBars = endBar - startBar + 1;

            // 2마디 이상 + 2개 이상 코드이면 페달 포인트로 인정
            if (spanBars >= MIN_DURATION_BARS && endIdx - startIdx >= 1) {
                // 베이스와 키 근음의 간격으로 페달 유형 판별
                int bassInterval = mod12(bass - keyRoot);
                String pedalType;
                if (bassInterval == 0) pedalType = "tonic";           // 으뜸음 페달
                else if (bassInterval == 7) pedalType = "dominant";   // 도미넌트 페달
                else if (bassInterval == 5) pedalType = "subdominant"; // 서브도미넌트 페달
                else pedalType = "on " + pcToNoteName(bass);           // 기타

                // 구간 내 모든 코드에 PedalInfo 설정
                for (int k = startIdx; k <= endIdx; k++) {
                    chords.get(k).setPedalInfo(PedalInfo.builder()
                            .pedalNote(bass)
                            .pedalNoteName(pcToNoteName(bass))
                            .pedalType(pedalType)
                            .isOverPedal(true)
                            .pedalStartBar(startBar)
                            .pedalEndBar(endBar)
                            .build());
                }
            }
            i = j;
        }
        return chords;
    }

    /** 유효 베이스 음을 구한다: 슬래시 코드면 bass, 아니면 root */
    private int effectiveBass(ParsedChord c) {
        return c.getBass() != null ? c.getBass() : c.getRoot();
    }
}
