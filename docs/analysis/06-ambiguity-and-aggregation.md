# 📊 Phase 5~6: 모호성 채점 & 최종 집계

> 이 문서는 분석 파이프라인의 **마지막 두 단계**를 설명합니다.
> Phase 5에서 각 코드의 해석 확신도를 점수화하고, Phase 6에서 모든 결과를 JSON으로 합칩니다.

---

## 목차

1. [AmbiguityScorer – 모호성 점수 계산](#1-ambiguityscorer--모호성-점수-계산)
2. [AnalysisAggregator – 최종 결과 집계](#2-analysisaggregator--최종-결과-집계)

---

## 1. AmbiguityScorer – 모호성 점수 계산

**파일:** `service/implementation/AmbiguityScorer.java`  
**역할:** 각 코드의 분석이 **얼마나 확실한지/불확실한지** 0.0~1.0 사이의 점수로 계산

### 1.1 왜 모호성 점수가 필요한가?

음악은 수학이 아닙니다. 같은 코드도 문맥에 따라 여러 가지로 해석될 수 있습니다.

```
C장조에서 Ab7:
- 해석 1: bVI7 (모달 인터체인지, 에올리안에서 빌림)
- 해석 2: V/Db (Db로의 세컨더리 도미넌트)
- 해석 3: Tritone sub of D7 (D7의 트라이톤 대리)

→ 이 코드의 모호성 점수는 높을 것!
```

vs.

```
C장조에서 Cmaj7:
- 해석: I (토닉)

→ 이 코드의 모호성 점수는 0에 가까울 것!
```

### 1.2 점수 계산 공식

최종 점수는 **5가지 하위 점수의 가중 합**입니다:

```
ambiguityScore = W_FUNCTION  × funcAmb       (0.30)
               + W_DIATONIC  × diaAmb        (0.15)
               + W_GROUP     × grpAmb         (0.20)
               + W_COMPETING × compAmb        (0.20)
               + W_FLAGS     × flagAmb        (0.15)
```

| 하위 점수 | 가중치 | 측정 대상 |
|-----------|--------|-----------|
| funcAmb | 30% | 화성 기능의 불확실성 |
| diaAmb | 15% | 다이어토닉 여부 |
| grpAmb | 20% | 패턴으로 설명 가능한지 |
| compAmb | 20% | 경쟁하는 해석의 수 |
| flagAmb | 15% | 모호성 플래그 수 |

### 1.3 각 하위 점수 상세

#### (1) functionAmbiguity – 화성 기능 불확실성 (가중치 30%)

기능이 하나만 있고 확신도가 높을수록 낮은 점수(= 확실함):

```
Case 1: 기능 없음 → 1.0 (완전 불확실)

Case 2: 기능 1개
  → 1.0 - confidence
  예: [T(1.0)] → 1.0 - 1.0 = 0.0 (완전 확실)
  예: [D(0.6)] → 1.0 - 0.6 = 0.4 (좀 불확실)

Case 3: 기능 2개 이상 (경쟁 해석)
  상위 2개의 확신도 비율 + 낮은 확신도 페널티:
  ratio = 2nd_confidence / 1st_confidence
  lowConfPenalty = (1.0 - top) × 0.3
  점수 = ratio × 0.7 + lowConfPenalty
  
  예: [T(0.7), SD(0.3)]
      ratio = 0.3/0.7 ≈ 0.43
      penalty = (1.0-0.7)×0.3 = 0.09
      점수 = 0.43×0.7 + 0.09 ≈ 0.39
```

> 💡 두 해석의 확신도가 비슷할수록(ratio→1.0) 모호성이 높아집니다.

#### (2) diatonicAmbiguity – 다이어토닉 불확실성 (가중치 15%)

| 상태 | 점수 | 이유 |
|------|------|------|
| 다이어토닉 ✅ | 0.0 | 키에 자연스럽게 속함, 확실 |
| 다이어토닉 ❌ | 0.6 | 키에 속하지 않음, 설명이 필요 |
| 판별 불가 | 0.5 | 정보 부족 |

#### (3) groupAmbiguity – 패턴 설명 가능성 (가중치 20%)

비다이어토닉 코드가 **어떤 패턴으로든 설명되었는지** 확인:

```
설명 가능 = 다음 중 하나라도 있으면:
  - groupMemberships (ii-V-I 소속)
  - secondaryDominant (세컨더리 도미넌트)
  - modalInterchange (모달 인터체인지)
  - diminishedFunction (감화음 기능)
  - chromaticApproach (반음계적 접근)
  - deceptiveResolution (기만 종지)
  - pedalInfo (페달 포인트)
```

| 상태 | 점수 |
|------|------|
| 다이어토닉 | 0.0 (설명 불필요) |
| 비다이어토닉 + 설명됨 | 0.1 (패턴으로 잘 설명됨) |
| 비다이어토닉 + 미설명 | 0.9 (왜 여기 있는지 모름!) |

#### (4) competingInterpretations – 경쟁 해석 수 (가중치 20%)

하나의 코드에 **동시에 붙은 분석 레이어**가 많을수록 모호:

```
layers 수 계산 (각각 있으면 +1):
  - secondaryDominant
  - modalInterchange
  - tonicization
  - chromaticApproach
  - deceptiveResolution
```

| 레이어 수 | 점수 |
|-----------|------|
| 0~1 | 0.0 |
| 2 | 0.4 |
| 3+ | 0.7 |

> 💡 "세컨더리 도미넌트이면서 동시에 모달 인터체인지이기도 하다" → 어떤 해석이 맞는지 모호!

#### (5) flagAmbiguity – 플래그 기반 (가중치 15%)

```
점수 = min(1.0, 0.5 × 플래그 수 + 0.3 × contextNeeded 수)
```

| 상태 | 점수 |
|------|------|
| 플래그 없음 | 0.0 |
| 플래그 1개 (context 불필요) | 0.5 |
| 플래그 1개 (context 필요) | 0.8 |
| 플래그 2개 | 1.0 |

### 1.4 점수 해석 가이드

| 점수 범위 | 의미 | 예시 |
|-----------|------|------|
| **0.0 ~ 0.1** | 매우 확실 | 다이어토닉 I, IV, V 코드 |
| **0.1 ~ 0.3** | 꽤 확실 | ii-V-I 패턴의 코드, 흔한 모달 인터체인지 |
| **0.3 ~ 0.5** | 약간 모호 | 세컨더리 도미넌트, 해결되지 않은 비다이어토닉 |
| **0.5 ~ 0.7** | 상당히 모호 | 여러 해석이 경쟁하는 코드 |
| **0.7 ~ 1.0** | 매우 모호 | 어떤 패턴으로도 설명되지 않는 비다이어토닉 코드 |

### 1.5 계산 예시

**Cmaj7 (C장조의 I):**
```
funcAmb = 0.0    (T(1.0) → 1.0-1.0 = 0.0)
diaAmb  = 0.0    (다이어토닉)
grpAmb  = 0.0    (다이어토닉이므로)
compAmb = 0.0    (경쟁 없음)
flagAmb = 0.0    (플래그 없음)

score = 0.30×0 + 0.15×0 + 0.20×0 + 0.20×0 + 0.15×0 = 0.0 ✨
```

**Bb7 (C장조의 bVII, 백도어 도미넌트):**
```
funcAmb ≈ 0.39   ([SD(0.6), D(0.3)] → ratio=0.5, penalty=0.12)
diaAmb  = 0.6    (비다이어토닉)
grpAmb  = 0.1    (모달 인터체인지로 설명됨)
compAmb = 0.0    (1개 레이어만)
flagAmb = 0.0    (플래그 없음)

score = 0.30×0.39 + 0.15×0.6 + 0.20×0.1 + 0.20×0 + 0.15×0
      = 0.117 + 0.09 + 0.02 = 0.227
      → 반올림: 0.227 (꽤 확실한 편)
```

---

## 2. AnalysisAggregator – 최종 결과 집계

**파일:** `service/implementation/AnalysisAggregator.java`  
**역할:** 모든 분석 결과를 **하나의 JSON 응답용 Map**으로 합치기

### 2.1 출력 구조

```json
{
  "song": {
    "title": "My Song",
    "key": "C",
    "time_signature": "4/4"
  },
  
  "chords": [
    {
      "bar": 1,
      "beat": 1.0,
      "symbol": "Dm7",
      "duration_beats": 2.0,
      "analysis": {
        "root": 2,
        "root_name": "D",
        "quality": "min7",
        "normalized_quality": "min7",
        "tensions": [],
        "bass": null,
        "bass_name": null,
        "degree": "ii",
        "is_diatonic": true,
        "functions": [{"function": "SD", "confidence": 1.0, "note": null}],
        "secondary_dominant": null,
        "group_memberships": [{"groupId": 1, "groupType": "ii-V-I", "role": "ii", "variant": "standard"}],
        "diminished_function": null,
        "chromatic_approach": null,
        "deceptive_resolution": null,
        "pedal_info": null,
        "modal_interchange": null,
        "mode_segment": "ionian",
        "tonicization": null,
        "ambiguity_flags": [],
        "ambiguity_score": 0.0
      }
    }
    // ... 나머지 코드들
  ],
  
  "groups": [
    {
      "group_id": 1,
      "group_type": "ii-V-I",
      "variant": "standard",
      "target_key": "C",
      "is_diatonic_target": true,
      "members": [...],
      "notes": "Diatonic ii-V-I in C"
    }
  ],
  
  "sections": [
    {
      "start_bar": 1,
      "end_bar": 32,
      "key": "C",
      "mode": "ionian",
      "type": "original_key"
    }
  ],
  
  "ambiguity_stats": {
    "total_chords": 32,
    "high_confidence_count": 28,
    "high_confidence_pct": 87.5,
    "ambiguous_count": 2,
    "ambiguous_pct": 6.3,
    "mean_score": 0.087,
    "max_score": 0.453
  },
  
  "engine_version": "0.1.0",
  
  "coverage": [
    "diatonic_classification",
    "scale_degree_calculation",
    "T_SD_D_function_labeling",
    "chord_normalization",
    "ii-V-I_detection",
    "tritone_substitution_detection",
    "secondary_dominant_detection",
    "diminished_chord_classification",
    "chromatic_approach_detection",
    "deceptive_resolution_detection",
    "pedal_point_detection",
    "modal_interchange_detection",
    "mode_segment_detection",
    "tonicization_modulation_detection",
    "section_boundary_detection",
    "ambiguity_scoring"
  ]
}
```

### 2.2 각 섹션 설명

#### song

곡의 메타 정보입니다.

#### chords

모든 코드의 **위치 정보 + 상세 분석 결과**입니다. 각 코드는:
- **위치 정보:** `bar`, `beat`, `symbol`, `duration_beats`
- **분석 결과 (analysis):** Layer 1~3의 모든 분석 결과 + 모호성 점수

#### groups

감지된 ii-V-I 그룹 목록입니다. 그룹에 속한 코드들의 역할과 변형 정보가 포함됩니다.

#### sections

곡의 구간 분할 결과입니다. 각 구간의 키, 모드, 타입(원래 키/전조)이 표시됩니다.

#### ambiguity_stats

전체 코드에 대한 **모호성 통계 요약**:

| 항목 | 설명 |
|------|------|
| `total_chords` | 전체 코드 수 |
| `high_confidence_count` | 모호성 ≤ 0.1인 코드 수 |
| `high_confidence_pct` | 높은 확신도 비율 (%) |
| `ambiguous_count` | 모호성 > 0.3인 코드 수 |
| `ambiguous_pct` | 모호한 코드 비율 (%) |
| `mean_score` | 평균 모호성 점수 |
| `max_score` | 최대 모호성 점수 |

> 💡 이 통계를 보면 "이 곡의 87.5%는 확실하게 분석됐고, 6.3%만 모호하다"처럼 전체적인 분석 품질을 한 눈에 파악할 수 있습니다.

#### engine_version

분석 엔진의 버전 (`"0.1.0"`). 결과 비교나 디버깅에 사용됩니다.

#### coverage

이 엔진이 수행하는 **16가지 분석 항목** 목록입니다.

---

## 🎯 전체 파이프라인 요약 (처음부터 끝까지)

```
"Dm7 G7 | Cmaj7" + key="C"
    │
    ▼ ChordSymbolParser
    │  // 정규식으로 "Dm7"→근음D(2)+품질min7, "G7"→근음G(7)+품질dom7 등으로 분해
    │  // "|"로 마디 분할 → 마디1에 코드2개 → 각 2박씩 배분
    [ParsedChord(Dm7, root=2, quality=min7),
     ParsedChord(G7, root=7, quality=dom7),
     ParsedChord(Cmaj7, root=0, quality=maj7)]
    │
    ▼ ChordNormalizer
    │  // CORE_QUALITY_MAP에서 품질 조회 → min7→min7, dom7→dom7 (이미 핵심 품질)
    [normalizedQuality = min7, dom7, maj7]
    │
    ▼ DiatonicClassifier
    │  // 각 코드 구성음이 C장조 음계{0,2,4,5,7,9,11}에 모두 포함되는지 확인
    │  // interval(0,2)=2→"ii", interval(0,7)=7→"V", interval(0,0)=0→"I"
    [degree=ii/isDia=T, degree=V/isDia=T, degree=I/isDia=T]
    │
    ▼ FunctionLabeler
    │  // major_key 맵에서 "ii"→SD(1.0), "V"→D(1.0), "I"→T(1.0) 조회
    [functions=[SD(1.0)], [D(1.0)], [T(1.0)]]
    │
    ▼ IiViDetector
    │  // G7(dom7)을 V 후보로 → 타겟 I 근음 = mod12(7-7)=0=C
    │  // 다음 코드 Cmaj7(root=0) == 타겟 → I 발견!
    │  // 이전 코드 Dm7(root=2) == mod12(0+2)=2 → ii 발견!
    Group #1: ii-V-I standard targeting C (diatonic)
    [groupMemberships 추가]
    │
    ▼ TritoneSubDetector → (G7→Cmaj7은 반음 하행이 아님, 해당 없음)
    ▼ SecondaryDominantDetector → (G7은 다이어토닉 V이므로 건너뜀)
    ▼ DiminishedClassifier → (감화음 없음)
    ▼ ChromaticApproachDetector → (모든 코드가 다이어토닉, 건너뜀)
    ▼ DeceptiveResolutionDetector → (G7→Cmaj7: 기대 근음=mod12(7+5)=0=C, 일치→정상 해결)
    ▼ PedalPointDetector → (2마디밖에 안 되지만 코드 3개, 근음이 모두 다름→페달 아님)
    │
    ▼ ModalInterchangeDetector → (모두 다이어토닉이므로 건너뜀)
    ▼ ModeSegmentDetector → (C장조 음계와 거의 100% 일치 → ionian)
    ▼ TonicizationModulationDetector → (원래 키 C만 타겟, 건너뜀)
    ▼ SectionBoundaryDetector → [Section: bars 1-2, key=C, mode=ionian]
    │
    ▼ AmbiguityScorer
    │  // Dm7: funcAmb=0(SD 1.0), diaAmb=0, grpAmb=0, compAmb=0, flagAmb=0 → 0.0
    │  // G7:  funcAmb=0(D 1.0),  diaAmb=0, grpAmb=0, compAmb=0, flagAmb=0 → 0.0
    │  // Cmaj7: funcAmb=0(T 1.0), diaAmb=0, grpAmb=0, compAmb=0, flagAmb=0 → 0.0
    [0.0, 0.0, 0.0]  ← 모든 코드가 매우 확실!
    │
    ▼ AnalysisAggregator
    │  // song + chords(3개) + groups(1개) + sections(1개) + ambiguity_stats 조립
    → 최종 JSON 출력
```

이 간단한 예시에서는 모든 코드가 깔끔하게 분석됩니다.
실제 재즈 스탠다드곡에서는 세컨더리 도미넌트, 트라이톤 대리, 모달 인터체인지 등이
풍부하게 나타나서, 각 분석기의 진가가 발휘됩니다! 🎷

