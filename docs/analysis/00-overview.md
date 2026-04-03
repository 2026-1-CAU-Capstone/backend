# 🎷 Jazzify 화성 분석 엔진 – 전체 개요

## 이 문서는 무엇인가요?

Jazzify의 **화성 분석(Harmonic Analysis) 엔진**은 재즈 코드 진행(chord progression)을 입력받아, 각 코드의 **음악 이론적 의미**를 자동으로 분석해주는 시스템입니다.

> 💡 **쉽게 말하면:** "Dm7 → G7 → Cmaj7"이라는 코드 진행을 넣으면, "이건 C장조에서의 ii-V-I 진행이고, Dm7은 서브도미넌트 기능, G7은 도미넌트 기능, Cmaj7은 토닉 기능이야"라고 알려주는 시스템입니다.

---

## 📁 패키지 구조

```
analysis/
├── config/                          # 설정 데이터 (화성 기능 맵, 모달 인터체인지 데이터)
│   └── AnalysisConfigData.java
│
├── controller/                      # REST API 진입점
│   ├── AnalysisController.java      # POST /v1/analysis 엔드포인트
│   └── AnalysisControllerSpec.java  # Swagger 문서용 인터페이스
│
├── dto/
│   └── request/
│       └── AnalysisRequest.java     # 분석 요청 DTO (text, key, title, timeSignature)
│
├── model/                           # 내부 도메인 모델 (DB 미사용)
│   ├── SongInput.java               # 곡 전체 입력 정보
│   ├── ChordEntry.java              # 입력된 개별 코드 항목
│   ├── ParsedChord.java             # ⭐ 핵심! 분석 결과가 점진적으로 채워지는 코드 객체
│   ├── FunctionEntry.java           # 화성 기능 (T/SD/D) 항목
│   ├── GroupMembership.java         # ii-V-I 그룹 소속 정보
│   ├── SecondaryDominantInfo.java   # 세컨더리 도미넌트 정보
│   ├── ChromaticApproachInfo.java   # 반음계적 접근 정보
│   ├── DeceptiveResolutionInfo.java # 기만 종지 정보
│   ├── PedalInfo.java               # 페달 포인트 정보
│   ├── ModalInterchangeInfo.java    # 모달 인터체인지(차용 화음) 정보
│   ├── TonicizationInfo.java        # 조성화/전조 정보
│   └── AmbiguityFlag.java           # 모호성 플래그
│
├── service/
│   ├── HarmonicAnalysisService.java # ⭐ 메인 파이프라인 오케스트레이터
│   └── implementation/              # 파이프라인의 각 단계별 구현체
│       ├── ChordSymbolParser.java       # 코드 기호 파싱
│       ├── ChordNormalizer.java         # 코드 품질 정규화
│       ├── DiatonicClassifier.java      # 다이어토닉 분류
│       ├── FunctionLabeler.java         # 화성 기능 라벨링
│       ├── IiViDetector.java            # ii-V-I 패턴 감지
│       ├── TritoneSubDetector.java      # 트라이톤 대리 감지
│       ├── SecondaryDominantDetector.java # 세컨더리 도미넌트 감지
│       ├── DiminishedClassifier.java    # 감화음 분류
│       ├── ChromaticApproachDetector.java # 반음계적 접근 감지
│       ├── DeceptiveResolutionDetector.java # 기만 종지 감지
│       ├── PedalPointDetector.java      # 페달 포인트 감지
│       ├── ModalInterchangeDetector.java # 모달 인터체인지 감지
│       ├── ModeSegmentDetector.java     # 모드 세그먼트 감지
│       ├── TonicizationModulationDetector.java # 조성화/전조 감지
│       ├── SectionBoundaryDetector.java # 섹션 경계 감지
│       ├── AmbiguityScorer.java         # 모호성 점수 계산
│       └── AnalysisAggregator.java      # 최종 결과 집계
│
└── util/
    └── NoteUtils.java               # 음표/키 유틸리티 (피치 클래스 변환 등)
```

---

## 🔄 분석 파이프라인 요약

분석은 **6단계 파이프라인**으로 진행됩니다:

```
[입력 텍스트] ──────────────────────────────────────────────────────────────
    │
    ▼ Phase 1: 파싱 (Parse)
    │  ChordSymbolParser: 텍스트 → ParsedChord 리스트
    │  // 정규식으로 "Dm7" 같은 코드 기호를 근음(D)+품질(min7)+텐션+베이스로 분해
    │  // "|"로 마디를 나누고, 마디 내 코드 수에 따라 박 위치·지속시간을 균등 분배
    │
    ▼ Phase 2: Layer 1 – 개별 코드 분석
    │  ChordNormalizer   → 코드 품질 정규화 (9th/11th/13th → 핵심 7th 품질로 통일)
    │  DiatonicClassifier → 코드 구성음이 모두 키 음계에 속하는지 확인 + 디그리 부여
    │  FunctionLabeler   → 설정 맵 참조하여 T/SD/D 기능 부여 (다이어토닉→키맵, 비→크로매틱맵)
    │
    ▼ Phase 3: Layer 2 – 문맥 패턴 감지
    │  IiViDetector              → V 코드 기준 앞뒤 탐색으로 ii-V-I + 5가지 변형 감지
    │  FunctionLabeler.labelFromGroups → ii-V-I 역할 기반으로 빈 기능 보완
    │  TritoneSubDetector        → dom7이 반음 하행 해결하면(interval=11) 트라이톤 대리
    │  SecondaryDominantDetector → 비다이어토닉 dom7의 완전4도 위 타겟 계산 → V/ii, V/vi 등
    │  DiminishedClassifier      → dim의 전후 관계로 경과음/보조음/도미넌트기능 분류
    │  ChromaticApproachDetector → 미설명 비다이어토닉이 반음 미끄러짐이면 감지
    │  DeceptiveResolutionDetector → dom7의 기대해결(완전4도 위)과 실제 비교 → 기만 종지
    │  PedalPointDetector        → 같은 베이스가 2마디 이상 지속되면 페달 포인트
    │
    ▼ Phase 4: Layer 3 – 구조 분석
    │  ModalInterchangeDetector       → 설정 데이터의 모드별 (interval,quality) 매칭
    │  ModeSegmentDetector            → 4마디 윈도우 스코어링 (overlap-outside×2)
    │  TonicizationModulationDetector → 케이던스 수+기간으로 조성화 vs 전조 판별
    │  SectionBoundaryDetector        → 6단계: 키 변경점 분할→병합→흡수→조성화 어노테이션
    │
    ▼ Phase 5: 모호성 채점
    │  AmbiguityScorer → 5가지 하위 점수(기능/다이어토닉/그룹/경쟁/플래그)의 가중 합
    │
    ▼ Phase 6: 집계
    │  AnalysisAggregator → song/chords/groups/sections/ambiguity_stats로 구조화
    │
    ▼
[분석 결과 JSON] ──────────────────────────────────────────────────────────
```

---

## 🎵 음악 이론 기초 용어 해설

이 문서들을 이해하기 위해 알아야 할 기본 용어:

| 용어 | 영문 | 설명 |
|------|------|------|
| **피치 클래스** | Pitch Class | 옥타브를 무시한 음 이름. C=0, C#=1, D=2, ..., B=11 (총 12개) |
| **스케일 디그리** | Scale Degree | 키(조성) 안에서 몇 번째 음인지. I, ii, iii, IV, V, vi, vii |
| **다이어토닉** | Diatonic | 해당 키의 음계에 자연스럽게 속하는 코드 |
| **토닉 (T)** | Tonic | "집"처럼 안정감을 주는 기능 (I, iii, vi) |
| **서브도미넌트 (SD)** | Subdominant | 토닉에서 벗어나려는 기능 (ii, IV) |
| **도미넌트 (D)** | Dominant | 토닉으로 돌아가고 싶은 긴장감 (V, vii) |
| **ii-V-I** | ii-V-I | 재즈의 가장 기본적인 코드 진행 패턴 |
| **세컨더리 도미넌트** | Secondary Dominant | 일시적으로 다른 코드를 토닉처럼 만드는 도미넌트 코드 |
| **트라이톤 대리** | Tritone Substitution | 도미넌트 코드를 반음 위의 도미넌트로 대체하는 기법 |
| **모달 인터체인지** | Modal Interchange | 같은 으뜸음의 다른 모드에서 코드를 빌려오는 것 |
| **전조** | Modulation | 곡의 조성(키)이 바뀌는 것 |
| **조성화** | Tonicization | 일시적으로 다른 키를 암시하는 것 (전조보다 짧음) |

---

## 📚 문서 목차

| 파일 | 내용 |
|------|------|
| [01-models.md](./01-models.md) | 데이터 모델 상세 설명 |
| [02-utils-and-config.md](./02-utils-and-config.md) | 유틸리티와 설정 데이터 |
| [03-layer1-parsing-and-classification.md](./03-layer1-parsing-and-classification.md) | Phase 1~2: 파싱, 정규화, 분류, 기능 라벨링 |
| [04-layer2-pattern-detection.md](./04-layer2-pattern-detection.md) | Phase 3: 문맥 패턴 감지 (ii-V-I, 세컨더리 도미넌트 등) |
| [05-layer3-structural-analysis.md](./05-layer3-structural-analysis.md) | Phase 4: 구조 분석 (모달 인터체인지, 전조 등) |
| [06-ambiguity-and-aggregation.md](./06-ambiguity-and-aggregation.md) | Phase 5~6: 모호성 채점 & 최종 집계 |

---

## 🚀 API 사용 예시

```http
POST /v1/analysis
Content-Type: application/json

{
  "text": "Dm7 | G7 | Cmaj7 | Cmaj7",
  "key": "C",
  "title": "My Song",
  "timeSignature": "4/4"
}
```

응답은 `ApiResponse<Map<String, Object>>` 형태로, 각 코드의 상세 분석 결과가 포함됩니다.

