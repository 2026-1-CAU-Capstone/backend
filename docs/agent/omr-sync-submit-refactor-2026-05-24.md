# OMR 제출 동기화 리팩터링 - 2026-05-24

## 작업 내용 요약

OMR 파일 제출 시 **비동기 이벤트 리스너 방식**을 제거하고,  
**서비스 메서드 내에서 직접 동기 제출 + 응답 확인 후 반환**하는 방식으로 전환.

---

## 문제 상황 (Before)

```
클라이언트 → createFromOmr()
              ├─ ① PENDING 엔티티 저장
              ├─ ② ApplicationEventPublisher.publishEvent(OmrRequestedEvent)
              └─ ③ PENDING 상태 즉시 반환  ← 클라이언트는 OMR 전달 여부 모름

# @Async @TransactionalEventListener(AFTER_COMMIT) 에 의해 별도 스레드에서:
              └─ ④ omrClient.submitJob() — fire-and-forget
```

**핵심 문제**: 클라이언트는 OMR 서버가 파일을 수신했는지 확인하기 전에 응답을 받았다.  
제출 실패가 발생해도 API 호출자는 성공(200 OK)을 받게 됨.

---

## 수정 후 흐름 (After)

```
클라이언트 → createFromOmr()
              ├─ ① PENDING 엔티티 저장 & 커밋  (Writer 자체 @Transactional)
              ├─ ② omrClient.submitJob() ← OMR 서버 응답(job_id) 대기
              │       성공 → storeJobIdAndMarkProcessing() → PROCESSING 커밋
              │       실패 → fail() → FAILED 커밋 → CustomException throw
              └─ ③ DB에서 최신 엔티티 재조회 → PROCESSING 상태로 반환
```

**클라이언트는 OMR 서버가 job_id를 반환한 이후에만 응답을 받는다.**

---

## 설계 의도

| 목적 | 선택 |
|------|------|
| 엔티티 커밋 보장 | Writer 자체 `@Transactional`로 제출 전 DB 확정 |
| OMR 서버 응답 확인 | `omrClient.submitJob()` 동기 블로킹 호출 (기존 유지) |
| 클라이언트 정확한 상태 반환 | DB 재조회로 PROCESSING/FAILED 상태 반환 |
| User 엔티티 세션 문제 해결 | SheetProject·ChordProject에 `TransactionTemplate` 적용 |

---

## 변경된 파일

### 서비스 (핵심 로직 변경)

| 파일 | 변경 내용 |
|------|-----------|
| `LickService.java` | `createFromOmr()` — `@Transactional` 제거, 직접 submitJob 호출 |
| `SoloService.java` | 동일 |
| `SheetProjectService.java` | 동일 + `TransactionTemplate` 주입 (User 엔티티 세션 처리) |
| `ChordProjectService.java` | 동일 + `TransactionTemplate` 주입 |

### 삭제된 파일 (이벤트 기반 비동기 패턴 제거)

| 삭제 파일 |
|----------|
| `LickOmrEventListener.java` |
| `SoloOmrEventListener.java` |
| `SheetProjectOmrEventListener.java` |
| `ChordProjectOmrEventListener.java` |
| `SheetProjectOmrEventListenerTest.java` |
| `ChordProjectOmrEventListenerTest.java` |

> 이벤트 클래스(`LickOmrRequestedEvent` 등)는 참고용으로 유지. 필요 없으면 삭제해도 무방.

---

## 트랜잭션 구조 상세

### Lick·Solo (User 엔티티 미사용, 간단)

```
createFromOmr() — @Transactional 없음
   ↓
Writer.createPending()         → 자체 트랜잭션 T1 생성·커밋
   ↓
omrClient.submitJob()          → HTTP 요청 (트랜잭션 없음)
   ↓ 성공
Writer.storeJobIdAndMarkProcessing() → 자체 트랜잭션 T2 생성·커밋
   ↓
Reader.getByPublicId()         → 자체 readOnly 트랜잭션 T3 생성·종료
   ↓ 반환
```

### SheetProject·ChordProject (User 엔티티 포함)

```
createFromOmr() — @Transactional 없음
   ↓
transactionTemplate.execute {
   UserReader.getByPublicId()  → T1 내에서 User 관리 엔티티 조회
   OmrWriter.createPending()   → T1에 참여 (User 세션 안에서 저장)
} → T1 커밋                   ← User detach 문제 해결
   ↓
omrClient.submitJob()          → HTTP 요청 (트랜잭션 없음)
   ↓ 성공
OmrWriter.storeJobIdAndMarkProcessing() → 자체 트랜잭션 T2 커밋
   ↓
Reader.findByPublicId()        → 자체 readOnly 트랜잭션 T3
   ↓ 반환
```

#### 왜 TransactionTemplate이 필요한가?

SheetProject·ChordProject의 Writer는 `User user` 객체를 직접 받아 `ChordProject.setUser(user)` 형태로 FK를 세팅한다.  
외부 `@Transactional`이 없으면 `UserReader.getByPublicId()`가 자체 트랜잭션을 닫은 후 `User` 엔티티가 **detached** 상태가 된다.  
`ChordProjectRepository.save(project)` 시 detached `User`를 참조하면 Hibernate가 `DetachedObjectException`을 던질 수 있다.  
`TransactionTemplate`으로 두 작업을 같은 세션 안에서 실행하여 해결.

---

## 에러 처리 정책

| 상황 | 처리 |
|------|------|
| OMR 서버 미설정 | `CustomException(OMR_SERVER_NOT_CONFIGURED)` throw |
| OMR 서버 응답 없음·오류 | 엔티티 `FAILED` 저장 후 `CustomException(OMR_SUBMIT_FAILED)` throw |
| OMR 서버 job_id 누락 | 동일 (OmrClient 내에서 검증) |

**FAILED 상태 엔티티는 DB에 남는다.** 재시도 API는 별도 구현 필요 (TODO).

---

## 임의 결정 사항

1. **FAILED 엔티티 유지**: OMR 제출 실패 시 엔티티를 삭제하지 않고 FAILED 상태로 유지.  
   클라이언트가 오류 응답을 받지만, 엔티티는 남아 있다.  
   재시도 메커니즘은 없으므로, 필요 시 새로운 요청을 보내야 한다.

2. **이벤트 클래스 유지**: `LickOmrRequestedEvent` 등 이벤트 레코드는 삭제하지 않았다.  
   향후 다른 목적(예: 감사 로그, 알림)에 재사용될 수 있기 때문.

3. **SoloService에서 `contentType` 변수 제거**:  
   제출 방식 변경으로 `contentType`을 직접 이벤트로 전달할 필요가 없어졌으므로 제거.

---

## 개발자 유의사항

- **OMR 서버 응답 지연**: `omrClient.submitJob()`은 blocking HTTP 호출이므로, OMR 서버 응답이 느릴 경우 API 응답 시간이 길어진다.  
  운영 환경에서 타임아웃 설정(`WebClient` 레벨)을 반드시 적용할 것.

- **재시도 없음**: 제출 실패 시 자동 재시도 로직이 없다. FAILED 상태의 프로젝트를 재처리하는 Admin API 또는 재시도 엔드포인트가 필요하면 추가 구현 필요.

- **콜백 URL 설정**: OMR 서버가 처리 완료 후 콜백을 보내야 하므로, dev 환경에서는 ngrok 등으로 외부 노출이 필요.  
  각 도메인별 콜백 엔드포인트:
  - SheetProject: `POST /v1/sheet-projects/omr/callback`
  - ChordProject: `POST /v1/chord-projects/omr/callback`
  - Lick:         `POST /v1/licks/omr/callback`
  - Solo:         `POST /v1/solos/omr/callback`
  
  `omr.callback-url`에는 도메인 경로 포함 전체 URL을 설정해야 한다  
  (예: `https://xxxx.ngrok-free.app/v1/licks/omr/callback`).  
  도메인마다 다른 URL이 필요한 경우 `callback-url`을 베이스 URL로 변경하고  
  서비스 레벨에서 경로를 조합하는 방식으로 개선 가능.

---

## 클래스 역할 표

| 클래스 | 역할 |
|--------|------|
| `LickService.createFromOmr()` | OMR 제출 오케스트레이터 (① 생성 ② 제출 ③ 반환) |
| `LickWriter.createPending()` | PENDING 엔티티 생성·커밋 (자체 트랜잭션) |
| `LickWriter.storeJobIdAndMarkProcessing()` | job_id 저장 + PROCESSING 전환·커밋 |
| `LickWriter.fail()` | FAILED 전환·커밋 |
| `OmrClient.submitJob()` | OMR 서버 HTTP 제출·응답 수신 (blocking) |
| `LickOmrCallbackController` | OMR 서버에서 오는 처리 완료 콜백 수신 |

> SheetProject / ChordProject / Solo 도 동일 패턴.

## 논리 흐름도

```
클라이언트 POST /v1/licks/omr
    │
    ▼
LickService.createFromOmr()
    │
    ├─[T1]─ LickWriter.createPending()
    │           └─ lickRepository.save(PENDING)  ──→ DB commit
    │
    ├─[HTTP]─ OmrClient.submitJob(fileData, filename, lickPublicId)
    │           └─ POST /omr/{dev|prod}/process
    │               └─ 202 응답 ← { job_id, status }
    │
    ├─ 성공 ─[T2]─ LickWriter.storeJobIdAndMarkProcessing()
    │                   └─ markOmrProcessing(10%)  ──→ DB commit
    │
    ├─ 실패 ─[T2]─ LickWriter.fail()
    │                   └─ markOmrFailed()  ──→ DB commit
    │                   └─ CustomException throw  → HTTP 4xx/5xx
    │
    └─[T3]─ LickReader.getByPublicId()
                └─ SELECT lick  ──→ LickResponse(omrStatus=PROCESSING)
                └─ 클라이언트에 반환

# 이후 비동기 흐름 (변경 없음):
OMR 서버 처리 완료
    │
    ▼
POST /v1/licks/omr/callback  (X-OMR-Callback-API-Key 헤더 검증)
    │
    ▼
LickService.handleOmrCallback()
    └─ 완료: fetchMusicXml() + fetchChordAssignments() → completePending()
    └─ 실패: fail()
```

