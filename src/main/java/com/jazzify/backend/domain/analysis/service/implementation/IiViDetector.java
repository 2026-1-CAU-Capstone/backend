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
 * Layer 2‑4: ii‑V‑I Detector.
 * Detects ii‑V‑I progressions and variants (backdoor, tritone sub, sus delay, etc.).
 */
@Component
public class IiViDetector {

    private static final Set<String> DOMINANT_QUALITIES = Set.of("dom7", "dom7sus4", "aug7");
    private static final Set<String> II_MINOR_QUALITIES = Set.of("min7", "min", "min6");
    private static final Set<String> II_HALFDIM_QUALITIES = Set.of("min7b5", "dim");
    private static final Set<String> I_MAJOR_QUALITIES = Set.of("maj7", "maj", "maj6");
    private static final Set<String> I_MINOR_QUALITIES = Set.of("min7", "min", "minmaj7", "min6");

    public record IiViResult(List<ParsedChord> chords, List<Map<String, Object>> groups) {}

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
            if (!DOMINANT_QUALITIES.contains(nq)) continue;

            int vRoot = vChord.getRoot();
            int targetRoot = mod12(vRoot - 7);

            // sus4 delay
            boolean susDelay = false;
            int actualVIdx = vIdx;
            if ("dom7sus4".equals(nq) && vIdx + 1 < n) {
                ParsedChord next = chords.get(vIdx + 1);
                if ("dom7".equals(nq(next)) && next.getRoot() == vRoot) {
                    susDelay = true;
                    actualVIdx = vIdx + 1;
                }
            }

            // Look for I after V
            int iIdx = actualVIdx + 1;
            ParsedChord iChord = null;
            String iRole = "I";
            String variant = "standard";
            boolean hasI = false;

            if (iIdx < n) {
                iChord = chords.get(iIdx);
                String iNq = nq(iChord);
                int iRoot = iChord.getRoot();
                if (iRoot == targetRoot) {
                    if (I_MAJOR_QUALITIES.contains(iNq)) { hasI = true; variant = "standard"; }
                    else if (I_MINOR_QUALITIES.contains(iNq)) { hasI = true; variant = "minor"; }
                } else if (NoteUtils.interval(targetRoot, iRoot) == 4) {
                    if (II_MINOR_QUALITIES.contains(iNq)) { hasI = true; iRole = "I (iii substitute)"; }
                }
            }

            // Look for ii before V
            int iiIdx = vIdx - 1;
            ParsedChord iiChord = null;
            String iiRole = "ii";
            boolean hasIi = false;
            int expectedIiRoot = mod12(targetRoot + 2);

            if (iiIdx >= 0) {
                iiChord = chords.get(iiIdx);
                String iiNq = nq(iiChord);
                if (iiChord.getRoot() == expectedIiRoot) {
                    if (II_MINOR_QUALITIES.contains(iiNq)) hasIi = true;
                    else if (II_HALFDIM_QUALITIES.contains(iiNq)) { hasIi = true; variant = "minor"; }
                }
            }

            // Tritone sub on V
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

            // Tritone sub on ii
            if (hasI && !isTritoneSubV && !hasIi && iiIdx >= 0) {
                iiChord = chords.get(iiIdx);
                int expectedSubIiRoot = mod12(expectedIiRoot + 6);
                if (iiChord.getRoot() == expectedSubIiRoot && II_MINOR_QUALITIES.contains(nq(iiChord))) {
                    hasIi = true;
                    iiRole = "ii (tritone sub)";
                    variant = "tritone_sub_ii_V";
                }
            }

            // Backdoor: iv → bVII7 → I
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

            if (!hasI && !hasIi) continue;

            groupId++;
            String targetKeyName = pcToNoteName(targetRoot);
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
    private static String nq(ParsedChord c) {
        return c.getNormalizedQuality() != null ? c.getNormalizedQuality() : c.getQuality();
    }

    private static Map<String, Object> memberMap(ParsedChord c, String role) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("bar", c.getBar());
        m.put("beat", c.getBeat());
        m.put("symbol", c.getOriginalSymbol());
        m.put("role", role);
        m.put("is_diatonic", c.getIsDiatonic());
        return m;
    }

    private static GroupMembership gm(int groupId, String role, String variant) {
        return GroupMembership.builder()
                .groupId(groupId).groupType("ii-V-I").role(role).variant(variant)
                .build();
    }
}

