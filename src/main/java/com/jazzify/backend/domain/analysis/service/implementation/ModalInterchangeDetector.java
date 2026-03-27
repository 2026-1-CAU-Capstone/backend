package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.config.AnalysisConfigData;
import com.jazzify.backend.domain.analysis.config.AnalysisConfigData.*;
import com.jazzify.backend.domain.analysis.model.FunctionEntry;
import com.jazzify.backend.domain.analysis.model.ModalInterchangeInfo;
import com.jazzify.backend.domain.analysis.model.ModalInterchangeInfo.ModalInterchangeMatch;
import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import com.jazzify.backend.domain.analysis.util.NoteUtils.KeyInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.jazzify.backend.domain.analysis.util.NoteUtils.mod12;

/**
 * Layer 3: 모달 인터체인지 감지기.
 * 장조 키에서 다른 모드(에올리안, 도리안 등)의 코드를 빌려온 것을 감지한다.
 * 비다이어토닉이면서 세컨더리 도미넌트가 아닌 코드만 검사한다.
 */
@Component
@RequiredArgsConstructor
public class ModalInterchangeDetector {

    private final AnalysisConfigData configData;

    /**
     * 모달 인터체인지를 감지하여 modalInterchange 정보를 설정한다.
     *
     * 알고리즘:
     * 1) 장조 키에서만 동작 (단조는 건너뜀)
     * 2) 비다이어토닉 + 세컨더리 도미넌트가 아닌 코드만 대상
     * 3) 각 모드의 availableDegrees에서 (interval, quality) 쌍이 일치하는 것을 찾음
     * 4) commonBorrows에 있으면 isCommonBorrow=true로 표시
     * 5) 매치가 여러 개면 commonBorrow인 것을 우선 선택
     */
    public List<ParsedChord> detect(List<ParsedChord> chords, String key) {
        KeyInfo ki = NoteUtils.parseKey(key);
        if (!ki.isMajor()) return chords; // 장조에서만 감지

        int keyRoot = ki.root();
        Map<String, ModeInterchangeData> miConfig = configData.getModalInterchange();

        for (ParsedChord chord : chords) {
            // 비다이어토닉 + 세컨더리 도미넌트가 아닌 코드만 대상
            if (Boolean.TRUE.equals(chord.getIsDiatonic())) continue;
            if (chord.getSecondaryDominant() != null) continue;

            String nq = chord.getNormalizedQuality() != null ? chord.getNormalizedQuality() : chord.getQuality();
            int chordInterval = mod12(chord.getRoot() - keyRoot);

            // 모든 모드에서 (interval, quality) 일치 항목 수집
            List<ModalInterchangeMatch> matches = new ArrayList<>();
            for (var entry : miConfig.entrySet()) {
                String modeName = entry.getKey();
                ModeInterchangeData modeData = entry.getValue();
                for (DegreeInfo di : modeData.availableDegrees()) {
                    if (di.interval() == chordInterval && di.quality().equals(nq)) {
                        // commonBorrows에 해당 디그리가 있는지 확인
                        boolean isCommon = modeData.commonBorrows().stream()
                                .anyMatch(cb -> cb.degreeLabel().equals(di.degreeLabel()));
                        matches.add(ModalInterchangeMatch.builder()
                                .sourceMode(modeName)
                                .borrowedDegree(di.degreeLabel())
                                .isCommonBorrow(isCommon)
                                .build());
                    }
                }
            }

            if (!matches.isEmpty()) {
                // commonBorrow인 매치를 우선 선택, 없으면 첫 번째 선택
                ModalInterchangeMatch best = matches.stream()
                        .filter(ModalInterchangeMatch::isCommonBorrow)
                        .findFirst()
                        .orElse(matches.get(0));

                chord.setModalInterchange(ModalInterchangeInfo.builder()
                        .sourceMode(best.getSourceMode())
                        .borrowedDegree(best.getBorrowedDegree())
                        .isCommonBorrow(best.isCommonBorrow())
                        .allPossibleSources(matches)
                        .build());

                // 기능이 비어있으면 modal_interchange 기능 부여 (흔한 빌림: 0.7, 드문 빌림: 0.5)
                if (chord.getFunctions().isEmpty()) {
                    chord.setFunctions(List.of(new FunctionEntry("modal_interchange",
                            best.isCommonBorrow() ? 0.7 : 0.5,
                            "Borrowed from " + best.getSourceMode() + " mode (" + best.getBorrowedDegree() + ")")));
                }
            }
        }
        return chords;
    }
}
