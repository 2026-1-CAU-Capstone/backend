package com.jazzify.backend.domain.analysis.service.implementation;

import com.jazzify.backend.domain.analysis.model.GroupMembership;
import com.jazzify.backend.domain.analysis.model.ParsedChord;
import com.jazzify.backend.domain.analysis.util.NoteUtils;
import com.jazzify.backend.domain.analysis.util.NoteUtils.KeyInfo;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.jazzify.backend.domain.analysis.util.NoteUtils.mod12;
import static com.jazzify.backend.domain.analysis.util.NoteUtils.pcToNoteName;

/**
 * Layer 2: ii-V-I 진행 감지기.
 * 재즈의 핵심 진행인 ii-V-I과 그 변형(백도어, 트라이톤 대리, sus 딜레이 등)을 감지한다.
 * V(도미넌트) 코드를 기준으로 앞(ii)과 뒤(I)를 탐색하는 방식으로 동작한다.
 */
@Component
public class IiViDetector {

    // ── 코드 품질 분류 기준 ──
    private static final Set<String> DOMINANT_QUALITIES = Set.of("dom7", "dom7sus4", "aug7"); // V 후보
    private static final Set<String> II_MINOR_QUALITIES = Set.of("min7", "min", "min6");      // ii 후보 (장조)
    private static final Set<String> II_HALFDIM_QUALITIES = Set.of("min7b5", "dim");           // ii 후보 (단조)
    private static final Set<String> I_MAJOR_QUALITIES = Set.of("maj7", "maj", "maj6");        // I 후보 (장조)
    private static final Set<String> I_MINOR_QUALITIES = Set.of("min7", "min", "minmaj7", "min6"); // I 후보 (단조)

    public record IiViResult(List<ParsedChord> chords, List<Map<String, Object>> groups) {}

    /**
     * 코드 리스트에서 ii-V-I 진행과 변형을 감지한다.
     *
     * 알고리즘:
     * 1) 모든 도미넌트 품질 코드를 V 후보로 순회
     * 2) V의 완전5도 아래(= 완전4도 위)를 타겟 I 근음으로 계산
     * 3) V 다음 코드에서 I 찾기 (표준/마이너/iii 대리)
     * 4) V 이전 코드에서 ii 찾기 (타겟 I의 장2도 위)
     * 5) 찾지 못하면 변형 패턴 시도: 트라이톤 대리 V, 트라이톤 대리 ii, 백도어, sus 딜레이
     * 6) 감지된 그룹을 groups 리스트에 추가하고, 각 코드에 groupMemberships 기록
     */
    public IiViResult detect(List<ParsedChord> chords, String key) {
        KeyInfo ki = NoteUtils.parseKey(key);
        int keyRoot = ki.root();
        String keyMode = ki.mode();

        List<Map<String, Object>> groups = new ArrayList<>();
        int groupId = 0;
        Set<Integer> usedIndices = new HashSet<>();
        int n = chords.size();

        int[] majorScale = {0, 2, 4, 5, 7, 9, 11};
        int[] minorScale = {0, 2, 3, 5, 7, 8, 10};
        int[] scale = ki.isMinor() ? minorScale : majorScale;

        for (int vIdx = 0; vIdx < n; vIdx++) {
            ParsedChord vChord = chords.get(vIdx);
            String nq = nq(vChord);
            if (!DOMINANT_QUALITIES.contains(nq)) continue; // 도미넌트가 아니면 건너뜀

            int vRoot = vChord.getRoot();
            int targetRoot = mod12(vRoot - 7); // V의 완전5도 아래 = 타겟 I의 근음

            // ── sus4 딜레이 감지: V7sus4 → V7 연속이면 실제 V는 다음 코드 ──
            boolean susDelay = false;
            int actualVIdx = vIdx;
            if ("dom7sus4".equals(nq) && vIdx + 1 < n) {
                ParsedChord next = chords.get(vIdx + 1);
                if ("dom7".equals(nq(next)) && next.getRoot() == vRoot) {
                    susDelay = true;
                    actualVIdx = vIdx + 1;
                }
            }

            // ── I(토닉) 찾기: V 다음 코드 검사 ──
            int iIdx = actualVIdx + 1;
            ParsedChord iChord = null;
            String iRole = "I";
            String variant = "standard";
            boolean hasI = false;

            if (iIdx < n) {
                iChord = chords.get(iIdx);
                String iNq = nq(iChord);
                int iRoot = iChord.getRoot();
                // 표준 해결: 다음 코드 근음 == 타겟 근음
                if (iRoot == targetRoot) {
                    if (I_MAJOR_QUALITIES.contains(iNq)) { hasI = true; variant = "standard"; }
                    else if (I_MINOR_QUALITIES.contains(iNq)) { hasI = true; variant = "minor"; }
                // iii 대리: 타겟 I에서 장3도 위의 마이너 코드
                } else if (NoteUtils.interval(targetRoot, iRoot) == 4) {
                    if (II_MINOR_QUALITIES.contains(iNq)) { hasI = true; iRole = "I (iii substitute)"; }
                }
            }

            // ── ii(서브도미넌트) 찾기: V 이전 코드 검사 ──
            int iiIdx = vIdx - 1;
            ParsedChord iiChord = null;
            String iiRole = "ii";
            boolean hasIi = false;
            int expectedIiRoot = mod12(targetRoot + 2); // 타겟 I의 장2도 위 = ii의 근음

            if (iiIdx >= 0) {
                iiChord = chords.get(iiIdx);
                String iiNq = nq(iiChord);
                if (iiChord.getRoot() == expectedIiRoot) {
                    if (II_MINOR_QUALITIES.contains(iiNq)) hasIi = true;
                    else if (II_HALFDIM_QUALITIES.contains(iiNq)) { hasIi = true; variant = "minor"; }
                }
            }

            // ── 변형 1: 트라이톤 대리 V (V가 bII7로 대체된 경우) ──
            // V → I이 반음 하행(interval=11)이면 트라이톤 대리
            boolean isTritoneSubV = false;
            if (!hasI && iIdx < n) {
                ParsedChord iCheck = chords.get(iIdx);
                if (NoteUtils.interval(vRoot, iCheck.getRoot()) == 11) {
                    int actualTarget = iCheck.getRoot();
                    String iNq = nq(iCheck);
                    if (I_MAJOR_QUALITIES.contains(iNq) || I_MINOR_QUALITIES.contains(iNq)) {
                        hasI = true;
                        isTritoneSubV = true;
                        targetRoot = actualTarget;
                        variant = "tritone_sub_V";
                        int expectedIiTt = mod12(actualTarget + 2);
                        if (hasIi && iiChord.getRoot() != expectedIiTt) hasIi = false;
                        if (!hasIi && iiIdx >= 0) {
                            iiChord = chords.get(iiIdx);
                            if (iiChord.getRoot() == expectedIiTt && II_MINOR_QUALITIES.contains(nq(iiChord)))
                                hasIi = true;
                        }
                    }
                }
            }

            // ── 변형 2: 트라이톤 대리 ii ──
            if (hasI && !isTritoneSubV && !hasIi && iiIdx >= 0) {
                iiChord = chords.get(iiIdx);
                int expectedSubIiRoot = mod12(expectedIiRoot + 6);
                if (iiChord.getRoot() == expectedSubIiRoot && II_MINOR_QUALITIES.contains(nq(iiChord))) {
                    hasIi = true;
                    iiRole = "ii (tritone sub)";
                    variant = "tritone_sub_ii_V";
                }
            }

            // ── 변형 3: 백도어 (iv → bVII7 → I) ──
            // V가 다이어토닉 V7이 아니고, V+장2도 위가 I이면 백도어 진행
            boolean isDiatonicV = Boolean.TRUE.equals(vChord.getIsDiatonic())
                    && NoteUtils.interval(keyRoot, vRoot) == 7
                    && DOMINANT_QUALITIES.contains(nq);
            if (!hasI && !isDiatonicV && iIdx < n) {
                ParsedChord iBd = chords.get(iIdx);
                String iNq = nq(iBd);
                int bdTarget = mod12(vRoot + 2);
                if (iBd.getRoot() == bdTarget && (I_MAJOR_QUALITIES.contains(iNq) || I_MINOR_QUALITIES.contains(iNq))) {
                    targetRoot = bdTarget;
                    hasI = true;
                    hasIi = false;
                    iiRole = "ii";
                    if (iiIdx >= 0) {
                        iiChord = chords.get(iiIdx);
                        int expectedIv = mod12(bdTarget + 5);
                        if (iiChord.getRoot() == expectedIv && II_MINOR_QUALITIES.contains(nq(iiChord))) {
                            hasIi = true;
                            iiRole = "iv (backdoor)";
                        }
                    }
                    variant = "backdoor";
                    iChord = iBd;
                }
            }

            if (!hasI && !hasIi) continue; // ii도 I도 없으면 ii-V-I 아님

            // ── 그룹 생성 및 멤버 등록 ──
            groupId++;
            String targetKeyName = pcToNoteName(targetRoot);
            // 타겟 근음이 원래 키 음계에 속하는지 확인 (다이어토닉 타겟 여부)
            Set<Integer> scalePcs = new HashSet<>();
            for (int sv : scale) scalePcs.add(mod12(keyRoot + sv));
            boolean isDiatonicTarget = scalePcs.contains(targetRoot);

            if (!hasI) variant = "incomplete";
            if (susDelay) {
                variant = !"standard".equals(variant) ? variant + "_with_sus_delay" : "standard_with_sus_delay";
            }

            List<Map<String, Object>> members = new ArrayList<>();

            // ii member
            if (hasIi && iiChord != null) {
                members.add(memberMap(iiChord, iiRole));
                if (!usedIndices.contains(iiIdx)) {
                    iiChord.getGroupMemberships().add(gm(groupId, iiRole, variant));
                    usedIndices.add(iiIdx);
                }
            }

            // V member
            String vRole = "V";
            if (isTritoneSubV) vRole = "V (tritone sub bII7)";
            else if ("backdoor".equals(variant)) vRole = "V (backdoor bVII7)";
            members.add(memberMap(vChord, vRole));
            if (!usedIndices.contains(vIdx)) {
                vChord.getGroupMemberships().add(gm(groupId, vRole, variant));
                usedIndices.add(vIdx);
            }

            // sus delay member
            if (susDelay) {
                ParsedChord susChord = chords.get(actualVIdx);
                members.add(memberMap(susChord, "V (resolved from sus4)"));
                susChord.getGroupMemberships().add(gm(groupId, "V (resolved from sus4)", variant));
            }

            // I member
            if (hasI && iChord != null) {
                members.add(memberMap(iChord, iRole));
                if (!usedIndices.contains(iIdx)) {
                    iChord.getGroupMemberships().add(gm(groupId, iRole, variant));
                    usedIndices.add(iIdx);
                }
            }

            // Notes
            List<String> notes = new ArrayList<>();
            if (isDiatonicTarget) notes.add("Diatonic ii-V-I in " + targetKeyName);
            else notes.add("ii-V-I targeting " + targetKeyName);
            if (isTritoneSubV) notes.add(vChord.getOriginalSymbol() + " is tritone sub of " + pcToNoteName(mod12(vRoot + 6)) + "7");
            if ("backdoor".equals(variant)) notes.add("Backdoor progression (iv-bVII7-I)");

            Map<String, Object> group = new LinkedHashMap<>();
            group.put("group_id", groupId);
            group.put("group_type", "ii-V-I");
            group.put("variant", variant);
            group.put("target_key", targetKeyName);
            group.put("is_diatonic_target", isDiatonicTarget);
            group.put("members", members);
            group.put("notes", String.join(". ", notes));
            groups.add(group);
        }

        return new IiViResult(chords, groups);
    }

    // ── helpers ──
    /** normalizedQuality를 안전하게 가져오는 헬퍼 */
    private static String nq(ParsedChord c) {
        return c.getNormalizedQuality() != null ? c.getNormalizedQuality() : c.getQuality();
    }

    /** 코드 정보를 그룹 멤버 Map으로 변환 */
    private static Map<String, Object> memberMap(ParsedChord c, String role) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bar", c.getBar());
        m.put("beat", c.getBeat());
        m.put("symbol", c.getOriginalSymbol());
        m.put("role", role);
        m.put("is_diatonic", c.getIsDiatonic());
        return m;
    }

    /** GroupMembership 객체를 생성하는 팩토리 메서드 */
    private static GroupMembership gm(int groupId, String role, String variant) {
        return GroupMembership.builder()
                .groupId(groupId).groupType("ii-V-I").role(role).variant(variant)
                .build();
    }
}

