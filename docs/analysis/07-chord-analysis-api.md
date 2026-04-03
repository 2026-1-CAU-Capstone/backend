# Chord Analysis API – 엔티티 관계도 & 변경 사항 정리

## 엔티티 관계도 (ER Diagram)

```
┌─────────────────────┐
│        User         │
│─────────────────────│
│  id (PK)            │
│  publicId (UUID)    │
│  username           │
│  ...                │
└────────┬────────────┘
         │ 1
         │
         │ N
┌────────▼────────────────────────────────────────┐
│                  ChordProject                    │
│──────────────────────────────────────────────────│
│  id (PK)                                         │
│  publicId (UUID)                                 │
│  title                                           │
│  keySignature (MusicKey enum)                    │
│  timeSignature (String, e.g. "4/4")     [NEW]    │
│  user_id (FK → User)                             │
│  session_id (FK → Session, nullable)             │
│  ── 분석 메타데이터 ──                     [NEW]    │
│  lastAnalyzedAt (nullable)                       │
│  totalChords (nullable)                          │
│  highConfidenceCount (nullable)                  │
│  ambiguousCount (nullable)                       │
│  meanAmbiguityScore (nullable)                   │
│  maxAmbiguityScore (nullable)                    │
└──┬──────────┬──────────────┬─────────────────────┘
   │ 1        │ 1            │ 1
   │          │              │
   │ N        │ N            │ N
   ▼          ▼              ▼
┌──────────┐ ┌────────────┐ ┌───────────────────────┐
│ChordInfo │ │ ChordGroup │ │    ChordSection        │
│          │ │            │ │                       │
└────┬─────┘ └─────┬──────┘ └───────────────────────┘
     │ 1           │ 1
     │             │
     │ 1           │ N
     ▼             ▼
┌──────────────┐ ┌──────────────────┐
│ChordAnalysis │ │ChordGroupMember  │
│              │ │                  │
└──────────────┘ └──────────────────┘
```

### 상세 관계

```
User           1 ── N   ChordProject
ChordProject   1 ── N   ChordInfo           (cascade ALL, orphanRemoval)
ChordProject   1 ── N   ChordGroup
ChordProject   1 ── N   ChordSection
ChordInfo      1 ── 1   ChordAnalysis       (cascade ALL, orphanRemoval)
ChordGroup     1 ── N   ChordGroupMember    (cascade ALL, orphanRemoval)
ChordGroupMember N ── 1  ChordInfo           (FK)
```

---

## 엔티티 상세

### ChordProject (수정됨)

| 컬럼 | 타입 | 설명 | 변경 |
|---|---|---|---|
| id | Long (PK) | 내부 PK | 기존 |
| publicId | UUID | 외부 식별자 | 기존 |
| title | String | 곡 제목 | 기존 |
| keySignature | MusicKey (enum) | 곡의 키 | 기존 |
| timeSignature | String | 박자 (e.g. "4/4") | **신규** |
| user_id | FK → User | 소유자 | 기존 |
| session_id | FK → Session | 세션 (nullable) | 기존 |
| lastAnalyzedAt | LocalDateTime | 마지막 분석 시각 | **신규** |
| totalChords | Integer | 전체 코드 수 | **신규** |
| highConfidenceCount | Integer | 높은 확신도 코드 수 | **신규** |
| ambiguousCount | Integer | 모호한 코드 수 | **신규** |
| meanAmbiguityScore | Double | 평균 모호성 점수 | **신규** |
| maxAmbiguityScore | Double | 최대 모호성 점수 | **신규** |

### ChordInfo (수정됨 – 분석 필드 분리)

| 컬럼 | 타입 | 설명 | 변경 |
|---|---|---|---|
| id | Long (PK) | 내부 PK | 기존 |
| publicId | UUID | 외부 식별자 | 기존 |
| chord | String (nullable) | 코드 심볼 (e.g. "Dm7") | 기존 |
| bar | int | 마디 번호 | **신규** (sortOrder 대체) |
| beat | double | 마디 내 박 위치 | **신규** |
| durationBeats | double | 지속 박수 | **신규** |
| chord_project_id | FK → ChordProject | | 기존 |
| sheet_project_id | FK → SheetProject | nullable | 기존 |
| session_id | FK → Session | nullable (기존 NOT NULL → nullable로 변경) | 수정 |

> ❌ **제거됨**: `ChordLength` enum, `sortOrder`, 모든 분석 필드 (degree, isDiatonic 등)
> ✅ **추가됨**: `analysis` (1:1 → ChordAnalysis, cascade)

### ChordAnalysis (신규 – ChordInfo와 1:1)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | Long (PK) | 내부 PK |
| publicId | UUID | 외부 식별자 |
| chord_info_id | FK → ChordInfo (UNIQUE) | 1:1 대응 |
| **Layer 1** | | |
| degree | String(30) | 스케일 디그리 (e.g. "ii", "V", "I") |
| isDiatonic | Boolean | 다이어토닉 여부 |
| normalizedQuality | String(30) | 정규화된 코드 품질 |
| functions | JSON | 화성 기능 배열 [{function, confidence, note}] |
| **Layer 2** | | |
| secondaryDominant | JSON | 세컨더리 도미넌트 정보 |
| diminishedFunction | String(30) | 감화음 기능 (passing/auxiliary/dominant_function) |
| chromaticApproach | JSON | 반음계적 접근 정보 |
| deceptiveResolution | JSON | 기만 종지 정보 |
| pedalInfo | JSON | 페달 포인트 정보 |
| **Layer 3** | | |
| modalInterchange | JSON | 모달 인터체인지 정보 |
| modeSegment | String(30) | 모드 세그먼트 (dorian, lydian 등) |
| tonicization | JSON | 조성화/전조 정보 |
| **Ambiguity** | | |
| ambiguityScore | double | 모호성 점수 (0.0 ~ 1.0) |
| ambiguityFlags | JSON | 모호성 플래그 배열 |

### ChordGroup (신규)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | Long (PK) | 내부 PK |
| publicId | UUID | 외부 식별자 |
| chord_project_id | FK → ChordProject | 소속 프로젝트 |
| groupIndex | int | 그룹 순번 |
| groupType | String(30) | 그룹 타입 (e.g. "ii-V-I") |
| variant | String(60) | 변형 (standard, backdoor, tritone_sub 등) |
| targetKey | String(30) | 타겟 키 (e.g. "C") |
| isDiatonicTarget | boolean | 다이어토닉 타겟 여부 |
| notes | TEXT | 설명 노트 |

### ChordGroupMember (신규 – 중간 테이블)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | Long (PK) | 내부 PK |
| publicId | UUID | 외부 식별자 |
| chord_group_id | FK → ChordGroup | 소속 그룹 |
| chord_info_id | FK → ChordInfo | 대응 코드 |
| role | String(60) | 그룹 내 역할 (e.g. "ii", "V", "I") |

### ChordSection (신규)

| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | Long (PK) | 내부 PK |
| publicId | UUID | 외부 식별자 |
| chord_project_id | FK → ChordProject | 소속 프로젝트 |
| startBar | int | 시작 마디 |
| endBar | int | 끝 마디 |
| sectionKey | String(30) | 섹션의 키 |
| sectionType | String(30) | 타입 (original_key, modulation 등) |
| mode | String(30) | 모드 (ionian, dorian 등) |
| tonicizations | JSON | 조성화 정보 |

---

## MusicKey enum 변경

각 enum 상수에 `analysisKey` 필드를 추가하여 분석기 호환 문자열을 제공.

```java
C_MAJOR("C"), D_FLAT_MAJOR("Db"), C_MINOR("Cm"), ...
```

---

## 신규 API 엔드포인트

| Method | URL | 설명 |
|---|---|---|
| `POST` | `/v1/chord-projects/{publicId}/chords` | 코드 정보 등록 (기존 데이터 덮어쓰기) |
| `POST` | `/v1/chord-projects/{publicId}/analyze` | 화성 분석 실행 → 결과 DB 저장 |

### POST /v1/chord-projects/{publicId}/chords

**입력 형식: iRealPro 스타일 문자열**

`|` 로 마디를 구분하고, 한 마디 안에 코드를 공백으로 나열한다.
마디 내 코드 수에 따라 프로젝트의 박자표(`timeSignature`) 기준으로 박자가 균등 분배된다.
**동일 마디 내에서** 연속되는 동일 코드는 하나로 병합된다 (마디 경계를 넘는 경우는 별도 저장).
쉬는 마디는 `N.C.`로 표기한다.

**Request Body:**
```json
{ "progression": "Dm7 G7 | Cmaj7 | Am7 D7 | Gmaj7" }
```

**파싱 결과 (4/4 박자 기준):**
| bar | beat | chord | durationBeats |
|-----|------|-------|---------------|
| 1 | 1.0 | Dm7 | 2.0 |
| 1 | 3.0 | G7 | 2.0 |
| 2 | 1.0 | Cmaj7 | 4.0 |
| 3 | 1.0 | Am7 | 2.0 |
| 3 | 3.0 | D7 | 2.0 |
| 4 | 1.0 | Gmaj7 | 4.0 |

**병합 예시 (동일 마디 내에서만):**
```
"C C D E | C"  →  C 2박(병합), D 1박, E 1박 | C 4박
                   (마디 경계를 넘는 C는 별도 저장)
```

### POST /v1/chord-projects/{publicId}/analyze

**Response (구조):**
```json
{
  "data": {
    "projectPublicId": "...",
    "title": "All The Things You Are",
    "keySignature": "Ab",
    "timeSignature": "4/4",
    "lastAnalyzedAt": "2026-03-31T12:00:00",
    "ambiguityStats": { "totalChords": 36, "highConfidenceCount": 30, ... },
    "chords": [
      {
        "publicId": "...",
        "chord": "Fm7",
        "bar": 1, "beat": 1.0, "durationBeats": 4.0,
        "analysis": {
          "degree": "vi", "isDiatonic": true,
          "normalizedQuality": "min7",
          "functions": [{"function": "T", "confidence": 0.9}],
          "secondaryDominant": null,
          "ambiguityScore": 0.05,
          ...
        }
      }
    ],
    "groups": [ { "groupIndex": 1, "groupType": "ii-V-I", ... } ],
    "sections": [ { "startBar": 1, "endBar": 8, "sectionKey": "Ab", ... } ]
  }
}
```

---

## 분석 실행 흐름

```
1. ChordProject + ChordInfos 로드 (bar/beat 순서 정렬)
2. bar 기준 그룹화 → "Dm7 G7 | Cmaj7 |" 텍스트 재구성
3. HarmonicAnalysisService.analyze(text, key, title, timeSignature) 실행
4. 기존 ChordGroup / ChordSection 삭제 (재분석 지원)
5. 분석 결과 → ChordAnalysis 생성/갱신 (ChordInfo와 1:1, 인덱스 매핑)
6. 그룹 결과 → ChordGroup + ChordGroupMember 저장
7. 섹션 결과 → ChordSection 저장
8. ChordProject 앰비규이티 통계 업데이트
9. 구조화된 AnalysisResultResponse 반환
```

---

## 파일 변경 목록

### 수정된 파일

| 파일 | 변경 내용 |
|---|---|
| `MusicKey.java` | `analysisKey` 필드 추가 |
| `ChordProject.java` | `timeSignature`, 분석 메타데이터 필드 추가 |
| `ChordInfo.java` | 분석 필드 전부 제거 → `ChordAnalysis` 1:1 관계로 분리 |
| `ChordProjectService.java` | `addChords()`, `analyze()` 메서드 추가 |
| `ChordProjectWriter.java` | `create()`에 `timeSignature` 파라미터 추가 |
| `ChordProjectCreateRequest.java` | `timeSignature` 필드 추가 |
| `ChordProjectResponse.java` | `timeSignature` 필드 추가 |
| `ChordProjectMapper.java` | `timeSignature` 반영 |
| `ChordProjectControllerSpec.java` | `addChords`, `analyze` 메서드 추가 |
| `ChordProjectController.java` | 엔드포인트 구현 |
| `ChordProjectErrorCode.java` | `CHORD_PROJECT_NO_CHORDS` 에러코드 추가 |

### 신규 파일

| 파일 | 역할 |
|---|---|
| `ChordAnalysis.java` | 코드별 분석 결과 엔티티 (ChordInfo 1:1) |
| `ChordGroup.java` | ii-V-I 등 그룹 엔티티 |
| `ChordGroupMember.java` | 그룹 ↔ 코드 중간 테이블 |
| `ChordSection.java` | 섹션 경계 엔티티 |
| `ChordInfoRepository.java` | ChordInfo Repository |
| `ChordAnalysisRepository.java` | ChordAnalysis Repository |
| `ChordGroupRepository.java` | ChordGroup Repository |
| `ChordSectionRepository.java` | ChordSection Repository |
| `ChordInfoReader.java` | 조회 전용 Component |
| `ChordInfoWriter.java` | 쓰기 전용 Component |
| `ChordAnalysisWriter.java` | 분석 결과 저장 Component |
| `ChordInfoMapper.java` | DTO ↔ Entity 변환 |
| `AddChordsRequest.java` | 코드 등록 요청 DTO (iRealPro 형식 `progression` 문자열) |
| `IRealProChordParser.java` | iRealPro 형식 문자열 → ChordInfo 리스트 파서 (동일 마디 내 병합 포함) |
| `ChordInfoResponse.java` | 코드 정보 + 분석 결과 응답 DTO |
| `AnalysisResultResponse.java` | 전체 분석 결과 응답 DTO |

### 삭제된 파일

| 파일 | 이유 |
|---|---|
| `ChordLength.java` | 더 이상 사용하지 않는 enum (bar/beat/durationBeats로 대체) |

