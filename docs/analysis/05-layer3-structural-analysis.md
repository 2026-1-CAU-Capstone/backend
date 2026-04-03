# 🏗️ Phase 4: Layer 3 – 구조 분석

> 이 문서는 **Phase 4**를 설명합니다.
> Layer 2에서 코드 간 패턴을 감지했다면, Layer 3에서는 **곡 전체의 구조적 특성**을 분석합니다.
> 모달 인터체인지, 모드 세그먼트, 조성화/전조, 섹션 경계를 다룹니다.

---

## 목차

1. [ModalInterchangeDetector – 모달 인터체인지 감지](#1-modalinterchangedetector--모달-인터체인지-감지)
2. [ModeSegmentDetector – 모드 세그먼트 감지](#2-modesegmentdetector--모드-세그먼트-감지)
3. [TonicizationModulationDetector – 조성화/전조 감지](#3-tonicizationmodulationdetector--조성화전조-감지)
4. [SectionBoundaryDetector – 섹션 경계 감지](#4-sectionboundarydetector--섹션-경계-감지)

---

## 1. ModalInterchangeDetector – 모달 인터체인지 감지

**파일:** `service/implementation/ModalInterchangeDetector.java`  
**역할:** 같은 으뜸음의 **다른 모드(음계)에서 빌려온 코드**를 감지

### 1.1 모달 인터체인지란?

C 장조(이오니안)의 코드가 아닌데, C를 으뜸음으로 하는 **다른 음계의 코드**를 가져와 사용하는 것입니다.

```
C장조 곡에서:

Cmaj7 → Fm7 → Cmaj7
         ▲
    이건 C장조의 코드가 아니야!
    하지만 C 에올리안(자연단음계)의 iv(Fm7)를 빌려온 거야!
    → 모달 인터체인지!
```

> 💡 같은 "C"를 집으로 하지만, 방의 분위기(모드)를 잠깐 바꾸는 것입니다.
> 밝은 장조 방에서 잠깐 어두운 단조 방의 가구를 빌려오는 느낌.

### 1.2 감지 조건

이 감지기는 **장조 키에서만** 동작합니다 (단조에서는 건너뜀).

다음 조건을 모두 만족하는 코드만 검사:
- ❌ 다이어토닉이 아님
- ❌ 세컨더리 도미넌트가 아님

### 1.3 감지 로직

```
for (각 비다이어토닉 코드) {
    코드의 근음과 키 근음의 간격(interval) 계산
    코드의 정규화된 품질(normalizedQuality) 확인
    
    for (각 모드: aeolian, dorian, phrygian, lydian, mixolydian) {
        이 모드의 availableDegrees에서:
        interval과 quality가 모두 일치하는 DegreeInfo가 있는가?
        
        있다면 → 매치!
        commonBorrows에 있으면 isCommonBorrow = true
    }
    
    매치가 있으면:
    - 가장 "흔한 빌림(commonBorrow)"을 최우선으로 선택
    - 모든 가능한 출처를 allPossibleSources에 기록
}
```

### 1.4 감지 예시

C장조에서 `Fm7`의 분석:

```
코드: Fm7 (root=5, quality="min7")
interval = mod12(5 - 0) = 5

aeolian 모드 검사:
  availableDegrees에서 interval=5, quality="min7" 찾기
  → DegreeInfo(5, "min7", "iv") 발견! ✅
  → commonBorrows에 "iv"가 있나? → 있음! (isCommonBorrow = true)

결과:
  modalInterchange = {
    sourceMode: "aeolian",
    borrowedDegree: "iv",
    isCommonBorrow: true,
    allPossibleSources: [
      {sourceMode: "aeolian", borrowedDegree: "iv", isCommonBorrow: true}
    ]
  }
```

하나의 코드가 **여러 모드에서 동시에 설명**될 수 있습니다:

```
C장조에서 Bb7:
  - mixolydian의 bVII (interval=10, quality="dom7") → X (mixolydian의 bVII는 maj7)
  - aeolian의 bVII (interval=10, quality="dom7") → ✅!
```

### 1.5 기능 부여

기능이 비어있는 코드에는 `modal_interchange` 기능이 추가됩니다:
- 흔한 빌림 → 확신도 0.7
- 드문 빌림 → 확신도 0.5

---

## 2. ModeSegmentDetector – 모드 세그먼트 감지

**파일:** `service/implementation/ModeSegmentDetector.java`  
**역할:** 곡의 각 구간이 **어떤 모드(음계) 색채**를 띠는지 감지

### 2.1 모드란?

같은 7개 음으로 구성되지만, **시작 음(으뜸음)**에 따라 분위기가 달라지는 것:

| 모드 | 음계 (C 기준) | 특징적 분위기 |
|------|---------------|---------------|
| 이오니안 (Ionian) | C D E F G A B | 밝고 안정적 (일반 장조) |
| 도리안 (Dorian) | C D Eb F G A Bb | 약간 어두우면서 밝은 느낌 |
| 프리지안 (Phrygian) | C Db Eb F G Ab Bb | 스페인/아랍 분위기 |
| 리디안 (Lydian) | C D E F# G A B | 몽환적, 떠있는 느낌 |
| 믹솔리디안 (Mixolydian) | C D E F G A Bb | 블루지, 록 |
| 에올리안 (Aeolian) | C D Eb F G Ab Bb | 자연단음계, 슬프고 어두운 |
| 로크리안 (Locrian) | C Db Eb F Gb Ab Bb | 매우 불안정 |

### 2.2 감지 방법: 윈도우 스코어링

4마디 단위의 **슬라이딩 윈도우**로 분석합니다:

```
전체 곡: [마디1] [마디2] [마디3] [마디4] [마디5] [마디6] ...
          ├──── 윈도우 1 ─────┤
                 ├──── 윈도우 2 ─────┤
                        ├──── 윈도우 3 ─────┤
```

각 윈도우에서:

1. **피치 클래스 수집:** 윈도우 내 모든 코드의 구성음을 피치 클래스 집합으로 모은다
2. **로컬 키 근음 결정:** 토니시제이션이나 ii-V-I의 I 코드가 있으면 그 근음을 사용, 없으면 곡의 키 근음
3. **각 모드에 점수 매기기:**

```
scoreMode(피치클래스집합, 근음, 모드음계):
    overlap = 피치클래스 중 모드 음계에 포함되는 수
    outside = 피치클래스 중 모드 음계에 포함되지 않는 수
    
    점수 = (overlap - outside × 2) / max(전체 피치클래스 수, 1)
```

> 💡 음계에 포함되지 않는 음에 **2배 페널티**를 줍니다. 하나라도 벗어나면 점수가 확 떨어집니다.

4. **가장 높은 점수의 모드** 선택 (임계값 0.55 이상이어야 함)
5. 아직 모드가 지정되지 않은 코드들에 해당 모드를 할당

### 2.3 결과

```
마디 1~4: Dm7 → G7 → Cmaj7 → Cmaj7
  → 대부분 C장조 음계에 맞음 → modeSegment = "ionian"

마디 5~8: Cm7 → Fm7 → Bb7 → Ebmaj7
  → C 에올리안/도리안 쪽 → modeSegment = "dorian" 또는 "aeolian"
```

모드가 결정되지 않은 코드는 키의 기본 모드(장조→ionian, 단조→aeolian)가 할당됩니다.

---

## 3. TonicizationModulationDetector – 조성화/전조 감지

**파일:** `service/implementation/TonicizationModulationDetector.java`  
**역할:** ii-V-I 그룹을 기반으로 **조성화(tonicization)** 인지 **전조(modulation)** 인지 판별

### 3.1 조성화 vs 전조

| | 조성화 (Tonicization) | 전조 (Modulation) |
|--|----------------------|-------------------|
| **정의** | 잠깐 다른 키를 암시 | 오래 동안 새 키에 머무름 |
| **기간** | 짧음 (1~5마디) | 김 (6마디 이상) |
| **케이던스 수** | 1~2회 | 2회 이상 |
| **예시** | C장조에서 A7→Dm7 (잠깐 D로) | C장조 → G장조로 완전히 전환 |

### 3.2 판별 기준

```
기본 입력: 원래 키가 아닌 키를 타겟으로 하는 ii-V-I 그룹들

같은 타겟 키를 가진 그룹들을 모아서:
  nComplete = 완전한(불완전이 아닌) ii-V-I 수
  span = 첫 번째 ~ 마지막 멤버의 마디 수
  anyDiatonicTarget = 타겟이 원래 키의 다이어토닉인지

판별 규칙:
  1. 다이어토닉 타겟이면:
     → "tonicization" (확신도 = 0.5 + 0.15 × 케이던스 수, 최대 0.8)
     
  2. 불완전만 있으면:
     → "tonicization" (확신도 0.4)
     
  3. 완전한 케이던스 ≥ 2 AND 기간 ≥ 6마디:
     → "modulation" (확신도 = 0.5 + 0.1 × 완전 수 + 0.03 × 기간, 최대 0.9)
     
  4. 그 외:
     → "tonicization" (확신도 = 0.4 + 0.2 × 케이던스 수, 최대 0.8)
```

### 3.3 감지 예시

```
C장조 곡에서:

마디 5: Em7 → 마디 6: A7 → 마디 7: Dm7  (ii-V-I to D)
마디 9: Em7 → 마디 10: A7 → 마디 11: Dm7  (ii-V-I to D)

타겟 키 = "D", 완전한 케이던스 = 2, 기간 = 7마디
→ 2 ≥ 2 AND 7 ≥ 6 → "modulation"!
→ confidence = 0.5 + 0.1×2 + 0.03×7 = 0.91 → min(0.9) = 0.9
```

### 3.4 결과

ii-V-I 그룹 멤버 코드들에 `tonicization` 정보가 기록됩니다:

```
tonicization = {
  type: "modulation",
  temporaryKey: "D",
  startBar: 5,
  endBar: 11,
  evidence: ["ii-V-I(standard) to D at bars 5-7", "ii-V-I(standard) to D at bars 9-11"],
  confidence: 0.9
}
```

---

## 4. SectionBoundaryDetector – 섹션 경계 감지

**파일:** `service/implementation/SectionBoundaryDetector.java`  
**역할:** 곡을 **키와 모드가 같은 구간(섹션)** 으로 분할

### 4.1 섹션이란?

곡에서 같은 키와 분위기를 유지하는 연속된 구간입니다.

```
곡 전체:
[C장조 ionian] → [G장조 ionian] → [C장조 dorian] → [C장조 ionian]
    섹션 1            섹션 2           섹션 3            섹션 4
```

### 4.2 감지 과정 (6단계)

#### Step 1: 마디별 키 결정

각 마디의 유효 키를 결정합니다:
- 기본값: 원래 곡의 키
- 전조(modulation) 코드가 있는 마디: 해당 전조의 temporaryKey로 변경

#### Step 2: 마디별 모드 결정

각 마디에 속한 코드들의 `modeSegment`에서 **가장 빈도가 높은 모드**를 선택합니다.

#### Step 3: 섹션 생성

키가 바뀌는 지점에서 섹션을 나눕니다:

```
마디 1: key=C  ─┐
마디 2: key=C   │→ 섹션 1 (C, bars 1-4)
마디 3: key=C   │
마디 4: key=C  ─┘
마디 5: key=G  ─┐
마디 6: key=G   │→ 섹션 2 (G, bars 5-8)
마디 7: key=G   │
마디 8: key=G  ─┘
```

#### Step 4: 인접한 같은 키 섹션 병합

키가 같은 인접 섹션은 합칩니다.

#### Step 5: 짧은 섹션 흡수

2마디 이하의 짧은 섹션은 이전 섹션에 흡수됩니다 (너무 짧은 "전조"는 의미 없음).

#### Step 6: 조성화 어노테이션

각 섹션 안에서 조성화(tonicization) 코드가 있으면, 해당 정보를 섹션에 기록합니다.

### 4.3 결과 예시

```json
[
  {
    "start_bar": 1,
    "end_bar": 16,
    "key": "C",
    "mode": "ionian",
    "type": "original_key",
    "tonicizations": ["D", "G"]
  },
  {
    "start_bar": 17,
    "end_bar": 24,
    "key": "Ab",
    "mode": "ionian",
    "type": "modulation"
  },
  {
    "start_bar": 25,
    "end_bar": 32,
    "key": "C",
    "mode": "ionian",
    "type": "original_key"
  }
]
```

> 💡 이 결과를 보면: "이 곡은 C장조로 시작해서, 중간에 Ab장조로 전조했다가, 다시 C장조로 돌아온다. 
> 그리고 첫 섹션 안에서 D와 G 방향으로 잠깐 조성화가 일어났다"는 것을 알 수 있습니다.

