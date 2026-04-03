# 📦 데이터 모델 상세 설명

> 이 문서는 `analysis/model/` 패키지에 있는 모든 데이터 모델을 설명합니다.
> 이 모델들은 데이터베이스(DB)에 저장되지 않고, **분석 과정에서만 메모리에 존재**하는 내부 모델입니다.

---

## 목차

1. [ChordEntry – 입력 코드 항목](#1-chordentry--입력-코드-항목)
2. [SongInput – 곡 입력 정보](#2-songinput--곡-입력-정보)
3. [ParsedChord – ⭐ 핵심 분석 객체](#3-parsedchord---핵심-분석-객체)
4. [FunctionEntry – 화성 기능](#4-functionentry--화성-기능)
5. [GroupMembership – 그룹 소속 정보](#5-groupmembership--그룹-소속-정보)
6. [SecondaryDominantInfo – 세컨더리 도미넌트](#6-secondarydominantinfo--세컨더리-도미넌트)
7. [ChromaticApproachInfo – 반음계적 접근](#7-chromaticapproachinfo--반음계적-접근)
8. [DeceptiveResolutionInfo – 기만 종지](#8-deceptiveresolutioninfo--기만-종지)
9. [PedalInfo – 페달 포인트](#9-pedalinfo--페달-포인트)
10. [ModalInterchangeInfo – 모달 인터체인지](#10-modalinterchangeinfo--모달-인터체인지)
11. [TonicizationInfo – 조성화/전조](#11-tonicizationinfo--조성화전조)
12. [AmbiguityFlag – 모호성 플래그](#12-ambiguityflag--모호성-플래그)

---

## 1. ChordEntry – 입력 코드 항목

```java
public record ChordEntry(
    int bar,              // 몇 번째 마디인지 (1부터 시작)
    double beat,          // 마디 안에서 몇 번째 박인지 (1.0부터 시작)
    String symbol,        // 코드 기호 ("Dm7", "G7", "Cmaj7" 등)
    double durationBeats  // 이 코드가 몇 박 동안 울리는지
)
```

> 💡 **비유:** 악보에서 "3번째 마디, 1박에 Dm7이 2박 동안 연주됨"과 같은 정보를 담고 있습니다.

**예시:**
```
| Dm7  G7 | Cmaj7 |
```
- `Dm7` → `ChordEntry(bar=1, beat=1.0, symbol="Dm7", durationBeats=2.0)`
- `G7` → `ChordEntry(bar=1, beat=3.0, symbol="G7", durationBeats=2.0)`
- `Cmaj7` → `ChordEntry(bar=2, beat=1.0, symbol="Cmaj7", durationBeats=4.0)`

---

## 2. SongInput – 곡 입력 정보

```java
public record SongInput(
    String title,                  // 곡 제목
    String key,                    // 곡의 키 (예: "C", "Bbm")
    String timeSignature,          // 박자 (예: "4/4", "3/4")
    List<ChordEntry> chords,       // 파싱된 코드 목록
    Integer tempo                  // 템포 (BPM, null 가능)
)
```

> 곡 전체의 메타 정보 + 파싱된 코드 목록을 묶어 놓은 껍데기입니다.

---

## 3. ParsedChord – ⭐ 핵심 분석 객체

**이 클래스가 전체 분석 엔진의 핵심입니다!**

`ParsedChord`는 **분석 파이프라인의 각 단계를 거치면서 점진적으로 필드가 채워지는** 가변(mutable) 객체입니다.
마치 의사가 환자 차트에 검사 결과를 하나씩 기록하는 것과 같습니다.

### 구조 한 눈에 보기

```
ParsedChord
├── [파싱 결과] ───── Phase 1에서 채워짐
│   ├── originalSymbol    "Dm7"
│   ├── root              2  (D = 피치클래스 2)
│   ├── quality           "min7"
│   ├── tensions          ["9", "11"] (텐션 노트)
│   ├── bass              null (슬래시 코드의 베이스)
│   ├── bar               1
│   ├── beat              1.0
│   └── durationBeats     4.0
│
├── [Layer 1] ──────── Phase 2에서 채워짐
│   ├── degree            "ii"  (스케일 디그리)
│   ├── isDiatonic        true  (다이어토닉 여부)
│   └── functions         [{function:"SD", confidence:1.0}]
│
├── [Layer 2] ──────── Phase 3에서 채워짐
│   ├── secondaryDominant     (세컨더리 도미넌트 정보)
│   ├── groupMemberships      (ii-V-I 그룹 소속)
│   ├── diminishedFunction    (감화음 기능)
│   ├── chromaticApproach     (반음계적 접근)
│   ├── deceptiveResolution   (기만 종지)
│   └── pedalInfo             (페달 포인트)
│
├── [Layer 3] ──────── Phase 4에서 채워짐
│   ├── modalInterchange      (모달 인터체인지)
│   ├── modeSegment           (모드 세그먼트)
│   └── tonicization          (조성화/전조)
│
└── [모호성] ────────── Phase 5에서 채워짐
    ├── ambiguityFlags        (모호성 플래그 목록)
    ├── normalizedQuality     (정규화된 코드 품질)
    └── ambiguityScore        (0.0~1.0 모호성 점수)
```

### 필드 상세 설명

#### 파싱 결과 (Phase 1)

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| `originalSymbol` | `String` | 원래 입력된 코드 기호 | `"Dm7"` |
| `root` | `int` | 근음의 피치 클래스 (C=0, C#=1, ..., B=11) | `2` (= D) |
| `quality` | `String` | 코드의 품질/종류 | `"min7"`, `"dom7"`, `"maj7"` |
| `tensions` | `List<String>` | 텐션 노트들 | `["9", "#11"]` |
| `bass` | `Integer` | 슬래시 코드의 베이스 (없으면 null) | `0` (= C/C) |
| `bar` | `int` | 마디 번호 | `1` |
| `beat` | `double` | 박 위치 | `1.0` |
| `durationBeats` | `double` | 지속 박수 | `4.0` |

#### Layer 1 – 개별 코드 분석 (Phase 2)

| 필드 | 타입 | 설명 | 예시 |
|------|------|------|------|
| `degree` | `String` | 스케일 디그리 | `"ii"`, `"V"`, `"bVII"` |
| `isDiatonic` | `Boolean` | 키의 음계에 속하는지 | `true` |
| `functions` | `List<FunctionEntry>` | 화성 기능 목록 | `[{T, 1.0}]` |

#### Layer 2 – 문맥 분석 (Phase 3)

| 필드 | 타입 | 설명 |
|------|------|------|
| `secondaryDominant` | `SecondaryDominantInfo` | 세컨더리 도미넌트이면 관련 정보 |
| `groupMemberships` | `List<GroupMembership>` | 어떤 ii-V-I 그룹에 속하는지 |
| `diminishedFunction` | `String` | 감화음이면 그 기능 ("passing", "auxiliary", "dominant_function") |
| `chromaticApproach` | `ChromaticApproachInfo` | 반음계적 접근이면 관련 정보 |
| `deceptiveResolution` | `DeceptiveResolutionInfo` | 기만 종지이면 관련 정보 |
| `pedalInfo` | `PedalInfo` | 페달 포인트 위에 있으면 관련 정보 |

#### Layer 3 – 구조 분석 (Phase 4)

| 필드 | 타입 | 설명 |
|------|------|------|
| `modalInterchange` | `ModalInterchangeInfo` | 다른 모드에서 빌려온 코드이면 정보 |
| `modeSegment` | `String` | 이 구간의 모드 ("dorian", "lydian" 등) |
| `tonicization` | `TonicizationInfo` | 조성화/전조에 관여하면 정보 |

#### 모호성 (Phase 5)

| 필드 | 타입 | 설명 |
|------|------|------|
| `ambiguityFlags` | `List<AmbiguityFlag>` | 해석이 모호한 지점들 |
| `normalizedQuality` | `String` | 정규화된 코드 품질 |
| `ambiguityScore` | `double` | 0.0(확실) ~ 1.0(모호) 점수 |

---

## 4. FunctionEntry – 화성 기능

```java
public class FunctionEntry {
    String function;     // "T"(토닉), "SD"(서브도미넌트), "D"(도미넌트), "D_substitute" 등
    double confidence;   // 확신도 (0.0 ~ 1.0)
    String note;         // 보충 설명 (null 가능)
}
```

> 💡 하나의 코드가 여러 기능을 가질 수 있습니다. 예를 들어 vi(Am7)는 T(토닉, 0.7)이면서 SD(서브도미넌트, 0.3)일 수 있습니다.

**예시:**
```
Cmaj7의 기능 → [FunctionEntry("T", 1.0)]          // 확실한 토닉
Am7의 기능  → [FunctionEntry("T", 0.7, "Tonic substitute (relative minor)"),
               FunctionEntry("SD", 0.3, "Subdominant function in some progressions")]
```

---

## 5. GroupMembership – 그룹 소속 정보

```java
public class GroupMembership {
    int groupId;        // 그룹 고유 번호
    String groupType;   // "ii-V-I"
    String role;        // 그룹 내 역할: "ii", "V", "I", "V (tritone sub bII7)" 등
    String variant;     // 변형: "standard", "minor", "backdoor", "tritone_sub_V" 등
}
```

> 💡 한 코드가 여러 그룹에 동시에 속할 수 있습니다. 예를 들어 한 코드가 한 ii-V-I의 "I"이면서 다음 ii-V-I의 "ii"가 되는 경우.

---

## 6. SecondaryDominantInfo – 세컨더리 도미넌트

```java
public class SecondaryDominantInfo {
    String type;             // "V/ii", "V/vi" 등
    String impliedDominant;  // 감화음일 때 암시하는 도미넌트 (예: "B7b9")
    String targetDegree;     // 해결 대상 디그리 ("ii", "vi" 등)
    String targetChord;      // 해결 대상 코드 기호 ("Dm7")
    boolean resolved;        // 실제로 해결되었는지
    Map<String, Object> originPosition;  // 원래 위치 {bar, beat}
}
```

> 💡 **세컨더리 도미넌트란?** 원래 키의 V7이 아닌데, 다른 코드를 "임시 토닉"으로 만들어주는 도미넌트7 코드입니다.
> 예: C장조에서 A7 → Dm7이면, A7은 "V/ii" (Dm으로 가기 위한 세컨더리 도미넌트)

---

## 7. ChromaticApproachInfo – 반음계적 접근

```java
public class ChromaticApproachInfo {
    String target;         // 접근 대상 코드 기호
    int targetBar;         // 대상 마디
    double targetBeat;     // 대상 박
    String direction;      // "above" (위에서) 또는 "below" (아래에서)
    boolean qualityMatch;  // 대상과 같은 코드 품질인지
}
```

> 💡 **반음계적 접근이란?** 다이어토닉이 아닌 코드가 반음 위 또는 아래에서 다음 코드로 미끄러지듯 진행하는 것.
> 예: Ebmaj7 → Dm7 (반음 위에서 접근)

---

## 8. DeceptiveResolutionInfo – 기만 종지

```java
public class DeceptiveResolutionInfo {
    String dominantChord;       // 도미넌트 코드 (예: "G7")
    String expectedResolution;  // 기대되는 해결 (예: "Cmaj7")
    String actualResolution;    // 실제 해결된 코드 (예: "Am7")
    String actualDegree;        // 실제 해결 코드의 디그리 ("vi")
    boolean commonPattern;      // 흔한 기만 종지 패턴인지
}
```

> 💡 **기만 종지란?** V7 → I으로 갈 것 같았는데, 예상과 다른 코드로 가는 것.
> 대표적: G7 → Am7 (V → vi, "기만 종지"의 가장 고전적인 형태)

---

## 9. PedalInfo – 페달 포인트

```java
public class PedalInfo {
    int pedalNote;          // 페달 음의 피치 클래스
    String pedalNoteName;   // 페달 음 이름 (예: "C")
    String pedalType;       // "tonic", "dominant", "subdominant", "on X"
    boolean isOverPedal;    // 페달 위에 있는지
    int pedalStartBar;      // 페달 시작 마디
    int pedalEndBar;        // 페달 끝 마디
}
```

> 💡 **페달 포인트란?** 베이스 음이 여러 마디에 걸쳐 같은 음으로 지속되는 것.
> 예: Cmaj7 → Dm7/C → Em7/C → Cmaj7 (C가 계속 베이스에서 울림)

---

## 10. ModalInterchangeInfo – 모달 인터체인지

```java
public class ModalInterchangeInfo {
    String sourceMode;        // 빌려온 모드 (예: "aeolian")
    String borrowedDegree;    // 빌려온 디그리 (예: "iv")
    boolean isCommonBorrow;   // 흔한 차용인지
    List<ModalInterchangeMatch> allPossibleSources;  // 가능한 모든 출처

    // 내부 클래스
    static class ModalInterchangeMatch {
        String sourceMode;
        String borrowedDegree;
        boolean isCommonBorrow;
    }
}
```

> 💡 **모달 인터체인지란?** 같은 으뜸음의 다른 음계(모드)에서 코드를 빌려오는 것.
> 예: C장조에서 Fm7을 사용 → 이건 C 에올리안(자연단음계)의 iv를 빌려온 것

---

## 11. TonicizationInfo – 조성화/전조

```java
public class TonicizationInfo {
    String type;            // "tonicization" 또는 "modulation"
    String temporaryKey;    // 일시적 키 (예: "G")
    int startBar;           // 시작 마디
    int endBar;             // 끝 마디
    List<String> evidence;  // 증거 목록
    double confidence;      // 확신도 (0.0 ~ 1.0)
}
```

> 💡 **조성화 vs 전조:**
> - **조성화(Tonicization):** 잠깐 다른 키를 암시했다가 금방 돌아옴 (1~2마디)
> - **전조(Modulation):** 오래 동안 새로운 키에 머무름 (6마디 이상 + 2회 이상 케이던스)

---

## 12. AmbiguityFlag – 모호성 플래그

```java
public class AmbiguityFlag {
    String aspect;                  // 모호한 측면 ("function", "diminished_function" 등)
    List<String> interpretations;   // 가능한 해석들
    boolean contextNeeded;          // 추가 문맥이 필요한지
}
```

> 💡 한 코드에 대해 "이건 이렇게도, 저렇게도 해석할 수 있다"는 것을 기록하는 플래그입니다.

**예시:**
```
AmbiguityFlag {
    aspect: "diminished_function",
    interpretations: ["passing", "auxiliary", "dominant_function"],
    contextNeeded: true
}
```
→ "이 감화음의 기능을 확실히 모르겠고, passing / auxiliary / dominant_function 중 하나일 수 있다"는 뜻

