```python
{
  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  // 1. IDENTITY — 식별 & 출처
  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  "id": "lk_a1b2c3d4",               // UUID (숫자 대신, 출처 무관하게 유일)
  "source": "user",                   // "user" | "weimar" | "curated"
  "userId": "u_abc123",              // null이면 공개/내장 릭
  "createdAt": "2026-05-04T09:00:00Z",
  "updatedAt": "2026-05-04T09:00:00Z",

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  // 2. PERFORMANCE METADATA — 검색/필터 기준
  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  "performer": "Cannonball Adderley" // 없으면 null
  "title": "Autumn Leaves",
  "album": "Somethin' Else",
  "instrument": "as",                 // as, ts, tp, p, g, b, voc, cl
  // as	Alto Saxophone (알토 색소폰)
ts	Tenor Saxophone (테너 색소폰)
tp	Trumpet (트럼펫)
p	Piano (피아노)
g	Guitar (기타)
b	Bass (베이스)
voc	Vocals (보컬)
cl	Clarinet (클라리넷)

  "style": "HARDBOP",                 // SWING, BEBOP, HARDBOP, COOL, MODAL, FUSION
  "tempo": 160,                       // BPM (null 허용)
  "key": "Bb-maj",                    // Weimar 포맷 그대로
  "rhythmfeel": "SWING",             // SWING, STRAIGHT, BOSSA, LATIN
  "timeSignature": "4/4",

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  // 3. HARMONIC CONTEXT — 화성 맥락
  //    → 유사도 검색 필터 + RAG 컨텍스트에 활용
  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  "chords": ["D-7", "G7", "CMaj7"],          // 이 릭에 등장하는 코드 목록
  "chordsPerNote": ["D-7","D-7","G7","G7","CMaj7"],  // 각 음 아래 코드 (현재 chords_per_event)
  "harmonicContext": "ii-V-I",               // "ii-V-I" | "minor-ii-V" | "blues" | "modal" | "turnaround" | "other"
  "targetChord": "CMaj7",                    // 마지막 해결 코드

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  // 4. SHEET DATA — 악보 렌더링 (VexFlow)
  //    → LickCard, LickCreator 에서 직접 사용
  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  "sheetData": {
    "title": "Cannonball Adderley — Autumn Leaves",
    "composer": "Cannonball Adderley",
    "key": "Bb",
    "timeSignature": "4/4",
    "tempo": 160,
    "measures": [
      {
        "chord": "D-7",
        "notes": [
          { "keys": ["d/5"], "duration": "8" },
          { "keys": ["f/5"], "duration": "8" }
        ]
      }
    ]
  },

  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  // 5. SIMILARITY FEATURES — 유사도 검색 핵심
  //    → 입력값이 제공된 경우 해당 값을 그대로 사용
  //    → 입력값이 없는 경우 1~4번(Identity·Performance·Harmonic·SheetData)
  //       정보를 바탕으로 저장 시 자동 집계·계산 (computeLickFeatures()로)
  //    → Levenshtein distance 기반 검색에 사용
  // ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
  "nEvents": 12,                             // 음표 수 (쉼표 제외) — sheetData.measures에서 자동 집계
  "pitches": [62, 65, 67, 69, 65, 64, 62],  // MIDI 절대 음고 (pitch-exact 검색) — sheetData notes에서 자동 변환
  "intervals": [3, 2, 2, -4, -1, -2],       // 반음 단위 음정 차이 (정확) — pitches에서 자동 계산
  "parsons": [1, 1, 1, -1, -1, -1],         // 선율 윤곽 (1=상행, -1=하행, 0=동일) — intervals에서 자동 계산
  "fuzzyIntervals": [2, 1, 1, -2, -1, -1],  // 묶음 음정 (±1=반음, ±2=도약, ±3=큰도약) — intervals에서 자동 계산
  "durationClasses": [0, 0, 1, 0, -1, 0],  // 음길이 범주 (2=2박↑, 1=1박, 0=8분, -1=16분) — sheetData notes에서 자동 변환
  
  // 집계 통계 (검색 필터용) — 모두 pitches 배열에서 자동 집계
  "pitchMin": 60,                            // 최저음 MIDI
  "pitchMax": 72,                            // 최고음 MIDI
  "pitchRange": 12,                          // 음역대 (max - min)
  "pitchMean": 66.2,                         // 평균 음고 (밝음/어두움 경향)
  "startPitch": 62,                          // 첫 음 MIDI (pin-first 검색)
  "endPitch": 60                             // 끝 음 MIDI (pin-last 검색)
}
```

---

## 각 섹션이 왜 필요한가

| 섹션                 | 어디서 씀                | 언제 계산                                  |
|--------------------|----------------------|------------------------------------------|
| **1. Identity**    | 중복 방지, 사용자 릭 분리      | 저장 시 자동                                 |
| **2. Performance** | 필터 (조/스타일/연주자)       | 사람이 입력                                  |
| **3. Harmonic**    | 화성별 검색, 화성 맥락        | sheetData에서 자동 추출                       |
| **4. sheetData**   | 화면 렌더링               | 사람이 입력 (LickCreator)                    |
| **5. Features**    | 유사도 검색 (Levenshtein) | 입력값 제공 시 그대로 사용; 없으면 1~4번에서 자동 집계·계산 |

## 특이 케이스

이제 모든 케이스를 정리할 수 있습니다.

---

## NoteInfo — 특수 케이스 전체

### 1. 셋잇단음표 (Tuplet)

`{ "keys": ["g/4"], "duration": "8", "tuplet": 3 }
{ "keys": ["e/5"], "duration": "8", "tuplet": 3 }
{ "keys": ["c/5"], "duration": "8", "tuplet": 3 }`

**규칙**:

- 반드시**3개 연속**으로 나와야 함 (1그룹 = 3음)
- `duration: "8"`→ 8분음표 3개가 2박 자리에 (3:2 비율)
- `duration: "16"`→ 16분음표 3개가 1박 자리에 (16th triplet)
- 쉼표도 포함 가능:`{ "duration": "8r", "tuplet": 3 }`
- **beamBreak가 셋잇단 3번째 음에 붙으면**다음 셋잇단과 빔 분리

`{ "keys": ["e/5"], "duration": "8", "tuplet": 3, "beamBreak": true }`

---

### 2. 점음표 (Dotted)

`{ "keys": ["e/5"], "duration": "q", "dotted": true }
{ "keys": ["d/5"], "duration": "8", "dotted": true }`

**규칙**:

- `duration`+`dotted: true`조합
- `q`+ dotted = 1.5박 (3/4박자에서 자주)
- `8`+ dotted = 0.75박 (점8분 + 16분 리듬)
- 쉼표에도 적용 가능:`"duration": "8r", "dotted": true`

---

### 3. 타이 (Tie)

`{ "keys": ["d/6"], "duration": "q", "accidentals": {"0": "b"}, "tie": true }`

**규칙**:

- `tie: true`=**다음 음표로 이어짐**(같은 음고여야 함)
- 마디 넘어가는 타이도 가능 (마지막 음에`tie: true`)
- 렌더러는 이 음과 다음 음 사이에 슬러 곡선 그림
- 플레이어는 두 음을 하나로 합쳐서 연주

---

### 4. 글리산도 (Gliss)

`{ "keys": ["f/5"], "duration": "q", "gliss": true }`

**규칙**:

- `gliss: true`= 다음 음까지 글리산도 선 그림
- 반음계적 슬라이드 표현
- 실제 데이터에서 1개밖에 없을 정도로 희귀

---

### 5. 임시표 (Accidentals)

`{ "keys": ["d/4"], "duration": "8", "accidentals": {"0": "b"} }
{ "keys": ["f/5"], "duration": "q", "accidentals": {"0": "#"} }
{ "keys": ["c/5"], "duration": "q", "accidentals": {"0": "n"} }`

**규칙**:

- `accidentals`=`{ 음표인덱스: "#" | "b" | "n" }`
- 인덱스`"0"`= 첫 번째 음 (단음은 항상 0)
- 화음(chord note)이면`{"0": "b", "1": "#"}`형태 가능
- `"n"`= 제자리표 (natural, ♮)
- **키 시그니처 대비 실제 필요할 때만**붙임 (예: F 장조에서 B♮)

---

### 6. 쉼표 (Rest)

`{ "keys": ["b/4"], "duration": "qr" }
{ "keys": ["b/4"], "duration": "8r" }
{ "keys": ["b/4"], "duration": "wr" }`

**규칙**:

- `duration`뒤에`r`붙이면 쉼표
- `keys`는 항상`["b/4"]`고정 (VexFlow 기본값, 무시됨)
- `dotted: true`조합 가능
- `tuplet: 3`조합 가능 (셋잇단 중간 쉼표)
- 유사도 계산 시`nEvents`,`intervals`에서**제외**

---

### 7. 화음 (Chord Symbol)

**마디 레벨**:

`{
  "chord": "D-7",
  "notes": [...]
}`

**두 코드가 한 마디에**:

`{
  "chord": "D-7  G7",
  "notes": [...]
}`

→ 두 칸 이상 공백으로 구분 (`"D-7  G7"`)

---

### 8. 빔 브레이크 (BeamBreak)

`{ "keys": ["e/5"], "duration": "8", "tuplet": 3, "beamBreak": true }`

**규칙**:

- 이 음**이후에**빔 강제 끊기
- 주로 셋잇단 그룹 사이 분리에 사용
- 일반 8분음표 흐름 중 강제 분리에도 사용

---

## duration 값 전체 목록

| 값     | 의미            | 점음표 가능 |
|-------|---------------|--------|
| `w`   | 온음표 (4박)      | ✅      |
| `h`   | 2분음표 (2박)     | ✅      |
| `q`   | 4분음표 (1박)     | ✅      |
| `8`   | 8분음표 (0.5박)   | ✅      |
| `16`  | 16분음표 (0.25박) | ✅      |
| `wr`  | 온쉼표           | ✅      |
| `hr`  | 2분쉼표          | ✅      |
| `qr`  | 4분쉼표          | ✅      |
| `8r`  | 8분쉼표          | ✅      |
| `16r` | 16분쉼표         | ✅      |

---

## keys 포맷

`"c/4"  → 가운데 C (middle C, MIDI 60)
"d/5"  → 위 D
"f/5"  → 위 F`

`음이름(소문자)/옥타브` 형식. 임시표는 `accidentals`로 별도 처리.

---

## 유사도 계산 시 처리 방법

| 케이스       | intervals 계산       | 처리법                 |
|-----------|--------------------|---------------------|
| 쉼표        | **제외**             | 음표만 추출              |
| 타이        | **하나로 합침**         | 타이 이전 음 + 이후 음 = 1음 |
| 셋잇단       | 포함, 별도 처리 없음       | interval은 동일하게 계산   |
| 점음표       | durationClass만 달라짐 | interval 계산은 정상     |
| 글리산도      | interval 포함        | 표현 기법, 음정은 그대로      |
| 화음(chord) | 코드 표시만, 멜로디 무관     | 무시                  |