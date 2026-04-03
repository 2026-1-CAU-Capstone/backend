# 🔍 Phase 3: Layer 2 – 문맥 패턴 감지

> 이 문서는 **Phase 3**를 설명합니다.
> Layer 1에서 개별 코드의 기본 성질을 파악했다면, Layer 2에서는 **앞뒤 코드의 관계(문맥)** 를 분석합니다.

---

## 목차

1. [IiViDetector – ii-V-I 패턴 감지](#1-iividetector--ii-v-i-패턴-감지)
2. [TritoneSubDetector – 트라이톤 대리 감지](#2-tritonesubdetector--트라이톤-대리-감지)
3. [SecondaryDominantDetector – 세컨더리 도미넌트 감지](#3-secondarydominantdetector--세컨더리-도미넌트-감지)
4. [DiminishedClassifier – 감화음 분류](#4-diminishedclassifier--감화음-분류)
5. [ChromaticApproachDetector – 반음계적 접근 감지](#5-chromaticapproachdetector--반음계적-접근-감지)
6. [DeceptiveResolutionDetector – 기만 종지 감지](#6-deceptiveresolutiondetector--기만-종지-감지)
7. [PedalPointDetector – 페달 포인트 감지](#7-pedalpointdetector--페달-포인트-감지)

---

## 1. IiViDetector – ii-V-I 패턴 감지

**파일:** `service/implementation/IiViDetector.java`  
**역할:** 재즈의 가장 기본 패턴인 **ii-V-I 진행**을 감지하고, 다양한 변형도 찾아냅니다.

### 1.1 ii-V-I이란?

재즈에서 가장 흔한 코드 진행으로, **집을 떠났다가(ii) → 긴장하고(V) → 집에 돌아오는(I)** 느낌입니다.

```
C장조:  Dm7  →  G7  →  Cmaj7
        ii      V       I
        SD      D       T
       (떠남)  (긴장)  (해결)
```

### 1.2 감지 로직 (표준 ii-V-I)

감지기는 **V(도미넌트) 코드를 기준으로** 앞뒤를 탐색합니다:

```
전체 코드 리스트에서:

1. V 후보 찾기: 도미넌트 품질(dom7, dom7sus4, aug7)인 코드를 찾는다
   
2. I 찾기 (V 다음 코드):
   - V의 근음에서 완전5도 아래(= 완전4도 위)의 코드가 있는지 확인
   - 예: G7(root=7) → 타겟 = mod12(7-7) = 0 = C
   - 다음 코드가 Cmaj7(root=0)이면 → I 발견!
   
3. ii 찾기 (V 이전 코드):
   - 타겟 I의 장2도 위 코드가 있는지 확인
   - 예: 타겟 = C(0) → 예상 ii = mod12(0+2) = 2 = D
   - 이전 코드가 Dm7(root=2)이면 → ii 발견!
```

### 1.3 변형 패턴

감지기는 표준 외에 **5가지 변형**도 감지합니다:

#### (a) 마이너 ii-V-i

```
Dm7b5 → G7 → Cm7
ii°      V    i
```
ii가 하프디미니쉬(min7b5), I이 마이너 → `variant = "minor"`

#### (b) 트라이톤 대리 V (Tritone Sub V)

```
Dm7 → Db7 → Cmaj7
ii    bII7    I
```

V(G7) 대신 반음 위의 도미넌트(Db7)가 사용되면 → `variant = "tritone_sub_V"`

**감지 방법:** V 코드와 다음 코드(I 후보)의 간격이 반음 아래(interval=11)인 경우

#### (c) 백도어 ii-V-I (Backdoor)

```
Fm7  → Bb7 → Cmaj7
iv     bVII    I
```

일반적인 V7 대신 bVII7이 사용되는 "뒷문으로 들어오는" 진행 → `variant = "backdoor"`

**감지 방법:** V 코드가 다이어토닉 V7이 아니고, V의 근음 + 장2도 위가 I인 경우

#### (d) sus4 딜레이

```
Dm7 → G7sus4 → G7 → Cmaj7
ii    V(sus)   V     I
```

V7sus4 → V7으로 해결된 후 I으로 가는 패턴 → `variant = "standard_with_sus_delay"`

#### (e) 불완전 (Incomplete)

ii-V는 있지만 I이 없는 경우, 또는 V-I만 있는 경우 → `variant = "incomplete"`

### 1.4 그룹 결과

감지된 각 ii-V-I는 **그룹(Group)** 으로 묶여 반환됩니다:

```json
{
  "group_id": 1,
  "group_type": "ii-V-I",
  "variant": "standard",
  "target_key": "C",
  "is_diatonic_target": true,
  "members": [
    {"bar": 1, "beat": 1.0, "symbol": "Dm7", "role": "ii"},
    {"bar": 1, "beat": 3.0, "symbol": "G7",  "role": "V"},
    {"bar": 2, "beat": 1.0, "symbol": "Cmaj7", "role": "I"}
  ],
  "notes": "Diatonic ii-V-I in C"
}
```

각 코드의 `groupMemberships`에도 소속 정보가 추가됩니다.

---

## 2. TritoneSubDetector – 트라이톤 대리 감지

**파일:** `service/implementation/TritoneSubDetector.java`  
**역할:** ii-V-I 감지에서 놓친 **독립적인 트라이톤 대리 코드**를 추가로 감지

### 2.1 트라이톤 대리란?

두 도미넌트7 코드가 **트라이톤(증4도 = 반음 6개)** 관계에 있으면, 서로 대체 가능합니다.

```
G7의 트라이톤 대리 = Db7
(G의 피치클래스 7 + 6 = 13 → mod12 = 1 = Db)

G7  (G-B-D-F)  에서 핵심 음: B(11)과 F(5) ← 트라이톤 관계
Db7 (Db-F-Ab-Cb) 에서 핵심 음: F(5)과 Cb(=B)(11) ← 같은 트라이톤!
```

### 2.2 감지 로직

```
for (연속된 두 코드 chord, next) {
    1. chord가 도미넌트 품질(dom7 등)인가?
    2. 이미 ii-V-I에서 트라이톤 대리로 태깅되었나? → 건너뜀
    3. chord → next의 근음 간격이 반음 아래(interval=11)인가?
       예: Db7(1) → C코드(0), interval = mod12(0-1) = 11 ✅
    
    → 트라이톤 대리!
    
    원래 V 코드 = mod12(chord.root + 6)
    예: Db7(1) + 6 = 7 = G → "Db7은 G7의 트라이톤 대리"
}
```

**결과:** 해당 코드에 `D_substitute` 기능과 모호성 플래그가 추가됩니다.

---

## 3. SecondaryDominantDetector – 세컨더리 도미넌트 감지

**파일:** `service/implementation/SecondaryDominantDetector.java`  
**역할:** 원래 키의 V7이 아닌 도미넌트7 코드가 **다른 코드를 일시적으로 토닉화**하는 패턴을 감지

### 3.1 세컨더리 도미넌트란?

```
C장조에서:
  A7 → Dm7
  
A7은 C장조의 다이어토닉이 아니지만,
D를 임시 토닉으로 보면 A7은 "D의 V7"
→ A7 = V/ii (ii로 가는 세컨더리 도미넌트)
```

### 3.2 감지 로직

```
for (각 코드) {
    1. 도미넌트 품질(dom7 등)인가?
    2. 다이어토닉 V인가? → YES면 건너뜀 (그건 그냥 V)
    3. "이 코드의 V7이 해결되는 대상" 계산:
       타겟 근음 = mod12(코드 근음 + 5)  (완전4도 위 = 완전5도 아래)
       예: A7(9) → 타겟 = mod12(9+5) = 2 = D
    4. 타겟의 스케일 디그리를 찾는다:
       interval(keyRoot, targetRoot) = interval(0, 2) = 2 → "ii"
    5. 다음 코드가 실제로 타겟인지 확인:
       다음 코드가 Dm7(root=2) → 해결됨!
}
```

### 3.3 결과 예시

```
A7 (C장조에서):
  secondaryDominant = {
    type: "V/ii",
    targetDegree: "ii",
    targetChord: "Dm7",
    resolved: true
  }
  functions = [FunctionEntry("D", 0.9, "Secondary dominant V/ii (resolved)")]
```

> 💡 해결된(resolved) 세컨더리 도미넌트는 확신도 0.9, 미해결(unresolved)은 0.6입니다.

---

## 4. DiminishedClassifier – 감화음 분류

**파일:** `service/implementation/DiminishedClassifier.java`  
**역할:** 감화음(dim, dim7)의 **문맥적 기능**을 분류

### 4.1 감화음은 왜 분류가 필요한가?

감화음은 문맥에 따라 완전히 다른 역할을 합니다:

| 분류 | 영문 | 설명 | 예시 |
|------|------|------|------|
| **경과음** | passing | 두 코드 사이를 반음으로 연결 | C → C#dim → Dm |
| **보조음** | auxiliary | 같은 코드로 돌아오는 장식 | C → C#dim → C |
| **도미넌트 기능** | dominant_function | rootless V7b9처럼 기능 | F#dim → G (= D7b9 → G) |

### 4.2 감지 로직

```
for (각 감화음) {
    이전 코드 = prev, 현재 = chord, 다음 = next
    
    (1) 보조음(auxiliary) 검사:
        prev.root == next.root 이고
        chord.root - prev.root 가 0 또는 1반음이면
        → "auxiliary"
        
    (2) 경과음(passing) 검사:
        prev → chord → next 가 반음씩 올라가거나 내려가면
        → "passing"
        
        상행: prev=C(0), chord=C#dim(1), next=D(2) → 각 1반음씩
        하행: prev=D(2), chord=Db_dim(1), next=C(0) → 각 1반음씩
        
    (3) 도미넌트 기능 검사:
        chord.root + 1 == next.root 이면
        (감화음이 다음 코드의 반음 아래에서 이끌어가면)
        → "dominant_function"
        
        암시된 도미넌트 = mod12(chord.root - 4)
        예: F#dim(6) → next=G(7)
            암시된 도미넌트 = mod12(6-4) = 2 = D → "D7b9"
            → 이 F#dim은 실질적으로 D7b9 (rootless)와 같다!
    
    (4) 위 어느 것에도 해당 안 되면:
        → "unknown" + 모호성 플래그 추가
}
```

---

## 5. ChromaticApproachDetector – 반음계적 접근 감지

**파일:** `service/implementation/ChromaticApproachDetector.java`  
**역할:** 다이어토닉이 아닌 코드가 다음 코드로 **반음 미끄러짐**으로 접근하는 패턴 감지

### 5.1 반음계적 접근이란?

```
Ebmaj7 → Dm7    (반음 위에서 아래로 접근)
Dbmaj7 → Dm7    (반음 아래에서 위로 접근)
```

> 💡 재즈에서 "코드가 다이어토닉이 아닌데 왜 여기 있지?" → 
> "아, 다음 코드로 반음 미끄러지려고 잠깐 거쳐간 거구나!"

### 5.2 감지 로직

```
for (연속된 두 코드 chord, next) {
    다음 조건을 모두 만족해야 함:
    1. chord가 다이어토닉이 아님 (이미 설명된 코드 제외)
    2. 세컨더리 도미넌트가 아님
    3. 트라이톤 대리가 아님
    
    근음 간격 = mod12(next.root - chord.root)
    - interval == 1  → 아래에서 접근 (direction = "below")
    - interval == 11 → 위에서 접근 (direction = "above")
    
    품질 일치 여부도 기록 (같은 품질이면 더 확실한 접근)
}
```

**결과:**
```
Ebmaj7 (C장조에서):
  chromaticApproach = {
    target: "Dm7",
    direction: "above",     // 반음 위에서
    qualityMatch: false      // 품질이 다름 (maj7 vs min7)
  }
```

기능이 비어있으면 `chromatic_approach` 기능이 추가됩니다 (품질 일치 시 확신도 0.7, 불일치 시 0.5).

---

## 6. DeceptiveResolutionDetector – 기만 종지 감지

**파일:** `service/implementation/DeceptiveResolutionDetector.java`  
**역할:** 도미넌트7이 **예상과 다른 곳으로 해결**되는 기만 종지를 감지

### 6.1 기만 종지란?

```
기대: G7 → Cmaj7 (V → I, 정상 해결)
실제: G7 → Am7   (V → vi, 기만 종지!)
```

> 💡 "속았다!" 느낌. V7이 I으로 갈 줄 알았는데 다른 데로 갔어요.

### 6.2 흔한 기만 종지 패턴

기대되는 I에서의 간격으로 분류합니다:

| 예상 I로부터의 간격 | 실제 도착 디그리 | 흔한 패턴? | 예시 (C장조) |
|---------------------|-----------------|-----------|-------------|
| +9 반음 | vi | ✅ | G7 → Am7 |
| +8 반음 | bVI | ✅ | G7 → Abmaj7 |
| +5 반음 | IV | ✅ | G7 → Fmaj7 |
| +4 반음 | iii | ✅ | G7 → Em7 |
| +10 반음 | bVII | ✅ | G7 → Bbmaj7 |
| +3 반음 | bIII | ❌ | G7 → Ebmaj7 |

### 6.3 감지 로직

```
for (연속된 두 코드 chord, next) {
    1. chord가 도미넌트7인가?
    2. 기대되는 해결 = mod12(chord.root + 5) (완전4도 위)
       예: G7(7) → 기대 = 0 = C
    3. next.root == 기대? → YES면 정상 해결, 건너뜀
    4. 백도어로 태깅된 코드면 건너뜀
    5. next.root ≠ 기대 → 기만 종지!
    6. 간격을 계산하여 어떤 종류의 기만 종지인지 분류
}
```

**결과:**
```
G7 → Am7 (C장조):
  deceptiveResolution = {
    dominantChord: "G7",
    expectedResolution: "Cmaj7",
    actualResolution: "Am7",
    actualDegree: "vi",
    commonPattern: true
  }
```

---

## 7. PedalPointDetector – 페달 포인트 감지

**파일:** `service/implementation/PedalPointDetector.java`  
**역할:** 베이스 음이 **여러 마디에 걸쳐 지속**되는 페달 포인트를 감지

### 7.1 페달 포인트란?

```
Cmaj7 → Dm7/C → G7/C → Cmaj7
        ▲          ▲
    베이스 C가 계속 유지됨!
```

> 💡 오르간에서 발로 밟는 페달(pedal)처럼, 베이스가 한 음에 고정되어 있는 것.
> 위의 코드가 바뀌어도 베이스는 묵묵히 한 음을 유지합니다.

### 7.2 감지 로직

```
최소 지속: 2마디 이상 + 2개 이상의 코드

for (코드 리스트를 순회) {
    1. 유효 베이스 = 슬래시 코드면 bass, 아니면 root
    2. 같은 베이스가 연속되는 구간을 찾는다
    3. 그 구간이 2마디 이상이고 코드가 2개 이상이면 → 페달!
    
    페달 유형 판별:
    - 베이스와 키 근음의 간격이 0 → "tonic" (토닉 페달)
    - 간격이 7 → "dominant" (도미넌트 페달)
    - 간격이 5 → "subdominant" (서브도미넌트 페달)
    - 그 외 → "on X" (X음 페달)
}
```

### 7.3 페달 유형

| 유형 | 의미 | 흔한 사용처 |
|------|------|-------------|
| **tonic** | 으뜸음 위의 페달 | 곡의 시작이나 끝, 안정감 |
| **dominant** | 5번째 음 위의 페달 | 클라이맥스 전 긴장감 |
| **subdominant** | 4번째 음 위의 페달 | 부드러운 전환 |

**결과:**
```
Cmaj7, Dm7/C, G7/C, Cmaj7 (마디 1~4, C장조):
  각 코드에:
  pedalInfo = {
    pedalNote: 0,
    pedalNoteName: "C",
    pedalType: "tonic",
    isOverPedal: true,
    pedalStartBar: 1,
    pedalEndBar: 4
  }
```

