# 🎹 Phase 1~2: 파싱, 정규화, 분류, 기능 라벨링

> 이 문서는 분석 파이프라인의 **초반부(Phase 1~2)** 를 설명합니다.
> 텍스트 입력을 코드 객체로 변환하고, 각 코드의 기본 성질을 파악하는 단계입니다.

---

## 목차

1. [AnalysisRequest & Controller – API 진입점](#1-analysisrequest--controller--api-진입점)
2. [HarmonicAnalysisService – 파이프라인 오케스트레이터](#2-harmonicanalysisservice--파이프라인-오케스트레이터)
3. [ChordSymbolParser – 코드 기호 파서](#3-chordsymbolparser--코드-기호-파서)
4. [ChordNormalizer – 코드 품질 정규화](#4-chordnormalizer--코드-품질-정규화)
5. [DiatonicClassifier – 다이어토닉 분류기](#5-diatonicclassifier--다이어토닉-분류기)
6. [FunctionLabeler – 화성 기능 라벨러](#6-functionlabeler--화성-기능-라벨러)

---

## 1. AnalysisRequest & Controller – API 진입점

### AnalysisRequest (요청 DTO)

```java
public record AnalysisRequest(
    @NotBlank String text,       // 코드 진행 텍스트 (필수!)
    String key,                   // 키 (기본값: "C")
    String title,                 // 곡 제목 (기본값: "Untitled")
    String timeSignature          // 박자 (기본값: "4/4")
)
```

Java `record`의 **compact constructor**를 사용해 기본값을 처리합니다:
- `key`가 null이거나 비어있으면 → `"C"`
- `title`이 null이거나 비어있으면 → `"Untitled"`
- `timeSignature`가 null이거나 비어있으면 → `"4/4"`

### AnalysisController

```
POST /v1/analysis
```

1. `AnalysisRequest`를 `@Valid`로 검증
2. `HarmonicAnalysisService.analyze()`를 호출
3. 결과를 `ApiResponse<Map<String, Object>>`로 감싸서 반환

> `AnalysisControllerSpec` 인터페이스에 Swagger 어노테이션이 들어있고, 실제 Controller는 이를 구현합니다.

---

## 2. HarmonicAnalysisService – 파이프라인 오케스트레이터

**파일:** `service/HarmonicAnalysisService.java`

**이 클래스가 분석 엔진의 "지휘자" 역할을 합니다.**

17개의 `implementation` 컴포넌트를 주입받아, 정해진 순서대로 호출합니다.

```java
public Map<String, Object> analyze(String text, String key, String title, String timeSignature) {

    // Phase 1: 텍스트 파싱 → ParsedChord 리스트 생성
    //   정규식으로 코드 기호를 근음+품질+텐션+베이스로 분해하고, 마디/박 위치를 계산
    ParseResult pr = chordSymbolParser.parseProgressionText(text, title, key, timeSignature);
    List<ParsedChord> chords = pr.chords();

    // Phase 2: Layer 1 – 개별 코드 분석 (품질 정규화 → 다이어토닉 분류 → 기능 라벨링)
    chordNormalizer.normalize(chords);          // 텐션 무시, 핵심 품질만 추출
    diatonicClassifier.classify(chords, key);   // 구성음이 키 음계에 속하는지 + 디그리 부여
    functionLabeler.label(chords, key);          // 설정 맵 참조하여 T/SD/D 기능 부여

    // Phase 3: Layer 2 – 문맥 패턴 감지 (앞뒤 코드 관계 분석)
    IiViResult iiViResult = iiViDetector.detect(chords, key);  // V 기준 앞뒤 탐색
    // ... (여러 감지기: 트라이톤 대리, 세컨더리 도미넌트, 감화음, 반음계적 접근, 기만 종지, 페달)

    // Phase 4: Layer 3 – 구조 분석 (곡 전체 차원)
    modalInterchangeDetector.detect(chords, key);  // 다른 모드에서 빌려온 코드 감지
    // ... (모드 세그먼트, 조성화/전조, 섹션 경계)

    // Phase 5: 모호성 채점 – 5가지 하위 점수의 가중 합으로 0.0~1.0 계산
    ambiguityScorer.score(chords);

    // Phase 6: 최종 집계 – 모든 결과를 song/chords/groups/sections/stats로 구조화
    return aggregator.aggregate(title, key, timeSignature, chords, groups, sections);
}
```

> 💡 **핵심 패턴:** 모든 분석기는 `List<ParsedChord>`를 받아서, 각 `ParsedChord`의 필드를 채우고, 
> 같은 리스트를 반환합니다. 리스트가 **파이프라인을 통과하면서 점점 풍부해지는** 구조입니다.

---

## 3. ChordSymbolParser – 코드 기호 파서

**파일:** `service/implementation/ChordSymbolParser.java`  
**역할:** Phase 1 – 텍스트 문자열을 `ParsedChord` 객체 리스트로 변환

### 3.1 텍스트 입력 형식

```
Dm7 G7 | Cmaj7 | Am7 D7 | Gmaj7 |
```

- `|` 로 마디를 구분
- 공백으로 한 마디 안의 코드를 구분
- `#`으로 시작하는 줄은 주석
- `N.C.` 또는 `NC`는 "코드 없음"

### 3.2 전체 파싱 흐름

```
"Dm7 G7 | Cmaj7"
      │
      ▼ 줄 단위 분할 + | 단위 분할
      │
   마디 1: "Dm7 G7"      마디 2: "Cmaj7"
      │                       │
      ▼ 공백으로 분할           ▼
   ["Dm7", "G7"]          ["Cmaj7"]
      │                       │
      ▼ 박 계산 (4/4 기준)     ▼
   Dm7: beat=1.0, dur=2.0    Cmaj7: beat=1.0, dur=4.0
   G7:  beat=3.0, dur=2.0
      │                       │
      ▼ parseChordSymbol()    ▼
   ParsedChord 객체들 생성
```

**박 계산 로직:**
- 한 마디에 코드가 `n`개 있으면, 각 코드의 지속 시간은 `beatsPerBar / n`
- 예: 4/4 박자에 코드 2개 → 각 2박씩
- 예: 4/4 박자에 코드 1개 → 4박

### 3.3 개별 코드 기호 파싱

정규식으로 코드 기호를 3부분으로 분해합니다:

```
"Dm7(11)/C"
 ▲ ▲▲▲▲▲  ▲
 │ │      └── 슬래시 베이스: "C" → 피치클래스 0
 │ └── 품질+텐션: "m7(11)" → quality="min7", tensions=["11"]
 └── 근음: "D" → 피치클래스 2
```

**정규식:**
```regex
^([A-Ga-g][#b♯♭]*)    ← 근음 (필수)
(.*?)                   ← 품질 + 텐션 (선택)
(?:/([A-Ga-g][#b♯♭]*))?$  ← 슬래시 베이스 (선택)
```

### 3.4 품질(Quality) 파싱 – parseQualityAndTensions()

이 메서드가 가장 복잡합니다. "m7", "maj9", "dim7", "7#11" 같은 다양한 코드 표기법을 통일된 내부 표기로 변환합니다.

**지원하는 코드 품질 (우선순위 순):**

| 내부 이름 | 인식하는 표기법 | 설명 |
|-----------|-----------------|------|
| `minmaj7` | `mMaj7`, `m(maj7)`, `min(maj7)` | 마이너 메이저 세븐 |
| `min7b5` | `m7b5`, `ø`, `∅` | 하프디미니쉬 (반감7) |
| `dim7` | `dim7`, `o7` | 감7화음 |
| `dim` | `dim`, `o` | 감3화음 |
| `augmaj7` | `aug maj7`, `+maj7`, `maj7#5` | 증메이저세븐 |
| `aug7` | `aug7`, `+7`, `7#5` | 증7화음 |
| `aug` | `aug`, `+` | 증3화음 |
| `dom7sus4` | `7sus4`, `7sus` | 도미넌트7 서스4 |
| `sus4` | `sus4`, `sus` | 서스펜디드4 |
| `sus2` | `sus2` | 서스펜디드2 |
| `min7` | `m7`, `min7`, `-7` | 마이너7 |
| `min6` | `m6`, `min6`, `-6` | 마이너6 |
| `min` | `m`, `min`, `-` | 마이너 |
| `maj7` | `maj7`, `M7`, `^7`, `^`, `Δ` | 메이저7 |
| `maj6` | `maj6`, `M6`, `6` | 메이저6 |
| `dom7` | `7` | 도미넌트7 |
| `maj` | `maj`, `M`, (아무것도 없음) | 메이저 |
| `power` | `5` | 파워코드 |

**확장형 코드 처리:**

| 표기 | 변환 결과 | 이유 |
|------|-----------|------|
| `9` | quality=`dom7`, tensions=[`9`] | 9th = 7th + 9 |
| `11` | quality=`dom7`, tensions=[`9`, `11`] | 11th = 7th + 9 + 11 |
| `13` | quality=`dom7`, tensions=[`9`, `11`, `13`] | 13th = 7th + 9 + 11 + 13 |
| `m9` | quality=`min7`, tensions=[`9`] | 마이너 9th |
| `maj9` | quality=`maj7`, tensions=[`9`] | 메이저 9th |
| `7alt` | quality=`dom7`, tensions=[`b9`, `#9`, `#11`, `b13`] | 올터드 |

**특수 문자 처리:**
- `Δ` (삼각형) → `maj7`
- `°` (도 기호) → `dim`
- `∅` 또는 `ø` → `min7b5`

**괄호 처리:**
- `Dm7(11)` → 괄호 안의 `11`은 텐션으로 추출
- 단, `(maj7)`처럼 품질을 나타내는 경우는 품질 파싱에 포함

### 3.5 파싱 결과 예시

입력: `"Dm7 G7 | Cmaj7"`

```
ParsedChord[0]:
  originalSymbol = "Dm7"
  root = 2 (D)
  quality = "min7"
  tensions = []
  bass = null
  bar = 1, beat = 1.0, durationBeats = 2.0

ParsedChord[1]:
  originalSymbol = "G7"
  root = 7 (G)
  quality = "dom7"
  tensions = []
  bass = null
  bar = 1, beat = 3.0, durationBeats = 2.0

ParsedChord[2]:
  originalSymbol = "Cmaj7"
  root = 0 (C)
  quality = "maj7"
  tensions = []
  bass = null
  bar = 2, beat = 1.0, durationBeats = 4.0
```

---

## 4. ChordNormalizer – 코드 품질 정규화

**파일:** `service/implementation/ChordNormalizer.java`  
**역할:** 코드 품질을 **핵심 품질(core quality)** 로 정규화

### 왜 필요한가?

텐션이 다른 코드도 핵심 기능은 같습니다:
- `Dm7` = `Dm9` = `Dm11` → 모두 핵심은 `min7`
- `G7` = `G9` = `G13` → 모두 핵심은 `dom7`

### 동작

```java
// 매핑 테이블 (간략화)
"maj7" → "maj7"
"min7" → "min7"
"dom7" → "dom7"
// ... 등등 (총 18개)
```

각 `ParsedChord`의 `quality`를 `normalizedQuality` 필드에 복사합니다.
이미 핵심 품질이면 그대로, 알 수 없는 품질이면 원래 값을 그대로 사용합니다.

```
Dm9   → quality="min7", normalizedQuality="min7"
Cmaj7 → quality="maj7", normalizedQuality="maj7"
```

> 💡 이후 분석기들은 주로 `normalizedQuality`를 사용하여 패턴 매칭을 합니다.

---

## 5. DiatonicClassifier – 다이어토닉 분류기

**파일:** `service/implementation/DiatonicClassifier.java`  
**역할:** 각 코드가 현재 키의 음계에 속하는지(다이어토닉인지) 판별하고, 스케일 디그리를 부여

### 5.1 핵심 개념: "다이어토닉이란?"

**다이어토닉(Diatonic)** 코드란, 해당 키의 음계 음들만으로 구성된 코드입니다.

```
C 장조의 음계: C D E F G A B

다이어토닉 코드들:
I   = Cmaj7  (C-E-G-B)     ✅ 모든 음이 C장조 음계에 있음
ii  = Dm7    (D-F-A-C)     ✅
iii = Em7    (E-G-B-D)     ✅
IV  = Fmaj7  (F-A-C-E)     ✅
V   = G7     (G-B-D-F)     ✅
vi  = Am7    (A-C-E-G)     ✅
vii°= Bm7b5  (B-D-F-A)     ✅

다이어토닉이 아닌 코드:
Bb7  → Bb는 C장조 음계에 없음 ❌
Ebmaj7 → Eb는 C장조 음계에 없음 ❌
```

### 5.2 사용하는 음계

| 음계 | 반음 간격 | 설명 |
|------|-----------|------|
| 장음계 (Major) | `0,2,4,5,7,9,11` | C D E F G A B |
| 자연단음계 (Natural Minor) | `0,2,3,5,7,8,10` | C D Eb F G Ab Bb |
| 화성단음계 (Harmonic Minor) | `0,2,3,5,7,8,11` | C D Eb F G Ab B |
| 선율단음계 (Melodic Minor) | `0,2,3,5,7,9,11` | C D Eb F G A B |

### 5.3 다이어토닉 검사 로직

```
checkDiatonic(chordRoot, quality, keyRoot, scale):
    1. 키의 음계 음들을 피치 클래스 집합으로 만든다
       예: C장조 → {0, 2, 4, 5, 7, 9, 11}
    
    2. 코드의 구성음을 구한다
       예: Dm7 (root=2, quality="min7") → intervals=[0,3,7,10]
       → 실제 피치클래스: {2, 5, 9, 0}  (D, F, A, C)
    
    3. 코드의 모든 구성음이 음계에 포함되는지 확인
       {2, 5, 9, 0} ⊆ {0, 2, 4, 5, 7, 9, 11}?  → ✅ YES!
```

**단조의 경우:** 자연단음계에서 다이어토닉이 아니면, 화성단음계와 선율단음계에서도 확인합니다.
이는 단조에서 V(메이저 도미넌트)가 화성단음계에서는 다이어토닉이기 때문입니다.

### 5.4 스케일 디그리 부여

근음과 키 근음의 **반음 간격(interval)** 으로 디그리를 결정합니다.

```
C장조에서 D의 interval:
interval = mod12(D - C) = mod12(2 - 0) = 2
→ 기본 라벨: "ii"
```

**대소문자 규칙:**
- **마이너 품질** (`min7`, `min`, `dim` 등) → 소문자: `ii`, `iii`, `vi`
- **메이저 품질** (`maj7`, `dom7`, `aug` 등) → 대문자: `I`, `IV`, `V`
- **감화음** → 끝에 `°` 추가: `vii°`

**장조 디그리 라벨 테이블:**

| 반음 간격 | 기본 라벨 | 예 (C장조) |
|-----------|-----------|------------|
| 0 | I | C |
| 1 | bII | Db |
| 2 | ii | D |
| 3 | bIII | Eb |
| 4 | iii | E |
| 5 | IV | F |
| 6 | #IV | F# |
| 7 | V | G |
| 8 | bVI | Ab |
| 9 | vi | A |
| 10 | bVII | Bb |
| 11 | vii | B |

### 5.5 분류 결과 예시

C장조에서의 분류:

```
Dm7   → degree="ii",   isDiatonic=true
G7    → degree="V",    isDiatonic=true
Cmaj7 → degree="I",    isDiatonic=true
Bb7   → degree="bVII", isDiatonic=false
Ebmaj7 → degree="bIII", isDiatonic=false
```

---

## 6. FunctionLabeler – 화성 기능 라벨러

**파일:** `service/implementation/FunctionLabeler.java`  
**역할:** 각 코드에 T(토닉) / SD(서브도미넌트) / D(도미넌트) 기능을 부여

### 6.1 label() – 1차 기능 부여

`AnalysisConfigData`의 Function Map을 참조하여 기능을 부여합니다.

```
입력: 각 코드의 degree + isDiatonic
참조: AnalysisConfigData.getFunctionMap()
출력: 각 코드의 functions 필드
```

**로직:**

```
for (각 코드) {
    1. 디그리에서 °, + 등의 접미사를 제거하여 룩업 키를 만든다
       예: "vii°" → "vii"
    
    2. 다이어토닉이면?
       → major_key (또는 minor_key) 맵에서 찾는다
       예: "ii" → [FunctionEntry("SD", 1.0)]
    
    3. 다이어토닉이 아니면?
       → chromatic_degrees 맵에서 먼저 찾는다
       → 없으면 major_key/minor_key 맵에서 찾는다
       → 그래도 없으면 모호성 플래그를 추가한다
}
```

### 6.2 labelFromGroups() – 2차 기능 부여 (그룹 기반)

ii-V-I 감지 후에 호출됩니다. 아직 기능이 비어있는 코드에 대해, **ii-V-I 그룹 역할**을 기반으로 기능을 부여합니다.

| ii-V-I 역할 | 부여되는 기능 | 확신도 |
|-------------|---------------|--------|
| `ii` | SD | 0.9 |
| `iv (backdoor)` | SD | 0.8 |
| `V` | D | 1.0 |
| `V (tritone sub bII7)` | D | 0.9 |
| `V (backdoor bVII7)` | D | 0.8 |
| `I` | T | 1.0 |
| `I (iii substitute)` | T | 0.7 |

### 6.3 기능 부여 결과 예시

C장조에서 `Dm7 → G7 → Cmaj7`:

```
Dm7:
  degree = "ii"
  isDiatonic = true
  functions = [FunctionEntry("SD", 1.0)]  ← 서브도미넌트

G7:
  degree = "V"
  isDiatonic = true
  functions = [FunctionEntry("D", 1.0)]   ← 도미넌트

Cmaj7:
  degree = "I"
  isDiatonic = true
  functions = [FunctionEntry("T", 1.0)]   ← 토닉
```

C장조에서 `Bb7`:

```
Bb7:
  degree = "bVII"
  isDiatonic = false
  functions = [FunctionEntry("SD", 0.6, "Modal interchange (mixolydian/aeolian)"),
               FunctionEntry("D", 0.3, "Backdoor dominant approach")]
  ← 반음계적 코드이므로 여러 해석 가능!
```

