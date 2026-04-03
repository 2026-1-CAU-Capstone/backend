# 🔧 유틸리티와 설정 데이터

> 이 문서는 분석 엔진의 **기반 도구**인 `NoteUtils`와 `AnalysisConfigData`를 설명합니다.
> 파이프라인의 거의 모든 단계에서 사용되는 핵심 유틸리티입니다.

---

## 1. NoteUtils – 음악 이론 유틸리티

**파일:** `analysis/util/NoteUtils.java`

음표 이름 ↔ 숫자 변환, 키 파싱 등 **음악 이론의 가장 기본적인 연산**을 담당합니다.

### 1.1 피치 클래스(Pitch Class) 개념

서양 음악에서 한 옥타브는 12개의 반음으로 나뉩니다. 각 음에 0~11의 번호를 매긴 것이 **피치 클래스**입니다:

```
C=0   C#/Db=1   D=2   D#/Eb=3   E=4   F=5
F#/Gb=6   G=7   G#/Ab=8   A=9   A#/Bb=10   B=11
```

> 💡 옥타브는 무시합니다. 낮은 C나 높은 C나 모두 피치 클래스 0입니다.

### 1.2 주요 메서드

#### `parseNoteName(String name) → int`

**음표 이름을 피치 클래스 숫자로 변환합니다.**

```java
NoteUtils.parseNoteName("C")  → 0
NoteUtils.parseNoteName("D")  → 2
NoteUtils.parseNoteName("Bb") → 10   // B(11) - 1(♭) = 10
NoteUtils.parseNoteName("F#") → 6    // F(5) + 1(♯) = 6
NoteUtils.parseNoteName("Db") → 1    // D(2) - 1(♭) = 1
```

**동작 원리:**
1. 첫 글자(알파벳)로 기본 피치 클래스를 찾습니다 (`C→0, D→2, ...`)
2. 이후 `#`이 있으면 +1, `b`이 있으면 -1을 합니다
3. 결과를 mod 12 처리하여 0~11 범위로 맞춥니다

#### `pcToNoteName(int pc) → String`

**피치 클래스 숫자를 음표 이름으로 변환합니다.**

```java
NoteUtils.pcToNoteName(0)  → "C"
NoteUtils.pcToNoteName(1)  → "Db"
NoteUtils.pcToNoteName(6)  → "F#"
NoteUtils.pcToNoteName(10) → "Bb"
```

내부적으로 `{"C", "Db", "D", "Eb", "E", "F", "F#", "G", "Ab", "A", "Bb", "B"}` 배열을 사용합니다.

#### `parseKey(String keyStr) → KeyInfo`

**키 문자열을 파싱합니다.**

```java
NoteUtils.parseKey("C")    → KeyInfo(root=0, mode="major")
NoteUtils.parseKey("Am")   → KeyInfo(root=9, mode="minor")
NoteUtils.parseKey("Bbm")  → KeyInfo(root=10, mode="minor")
NoteUtils.parseKey("F#")   → KeyInfo(root=6, mode="major")
```

**규칙:**
- 끝에 `m` → 마이너 키 (`"Am"` → A 마이너)
- 끝에 `min` → 마이너 키 (`"Amin"` → A 마이너)
- 그 외 → 메이저 키

#### `interval(int from, int to) → int`

**두 피치 클래스 사이의 반음 간격을 구합니다.**

```java
NoteUtils.interval(0, 7)  → 7   // C에서 G까지 = 완전5도
NoteUtils.interval(7, 0)  → 5   // G에서 C까지 = 완전4도 (위로 올라감)
NoteUtils.interval(0, 4)  → 4   // C에서 E까지 = 장3도
```

> 항상 "위로 올라가는" 방향으로 계산합니다 (mod 12).

#### `mod12(int v) → int`

**어떤 정수든 0~11 범위로 만드는 모듈로 연산입니다.**

```java
NoteUtils.mod12(14) → 2    // 14 % 12 = 2
NoteUtils.mod12(-1) → 11   // 음수도 올바르게 처리
NoteUtils.mod12(0)  → 0
```

> Java의 `%` 연산자는 음수일 때 음수를 반환할 수 있어서, `((v % 12) + 12) % 12`로 항상 양수를 보장합니다.

### 1.3 KeyInfo 레코드

```java
public record KeyInfo(int root, String mode) {
    boolean isMajor()  // mode가 "major"이면 true
    boolean isMinor()  // mode가 "minor"이면 true
}
```

---

## 2. AnalysisConfigData – 설정 데이터

**파일:** `analysis/config/AnalysisConfigData.java`

화성 분석에 필요한 **참조 데이터(룩업 테이블)**를 제공하는 컴포넌트입니다.
Python 버전의 `config_data.py`를 Java로 포팅한 것입니다.

### 2.1 Function Map (화성 기능 매핑)

`getFunctionMap()` → `Map<String, Map<String, List<FunctionEntry>>>`

**"이 스케일 디그리는 어떤 화성 기능을 가지는가?"를 정의한 3단계 맵입니다.**

```
                      "major_key"    →  "I" → [T(1.0)]
getFunctionMap() →    "minor_key"        "ii" → [SD(1.0)]
                      "chromatic_degrees" "V" → [D(1.0)]
                                         "iii" → [T(0.6), D_mediant(0.3)]
                                         ...
```

#### 구조

1단계 키: `"major_key"`, `"minor_key"`, `"chromatic_degrees"`
2단계 키: 스케일 디그리 (`"I"`, `"ii"`, `"bVII"` 등)
3단계 값: `FunctionEntry` 리스트

#### major_key (장조에서의 기능)

| 디그리 | 기능 | 확신도 | 설명 |
|--------|------|--------|------|
| I | T (토닉) | 1.0 | 장조의 중심, 가장 안정적 |
| ii | SD (서브도미넌트) | 1.0 | "집을 떠나려는" 기능 |
| iii | T (토닉) | 0.6 | 토닉 대리 (I과 음 2개 공유) |
| iii | D_mediant | 0.3 | 일부 문맥에서 도미넌트 매디안트 |
| IV | SD (서브도미넌트) | 1.0 | 대표적 서브도미넌트 |
| V | D (도미넌트) | 1.0 | 가장 강한 도미넌트 |
| vi | T (토닉) | 0.7 | 토닉 대리 (상대단조) |
| vi | SD | 0.3 | 일부 진행에서 서브도미넌트 |
| vii | D (도미넌트) | 0.9 | 이끎음 화음, 도미넌트 기능 |

> 💡 하나의 디그리가 **여러 기능**을 가질 수 있다는 것이 핵심입니다. 
> iii는 보통 토닉(0.6)이지만, 문맥에 따라 도미넌트 매디안트(0.3)로 해석될 수도 있습니다.

#### chromatic_degrees (반음계적 코드의 기능)

다이어토닉이 아닌 코드들의 기능 해석입니다.

| 디그리 | 주요 기능 | 설명 |
|--------|-----------|------|
| bII | SD(0.6), D_substitute(0.4) | 나폴리탄/프리지안, 또는 V의 트라이톤 대리 |
| bIII | T(0.5), SD(0.3) | 마이너에서 빌려온 토닉 대리 |
| bVI | SD(0.7), T(0.3) | 마이너에서 빌려온 서브도미넌트 |
| bVII | SD(0.6), D(0.3) | 믹솔리디안/에올리안에서 빌려옴, 백도어 도미넌트 |

#### minor_key (단조에서의 기능)

장조와 비슷하지만, 단조 특유의 코드들이 추가됩니다.

| 디그리 | 기능 | 설명 |
|--------|------|------|
| i | T(1.0) | 단조의 토닉 |
| bIII | T(0.6) | 상대장조, 토닉 대리 |
| v | D(0.6) | 약한 도미넌트 (자연단음계) |
| V | D(1.0) | 화성단음계의 도미넌트 |
| bVII | SD(0.5), D(0.4) | 서브토닉 도미넌트 |

### 2.2 Modal Interchange (모달 인터체인지 데이터)

`getModalInterchange()` → `Map<String, ModeInterchangeData>`

**"이 모드에서는 어떤 코드를 빌려올 수 있는가?"를 정의합니다.**

#### 내부 레코드 타입

```java
// 모드에서 사용 가능한 각 음정 위치의 코드 정보
record DegreeInfo(
    int interval,        // 키의 근음에서부터의 반음 간격 (0~11)
    String quality,      // 코드 품질 ("min7", "maj7", "dom7" 등)
    String degreeLabel   // 디그리 표기 ("i", "bVII" 등)
)

// 흔히 빌려오는 코드 정보
record CommonBorrow(
    String degreeLabel,  // 디그리 표기
    String note          // 설명
)

// 모드 전체 데이터
record ModeInterchangeData(
    String name,                          // "Natural Minor (Aeolian)" 등
    List<DegreeInfo> availableDegrees,    // 이 모드에서 만들 수 있는 코드들
    List<CommonBorrow> commonBorrows      // 흔히 빌려오는 코드들
)
```

#### 지원하는 모드

| 모드 | 이름 | 자주 빌려오는 코드 |
|------|------|---------------------|
| **aeolian** | 자연단음계 | bVII, bVI, iv, bIII, v |
| **dorian** | 도리안 | IV7, bVII |
| **phrygian** | 프리지안 | bII |
| **lydian** | 리디안 | II7, #iv° |
| **mixolydian** | 믹솔리디안 | bVII, I7 |

#### 예시: aeolian (자연단음계)

C장조에서 C 에올리안(= C 자연단음계)의 코드를 빌려오면:

```
interval=0,  quality="min7",    degreeLabel="i"     → Cm7    (장조의 I을 단조의 i으로)
interval=3,  quality="maj7",    degreeLabel="bIII"  → Ebmaj7 (bIII 코드)
interval=5,  quality="min7",    degreeLabel="iv"    → Fm7    (iv 코드) ← 아주 흔함!
interval=8,  quality="maj7",    degreeLabel="bVI"   → Abmaj7 (bVI 코드)
interval=10, quality="dom7",    degreeLabel="bVII"  → Bb7    (bVII 코드) ← 가장 흔함!
```

> 💡 `commonBorrows`에 있는 코드는 "이 빌림은 매우 흔하다"라고 표시됩니다.
> 팝/록/재즈에서 bVII, bVI, iv는 정말 자주 등장하는 차용 화음입니다.

