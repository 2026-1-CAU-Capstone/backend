package com.jazzify.backend.domain.analysis.config;

import com.jazzify.backend.domain.analysis.model.FunctionEntry;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 화성 분석에 필요한 정적 설정 데이터.
 * 스케일 디그리 → 화성 기능(T/SD/D) 매핑 테이블과 모달 인터체인지 참조 데이터를 제공한다.
 * Python config_data.py에서 포팅됨.
 */
@Component
public class AnalysisConfigData {

    // ─────────────────── FUNCTION MAP ───────────────────
    // 3단계 맵 구조: (키 섹션) → (스케일 디그리) → (화성 기능 목록)
    // 키 섹션: "major_key", "minor_key", "chromatic_degrees"

    /** 스케일 디그리별 화성 기능 매핑 테이블을 반환한다 */
    public Map<String, Map<String, List<FunctionEntry>>> getFunctionMap() {
        return FUNCTION_MAP;
    }

    private static final Map<String, Map<String, List<FunctionEntry>>> FUNCTION_MAP = Map.of(
            // 장조에서의 다이어토닉 코드 기능 매핑
            "major_key", Map.ofEntries(
                    Map.entry("I", List.of(fe("T", 1.0))),
                    Map.entry("ii", List.of(fe("SD", 1.0))),
                    Map.entry("iii", List.of(fe("T", 0.6, "Tonic substitute (shares 2 tones with I)"),
                                              fe("D_mediant", 0.3, "Dominant mediant in some contexts"))),
                    Map.entry("IV", List.of(fe("SD", 1.0))),
                    Map.entry("V", List.of(fe("D", 1.0))),
                    Map.entry("vi", List.of(fe("T", 0.7, "Tonic substitute (relative minor)"),
                                             fe("SD", 0.3, "Subdominant function in some progressions"))),
                    Map.entry("vii", List.of(fe("D", 0.9, "Leading tone chord, dominant function"))),
                    Map.entry("viio", List.of(fe("D", 0.9, "Leading tone chord, dominant function")))
            ),
            // 반음계적(비다이어토닉) 코드의 기능 매핑 – 장/단조 공통
            "chromatic_degrees", Map.ofEntries(
                    Map.entry("bII", List.of(fe("SD", 0.6, "Neapolitan / Phrygian"),
                                              fe("D_substitute", 0.4, "Tritone sub of V when dom7"))),
                    Map.entry("bii", List.of(fe("SD", 0.6, "Neapolitan area (minor quality)"))),
                    Map.entry("II", List.of(fe("D", 0.7, "Secondary dominant area (V/V)"),
                                             fe("SD", 0.3, "Lydian II"))),
                    Map.entry("III", List.of(fe("D", 0.5, "Secondary dominant area (V/vi) when dom7"),
                                              fe("T", 0.4, "Major mediant, tonic substitute"))),
                    Map.entry("bIII", List.of(fe("T", 0.5, "Modal interchange from minor"),
                                               fe("SD", 0.3))),
                    Map.entry("biii", List.of(fe("T", 0.4, "Modal interchange from minor (minor quality)"))),
                    Map.entry("#iv", List.of(fe("D", 0.7, "Passing chromatic to V"))),
                    Map.entry("#IVo", List.of(fe("D", 0.8, "Passing diminished with dominant function"))),
                    Map.entry("#IV", List.of(fe("D", 0.5, "Lydian II or passing"))),
                    Map.entry("bV", List.of(fe("D_substitute", 0.7, "Tritone sub area"))),
                    Map.entry("VI", List.of(fe("D", 0.7, "Secondary dominant area (V/ii) when dom7"),
                                             fe("SD", 0.3))),
                    Map.entry("bVI", List.of(fe("SD", 0.7, "Modal interchange from minor"),
                                              fe("T", 0.3, "Deceptive resolution target"))),
                    Map.entry("VII", List.of(fe("D", 0.7, "Secondary dominant area (V/iii) when dom7"),
                                              fe("T", 0.2, "Leading tone area"))),
                    Map.entry("bVII", List.of(fe("SD", 0.6, "Modal interchange (mixolydian/aeolian)"),
                                               fe("D", 0.3, "Backdoor dominant approach"))),
                    Map.entry("bvii", List.of(fe("SD", 0.5, "Modal interchange (minor quality)"))),
                    Map.entry("i", List.of(fe("T", 0.8, "Parallel minor tonic (modal interchange)"))),
                    Map.entry("v", List.of(fe("D", 0.4, "Minor v, weaker dominant"),
                                            fe("T", 0.3)))
            ),
            // 단조에서의 다이어토닉 코드 기능 매핑
            "minor_key", Map.ofEntries(
                    Map.entry("i", List.of(fe("T", 1.0))),
                    Map.entry("ii", List.of(fe("SD", 1.0))),
                    Map.entry("iio", List.of(fe("SD", 1.0))),
                    Map.entry("bIII", List.of(fe("T", 0.6, "Relative major"),
                                               fe("SD", 0.3))),
                    Map.entry("III", List.of(fe("T", 0.5, "Major III (major quality)"))),
                    Map.entry("iv", List.of(fe("SD", 1.0))),
                    Map.entry("v", List.of(fe("D", 0.6, "Minor dominant (weaker)"),
                                            fe("T", 0.2))),
                    Map.entry("V", List.of(fe("D", 1.0, "Harmonic minor dominant"))),
                    Map.entry("bVI", List.of(fe("SD", 0.8), fe("T", 0.2))),
                    Map.entry("VI", List.of(fe("SD", 0.7, "Raised 6th degree"))),
                    Map.entry("bVII", List.of(fe("SD", 0.5), fe("D", 0.4, "Subtonic dominant"))),
                    Map.entry("VII", List.of(fe("D", 0.8, "Leading tone (harmonic minor)"))),
                    Map.entry("viio", List.of(fe("D", 0.9, "Leading tone diminished"))),
                    Map.entry("bII", List.of(fe("SD", 0.6, "Neapolitan"))),
                    Map.entry("bii", List.of(fe("SD", 0.5))),
                    Map.entry("II", List.of(fe("D", 0.6, "Secondary dominant area")))
            )
    );

    // ─────────────────── MODAL INTERCHANGE ───────────────────
    // 각 모드(에올리안, 도리안 등)에서 빌려올 수 있는 코드 정보

    /** 모드별 사용 가능 코드 정보: interval(근음으로부터 반음 간격), quality(코드 품질), degreeLabel(디그리 표기) */
    public record DegreeInfo(int interval, String quality, String degreeLabel) {}
    /** 흔히 빌려오는 코드 정보: degreeLabel(디그리 표기), note(설명) */
    public record CommonBorrow(String degreeLabel, String note) {}
    /** 모드 인터체인지 데이터: 모드 이름, 사용 가능 코드 목록, 흔한 차용 코드 목록 */
    public record ModeInterchangeData(String name, List<DegreeInfo> availableDegrees, List<CommonBorrow> commonBorrows) {}

    /** 모드별 인터체인지 데이터 테이블을 반환한다 (aeolian, dorian, phrygian, lydian, mixolydian) */
    public Map<String, ModeInterchangeData> getModalInterchange() {
        return MODAL_INTERCHANGE;
    }

    private static final Map<String, ModeInterchangeData> MODAL_INTERCHANGE = Map.of(
            "aeolian", new ModeInterchangeData("Natural Minor (Aeolian)",
                    List.of(di(0, "min7", "i"), di(2, "min7b5", "ii°"), di(3, "maj7", "bIII"),
                            di(5, "min7", "iv"), di(7, "min7", "v"), di(8, "maj7", "bVI"),
                            di(10, "dom7", "bVII")),
                    List.of(cb("bVII", "Very common in pop/rock and jazz"),
                            cb("bVI", "Common dramatic chord"),
                            cb("iv", "Minor subdominant, very common"),
                            cb("bIII", "Common in rock"),
                            cb("v", "Less common, modal flavor"))),
            "dorian", new ModeInterchangeData("Dorian",
                    List.of(di(0, "min7", "i"), di(2, "min7", "ii"), di(3, "maj7", "bIII"),
                            di(5, "dom7", "IV7"), di(7, "min7", "v"), di(9, "min7b5", "vi°"),
                            di(10, "maj7", "bVII")),
                    List.of(cb("IV7", "Dominant quality IV, bluesy"),
                            cb("bVII", "Major bVII"))),
            "phrygian", new ModeInterchangeData("Phrygian",
                    List.of(di(0, "min7", "i"), di(1, "maj7", "bII"), di(3, "dom7", "bIII7"),
                            di(5, "min7", "iv"), di(7, "min7b5", "v°"), di(8, "maj7", "bVI"),
                            di(10, "min7", "bvii")),
                    List.of(cb("bII", "Phrygian / Neapolitan chord"))),
            "lydian", new ModeInterchangeData("Lydian",
                    List.of(di(0, "maj7", "I"), di(2, "dom7", "II7"), di(4, "min7", "iii"),
                            di(6, "min7b5", "#iv°"), di(7, "maj7", "V"), di(9, "min7", "vi"),
                            di(11, "min7", "vii")),
                    List.of(cb("II7", "Lydian dominant II"),
                            cb("#iv°", "Sharp four diminished"))),
            "mixolydian", new ModeInterchangeData("Mixolydian",
                    List.of(di(0, "dom7", "I7"), di(2, "min7", "ii"), di(4, "min7b5", "iii°"),
                            di(5, "maj7", "IV"), di(7, "min7", "v"), di(9, "min7", "vi"),
                            di(10, "maj7", "bVII")),
                    List.of(cb("bVII", "Major bVII from mixolydian"),
                            cb("I7", "Dominant I instead of maj7")))
    );

    // ── 팩토리 헬퍼: 설정 데이터 초기화 시 보일러플레이트를 줄이기 위한 축약 메서드 ──
    private static FunctionEntry fe(String func, double conf) {
        return new FunctionEntry(func, conf);
    }
    private static FunctionEntry fe(String func, double conf, String note) {
        return new FunctionEntry(func, conf, note);
    }
    private static DegreeInfo di(int interval, String quality, String degreeLabel) {
        return new DegreeInfo(interval, quality, degreeLabel);
    }
    private static CommonBorrow cb(String degreeLabel, String note) {
        return new CommonBorrow(degreeLabel, note);
    }
}
