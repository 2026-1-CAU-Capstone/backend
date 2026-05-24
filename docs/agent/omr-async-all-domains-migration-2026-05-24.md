# OMR 비동기 전면 전환 정리

작성일: 2026-05-24  
범위: `SheetProject`, `ChordProject`, `Lick`, `Solo`, `shared/omr`, 관련 테스트

---

## 1. 작업한 내용

OMR 서버의 비동기 API 전환에 맞춰, **OMR를 사용하는 모든 API를 비동기 제출 + 콜백 완료 처리 구조로 변경**했다.

### 전환 완료 도메인

| 도메인 | 비동기 제출 | 콜백 처리 | 상태 저장 | 결과 반영 |
|---|---:|---:|---:|---:|
| `SheetProject` | 완료 | 완료 | 완료 | 완료 |
| `ChordProject` | 완료 | 완료 | 완료 | 완료 |
| `Lick` | 완료 | 완료 | 완료 | 완료 |
| `Solo` | 완료 | 완료 | 완료 | 완료 |

### 공통 변경사항

- `OmrClient`
  - 동기 `recognize()` 제거
  - `submitJob(byte[], filename, jobId)` 추가
  - `fetchMusicXml(jobId)` 추가
  - `fetchChordAssignments(jobId)` 추가
- `OmrProperties`
  - `apiKey`, `callbackApiKey`, `callbackUrl` 추가
- `OmrErrorCode`
  - `OMR_SUBMIT_FAILED`, `OMR_CALLBACK_KEY_INVALID`, `OMR_JOB_NOT_FOUND` 추가
- 테스트 전면 갱신
  - 동기 `recognize()` 기반 테스트 제거
  - `submitJob` / `fetch*` / `processJobResult` / submit-only event listener 테스트로 변경

---

## 2. 도메인별 변경 사항

## 2-1. SheetProject

기존에 부분적으로 바꿔둔 구조를 유지하되, 최종 기준 구조를 정리하면 다음과 같다.

- `createFromOmr()`
  - 파일 검증
  - pending `SheetProject` 생성 (`omrStatus=PENDING`)
  - 이벤트 발행
- `SheetProjectOmrEventListener`
  - OMR 서버에 파일 제출만 수행
  - 반환된 `job_id` 저장
  - `PROCESSING(10%)` 전환
- `SheetProjectOmrCallbackController`
  - `POST /v1/sheet-projects/omr/callback`
  - `X-OMR-Callback-API-Key` 검증
  - `status=completed|failed` 처리
- `SheetProjectOmrProcessor`
  - `processJobResult(jobId)`로 MusicXML + chord assignments 조회 후 파싱

## 2-2. ChordProject

### 추가/수정
- `ChordProject` 엔티티에 아래 필드 추가
  - `omrJobId`
  - `omrRequestedTitle`
  - `omrRequestedKey`
  - `omrRequestedTimeSignature`
- `ChordProjectOmrWriter`
  - `storeJobIdAndMarkProcessing()` 추가
- `ChordProjectOmrEventListener`
  - 기존 즉시 파싱 제거
  - OMR 서버 제출 전용으로 변경
- `ChordProjectService.handleOmrCallback()` 추가
- `ChordProjectOmrCallbackController`, `ChordProjectOmrCallbackControllerSpec` 추가

### 의도
ChordProject는 사용자 입력 `title/key/timeSignature`가 OMR 파싱 결과보다 우선할 수 있으므로, callback 시점에 반영할 override 값을 엔티티에 저장했다.

## 2-3. Lick

### 추가/수정
- `Lick` 엔티티에 아래 필드 추가
  - `omrStatus`
  - `omrProgress`
  - `omrFailureReason`
  - `omrJobId`
- `LickResponse`
  - OMR 상태 필드 추가
  - pending 상태 대응을 위해 `sheetData` nullable 허용
- `LickWriter`
  - `createPending()`
  - `storeJobIdAndMarkProcessing()`
  - `markProcessing()`
  - `completePending()`
  - `fail()` 추가
- `LickService`
  - `createFromOmr()`를 async submit 방식으로 변경
  - `handleOmrCallback()` 추가
- `LickOmrEventListener`, `LickOmrRequestedEvent` 추가
- `LickOmrCallbackController`, `LickOmrCallbackControllerSpec` 추가

### 의도
Lick는 인증 사용자 전용 엔티티가 아니고 공개 조회가 열려 있으므로, 별도 status DTO를 만들기보다 기존 `LickResponse`에 OMR 상태를 포함시켜 `GET /v1/licks/{publicId}`만으로 상태 확인 가능하도록 했다.

## 2-4. Solo

Lick와 동일한 패턴으로 구현했다.

### 추가/수정
- `Solo` 엔티티에 아래 필드 추가
  - `omrStatus`
  - `omrProgress`
  - `omrFailureReason`
  - `omrJobId`
- `SoloResponse`
  - OMR 상태 필드 추가
  - pending 상태 대응을 위해 `sheetData` nullable 허용
- `SoloWriter`
  - pending/processing/completion/failure 메서드 추가
- `SoloService`
  - `createFromOmr()` async submit 전환
  - `handleOmrCallback()` 추가
- `SoloOmrEventListener`, `SoloOmrRequestedEvent` 추가
- `SoloOmrCallbackController`, `SoloOmrCallbackControllerSpec` 추가

---

## 3. 새로 생긴/중요해진 클래스 역할 표

| 클래스 | 역할 |
|---|---|
| `OmrClient` | OMR 서버 HTTP 통신 전담. 제출/결과 조회 담당 |
| `SheetProjectOmrEventListener` | SheetProject OMR 제출 전용 비동기 리스너 |
| `SheetProjectOmrCallbackController` | SheetProject 콜백 엔드포인트 |
| `ChordProjectOmrEventListener` | ChordProject OMR 제출 전용 비동기 리스너 |
| `ChordProjectOmrCallbackController` | ChordProject 콜백 엔드포인트 |
| `LickOmrEventListener` | Lick OMR 제출 전용 비동기 리스너 |
| `LickOmrCallbackController` | Lick 콜백 엔드포인트 |
| `SoloOmrEventListener` | Solo OMR 제출 전용 비동기 리스너 |
| `SoloOmrCallbackController` | Solo 콜백 엔드포인트 |
| `SheetProjectOmrProcessor` | jobId 기반 결과 조회 + SheetProject용 파싱 |
| `ChordProjectOmrProcessor` | jobId 기반 결과 조회 + ChordProject용 파싱 |
| `LickOmrProcessor` | jobId 기반 결과 조회 + Lick용 파싱 |
| `SoloOmrProcessor` | jobId 기반 결과 조회 + Solo용 파싱 |
| `LickWriter` | Lick pending/processing/completed/failed 상태 반영 및 최종 데이터 저장 |
| `SoloWriter` | Solo pending/processing/completed/failed 상태 반영 및 최종 데이터 저장 |
| `ChordProjectOmrWriter` | ChordProject pending/processing/completed/failed 상태 반영 |

---

## 4. 논리 흐름도

## 4-1. 공통 흐름

```text
Client
  └─ POST /v1/{domain}/omr
       ├─ 파일 검증
       ├─ pending 엔티티 생성 (omrStatus=PENDING)
       ├─ 이벤트 발행
       └─ 즉시 응답 반환

AFTER_COMMIT + @Async EventListener
  └─ OmrClient.submitJob(fileData, filename, entityPublicId)
       ├─ dev: /omr/dev/process + callback_url
       └─ prod: /omr/prod/process
  └─ 엔티티에 omrJobId 저장
  └─ omrStatus=PROCESSING, progress=10

OMR Server
  └─ POST /v1/{domain}/omr/callback
       ├─ X-OMR-Callback-API-Key 검증
       ├─ job_id → publicId 역조회
       ├─ completed 이면 fetchMusicXml/fetchChordAssignments
       ├─ Processor.processJobResult(jobId)
       ├─ 도메인 데이터 계산/저장
       └─ omrStatus=COMPLETED 또는 FAILED
```

## 4-2. Lick / Solo 완료 시 내부 흐름

```text
CallbackController
  -> Service.handleOmrCallback()
      -> Reader.findByPublicId(UUID.fromString(job_id))
      -> OmrProcessor.processJobResult(job_id)
      -> buildOmrCreateRequest(existingPendingEntity, processedSheetData)
      -> FeatureCalculator.computeHarmonicData()
      -> FeatureCalculator.computeFeatures()
      -> Writer.completePending()
```

## 4-3. ChordProject 완료 시 내부 흐름

```text
CallbackController
  -> ChordProjectService.handleOmrCallback()
      -> project.omrRequestedTitle/key/timeSignature 확인
      -> ChordProjectOmrProcessor.processJobResult(job_id)
      -> 사용자 입력값 우선 적용
      -> ChordProjectOmrWriter.complete()
```

---

## 5. 설계 의도

1. **폴링 제거**
   - 사용자 요청대로, callback 기반 비동기 API에서 폴링을 제거했다.
   - 서버 스레드 점유, 불필요한 재시도, OMR 서버 부하를 줄이는 목적이다.

2. **job_id = publicId 문자열 통일**
   - 모든 도메인에서 OMR 제출 시 `job_id`로 우리 엔티티의 `publicId.toString()`을 사용했다.
   - callback 수신 시 `UUID.fromString(job_id)`만으로 바로 역조회 가능하다.

3. **도메인별 pending 상태 유지**
   - OMR 처리 시간이 길어질 수 있으므로, 제출 즉시 엔티티를 생성해서 프론트가 상태를 조회할 수 있게 했다.
   - `LickResponse`, `SoloResponse`에도 OMR 상태를 포함시켜 추가 status API 없이 기존 단건 조회 API로 상태 확인 가능하다.

4. **사용자 override 보존**
   - ChordProject는 제목/조성/박자표 override 개념이 분명해 엔티티에 따로 저장했다.
   - Lick/Solo는 입력 메타데이터 자체를 pending 엔티티 필드에 저장하고, callback 완료 시 그 값을 기반으로 최종 생성 request를 재구성했다.

---

## 6. 제대로 지시되지 않아 임의로 결정한 부분

1. **Lick/Solo 별도 status endpoint 미추가**
   - 기존 `GET /v1/licks/{publicId}`, `GET /v1/solos/{publicId}` 응답에 OMR 상태를 포함시키는 방식으로 해결했다.
   - 별도 `/omr-status` 엔드포인트를 새로 만들 수도 있었지만, API 증가를 피하기 위해 기존 단건 조회 응답을 확장했다.

2. **Lick/Solo callback 경로**
   - 각각 아래 경로로 정했다.
   - `POST /v1/licks/omr/callback`
   - `POST /v1/solos/omr/callback`

3. **pending 제목 기본값**
   - metadata에 title이 없으면 업로드 파일명(base name)을 우선 사용하고, 그것도 없으면 `OMR Processing`을 기본값으로 사용했다.

4. **Lick/Solo의 동기 `process(file)` 유지 방식**
   - 완전 제거 대신, 명시적으로 예외를 던지는 deprecated 메서드로 유지했다.
   - 기존 호출부를 빠르게 발견하기 위함이다.

---

## 7. 개발자가 알아둬야 하는 사항

### 환경 변수

| 변수 | 설명 |
|---|---|
| `OMR_SERVER_URL` | OMR 서버 주소 |
| `OMR_API_KEY` | OMR 서버 요청 헤더 `X-OMR-API-Key` |
| `OMR_CALLBACK_API_KEY` | 콜백 검증 헤더 `X-OMR-Callback-API-Key` |
| `OMR_CALLBACK_URL` | dev 환경에서만 OMR 서버에 전달할 callback URL |

### dev / prod 차이

- dev: `omr.callback-url` 값이 있으면 `/omr/dev/process` 사용
- prod: `omr.callback-url` 비워두면 `/omr/prod/process` 사용

### 상태 확인 방식

| 도메인 | 상태 확인 API |
|---|---|
| SheetProject | `GET /v1/sheet-projects/{publicId}` 또는 `/omr-status` |
| ChordProject | `GET /v1/chord-projects/{publicId}` 또는 `/omr-status` |
| Lick | `GET /v1/licks/{publicId}` |
| Solo | `GET /v1/solos/{publicId}` |

### 테스트 결과

다음 검증을 직접 수행했다.

- `gradlew.bat compileJava` ✅
- `gradlew.bat test` ✅

테스트에는 다음이 포함된다.
- `OmrClient` async API 테스트
- 각 OMR processor의 `processJobResult(jobId)` 테스트
- 각 event listener의 submit-only 동작 테스트

---

## 8. 후속 권장 작업

1. `LickControllerSpec`, `SoloControllerSpec`의 Swagger 설명 문구를 동기 처리 기준에서 비동기 처리 기준으로 더 상세히 갱신
2. 프론트엔드에서 `LickResponse` / `SoloResponse`의 `omrStatus`, `omrProgress`, `omrFailureReason` 표시 지원
3. callback 요청의 source IP allowlist 또는 HMAC 검증 추가 검토
4. 필요 시 `Lick` / `Solo`에도 별도 `GET /omr-status` 엔드포인트 추가 검토

