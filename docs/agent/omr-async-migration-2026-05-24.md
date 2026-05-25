# OMR 비동기 API 마이그레이션

**작성일**: 2026-05-24  
**적용 범위**: `shared/omr`, `domain/sheetproject` (전면), `core/security`

---

## 1. 작업 개요

MusicVision OMR 서버가 기존 동기식(`POST /omr/process`) API에서 비동기식으로 전환됨에 따라, 백엔드의 OMR 처리 흐름을 전면 수정하였다.

- **이전**: 파일 업로드 → OMR 서버가 처리 완료 후 응답 반환 (동기식, blocking)
- **이후**: 파일 업로드 → 202 Accepted (jobId 반환) → OMR 서버가 완료 후 콜백 전송 (비동기식)

---

## 2. 변경된 파일 목록

| 파일 | 변경 유형 | 요약 |
|------|-----------|------|
| `shared/omr/OmrProperties.java` | 수정 | `apiKey`, `callbackApiKey`, `callbackUrl` 필드 추가 |
| `shared/omr/OmrClient.java` | 전면 재설계 | `recognize()`(폴링 기반), `submitJob()`, `fetchMusicXml()`, `fetchChordAssignments()` |
| `shared/exception/code/OmrErrorCode.java` | 수정 | `OMR_SUBMIT_FAILED`, `OMR_CALLBACK_KEY_INVALID`, `OMR_JOB_NOT_FOUND` 추가 |
| `domain/sheetproject/entity/SheetProject.java` | 수정 | `omrJobId` 필드 추가, `storeOmrJobId()` 메서드 추가 |
| `domain/sheetproject/service/implementation/SheetProjectOmrProcessor.java` | 전면 수정 | `process(file)` 제거 → `processJobResult(jobId)` 추가 |
| `domain/sheetproject/service/implementation/SheetProjectOmrWriter.java` | 수정 | `storeJobIdAndMarkProcessing()` 추가 |
| `domain/sheetproject/event/SheetProjectOmrEventListener.java` | 전면 수정 | OMR 서버에 파일 제출 + job_id 저장만 담당(결과 처리 분리) |
| `domain/sheetproject/dto/request/OmrCallbackRequest.java` | 신규 | OMR 서버 콜백 페이로드 DTO |
| `domain/sheetproject/controller/SheetProjectOmrCallbackController.java` | 신규 | 콜백 수신 REST 컨트롤러 |
| `domain/sheetproject/controller/SheetProjectOmrCallbackControllerSpec.java` | 신규 | Swagger 명세 인터페이스 |
| `domain/sheetproject/service/SheetProjectService.java` | 수정 | `handleOmrCallback()` 추가, `SheetProjectOmrProcessor` 의존 추가 |
| `core/security/SecurityConfig.java` | 수정 | 콜백 엔드포인트 `permitAll()` 추가 |
| `resources/application-dev.yml` | 수정 | `omr.api-key`, `omr.callback-api-key`, `omr.callback-url` 추가 |
| `resources/application-prod.yml` | 수정 | `omr.api-key`, `omr.callback-api-key` 추가 (callback-url 없음) |

---

## 3. 클래스 역할 표

| 클래스 | 역할 |
|--------|------|
| `OmrProperties` | OMR 연동 설정 보유: serverUrl, apiKey, callbackApiKey, callbackUrl |
| `OmrClient` | OMR 서버 HTTP 통신 전담. dev/prod 엔드포인트 구분, API 키 헤더 처리, 폴링 기반 동기 보조 메서드 제공 |
| `SheetProjectOmrEventListener` | 트랜잭션 커밋 후 비동기 실행. OMR 서버에 파일 제출하고 job_id를 저장. 결과 처리는 콜백에 위임 |
| `SheetProjectOmrCallbackController` | `POST /v1/sheet-projects/omr/callback` 수신. X-OMR-Callback-API-Key 검증 후 서비스 위임 |
| `SheetProjectOmrCallbackControllerSpec` | 콜백 컨트롤러 Swagger 명세 인터페이스 |
| `OmrCallbackRequest` | OMR 서버 콜백 JSON 페이로드 DTO (record) |
| `SheetProjectService.handleOmrCallback()` | 콜백 처리 오케스트레이터. 키 검증 → 프로젝트 조회 → completed/failed 분기 |
| `SheetProjectOmrProcessor.processJobResult()` | job_id로 MusicXML + chord assignments 조회 후 도메인 데이터로 파싱 |
| `SheetProjectOmrWriter.storeJobIdAndMarkProcessing()` | job_id 저장 + PROCESSING 상태 전환 |

---

## 4. 비동기 처리 흐름도

### 4-1. SheetProject OMR 요청 흐름 (dev/prod 공통)

```
Frontend
  │─ POST /v1/sheet-projects/omr (multipart)
  │
Backend (SheetProjectService.createFromOmr)
  ├─ 파일 유효성 검증
  ├─ SheetProject 생성 (omrStatus=PENDING)
  ├─ storageFile 업로드
  ├─ ApplicationEventPublisher.publishEvent(SheetProjectOmrRequestedEvent)
  └─ 202 응답 반환 (jobId 포함) ──→ Frontend

[Transaction AFTER_COMMIT 비동기]
SheetProjectOmrEventListener.handle()
  ├─ omrClient.submitJob(fileData, filename, projectPublicId.toString())
  │     ├─ dev 모드 (callbackUrl 설정 시): POST /omr/dev/process + callback_url 포함
  │     └─ prod 모드: POST /omr/prod/process
  ├─ sheetProjectOmrWriter.storeJobIdAndMarkProcessing(projectPublicId, jobId, 10)
  │     └─ SheetProject: omrJobId 저장, omrStatus=PROCESSING(10%)
  └─ 종료 (결과 처리는 콜백 대기)
```

### 4-2. OMR 콜백 처리 흐름

```
OMR Server
  │─ POST /v1/sheet-projects/omr/callback
  │     Headers: X-OMR-Callback-API-Key: <key>
  │     Body: { "job_id": "...", "status": "completed"|"failed", ... }

SheetProjectOmrCallbackController.handleCallback()
  ├─ Header 추출 → SheetProjectService.handleOmrCallback(key, payload)

SheetProjectService.handleOmrCallback()
  ├─ validateCallbackApiKey() → 키 불일치 시 OMR_CALLBACK_KEY_INVALID(401)
  ├─ UUID.fromString(job_id) → SheetProject 조회 (없으면 OMR_JOB_NOT_FOUND)
  │
  ├─ [status=completed]
  │     ├─ markProcessing(80%)
  │     ├─ sheetProjectOmrProcessor.processJobResult(jobId)
  │     │     ├─ omrClient.fetchMusicXml(jobId)  → GET /omr/jobs/{jobId}/musicxml
  │     │     ├─ omrClient.fetchChordAssignments(jobId)
  │     │     └─ MusicXmlParser.parse() → SheetProjectOmrData
  │     └─ sheetProjectOmrWriter.complete() → omrStatus=COMPLETED(100%)
  │
  └─ [status=failed]
        └─ sheetProjectOmrWriter.fail() → omrStatus=FAILED
```

### 4-3. ChordProject / Lick / Solo 도메인 (미전환 상태)

OMR API가 동기 처리를 지원하지 않으므로, 이 도메인들의 `process(file)` 메서드는 현재 `OMR_RECOGNITION_FAILED` 예외를 던진다.  
각 프로세서에는 `processJobResult(jobId)` 메서드가 준비되어 있으며, 추후 SheetProject와 동일한 콜백 기반 비동기 흐름으로 전환 시 사용한다.

```
LickOmrProcessor.process(file)  →  throws OMR_RECOGNITION_FAILED ("아직 비동기 콜백 방식으로 전환되지 않음")
SoloOmrProcessor.process(file)  →  throws OMR_RECOGNITION_FAILED
ChordProjectOmrProcessor.process(file)  →  throws OMR_RECOGNITION_FAILED
```

---

## 5. dev vs prod 환경 구분

| 항목 | dev | prod |
|------|-----|------|
| OMR 엔드포인트 | `POST /omr/dev/process` | `POST /omr/prod/process` |
| callback_url 전달 | ✅ (omr.callback-url 설정값) | ❌ (OMR 서버에 정적 등록) |
| `omr.callback-url` 설정 | 필요 (예: `http://localhost:8080/api/v1/sheet-projects/omr/callback`) | 불필요 (빈 값) |
| `omr.api-key` | 선택 (설정 시 X-OMR-API-Key 헤더 추가) | 필수 권장 |
| `omr.callback-api-key` | 선택 (설정 시 검증, 미설정 시 검증 생략) | 필수 권장 |

---

## 6. 환경 변수 추가 목록

| 환경 변수 | 설명 | 필수 여부 |
|-----------|------|-----------|
| `OMR_SERVER_URL` | OMR 서버 베이스 URL | 필수 |
| `OMR_API_KEY` | OMR 서버 요청 인증 키 (X-OMR-API-Key) | 운영 시 필수 |
| `OMR_CALLBACK_API_KEY` | OMR 콜백 검증 키 (X-OMR-Callback-API-Key) | 운영 시 필수 |
| `OMR_CALLBACK_URL` | dev 환경에서 OMR 서버에 전달할 콜백 URL | dev에서 필요 |

---

## 7. 임의로 결정하고 행동한 부분

1. **job_id = projectPublicId.toString()**: 콜백 수신 시 역조회를 위해 OMR 서버에 전달하는 job_id로 프로젝트 publicId UUID 문자열을 사용했다. UUID는 hyphen 포함이지만 허용된 문자 범위(`[a-zA-Z0-9_-]`)에 해당한다. 이를 통해 SheetProject 엔티티에 `omrJobId`를 추가했지만, 실제로는 publicId로 역조회 가능하여 저장된 omrJobId는 디버깅/감사 목적이다.

2. **ChordProject/Lick/Solo 도메인 폴링 방식 유지**: 이 세 도메인은 콜백 기반 비동기 흐름으로 전환하면 각각 pending 상태 관리, 콜백 엔드포인트 추가 등 대규모 리팩토링이 필요하다. 요구사항이 SheetProject 위주였으므로, 기존 `recognize()` 인터페이스를 유지하되 내부에서 비동기 엔드포인트 + 폴링으로 동작하도록 구현했다. 향후 비동기로 전환 검토 필요.

3. **콜백 API 키 미설정 시 검증 생략**: `omr.callback-api-key`가 빈 값이면 키 검증을 건너뛰도록 했다. 로컬 개발 편의를 위한 결정이며, 운영 환경에서는 반드시 설정해야 한다.

4. **콜백 엔드포인트 경로**: `/v1/sheet-projects/omr/callback`으로 정했다. 문서에는 경로가 명시되지 않았으므로 임의로 결정했다.

5. **ChordProject/Lick/Solo 미전환**: 이 도메인들의 `process(file)`은 예외를 던지도록 처리했다. 각 프로세서에 `processJobResult(jobId)` 메서드를 미리 작성해 두었으므로, 해당 도메인에 콜백 수신 인프라(pending 상태, 이벤트, 콜백 라우팅)를 구축한 후에 연결하면 된다.

---

## 8. 개발자가 알아둬야 하는 사항

### 설정
- dev 환경에서 로컬 테스트 시, `OMR_CALLBACK_URL`에 ngrok 등 공개 URL을 설정하거나, OMR 서버와 같은 네트워크에서 직접 접근 가능한 주소를 사용해야 한다.
- prod 환경에서는 OMR 서버 담당자에게 `https://your-domain.com/api/v1/sheet-projects/omr/callback`을 `OMR_CALLBACK_URL`로 등록 요청해야 한다.

### 보안
- `X-OMR-Callback-API-Key` 헤더 검증 로직: `OmrProperties.callbackApiKey()`가 비어있으면 검증을 생략한다. **운영 환경에서 반드시 설정할 것.**
- 콜백 엔드포인트(`POST /v1/sheet-projects/omr/callback`)는 JWT 인증 없이 동작하지만, API 키 헤더로 보호된다.

### 트랜잭션 주의
- `SheetProjectOmrEventListener`는 `@Async` + `@TransactionalEventListener(phase = AFTER_COMMIT)` 로 동작한다.
- 이벤트 리스너 내에서 `SheetProjectOmrWriter.storeJobIdAndMarkProcessing()`을 호출하므로, 해당 Writer가 `@Transactional`이어야 DB에 반영된다.
- 콜백 처리(`handleOmrCallback`)는 `@Transactional`로 감싸져 있어 MusicXML 파싱 실패 시 DB에 중간 상태가 저장되지 않는다.

### 폴링 사용 도메인 (ChordProject / Lick / Solo)
- 이 도메인들의 `process(file)` 메서드는 현재 명시적으로 예외를 던진다.
- 각 프로세서에 `processJobResult(jobId)` 메서드가 준비되어 있으므로, 해당 도메인을 SheetProject와 동일한 구조(pending 엔티티 상태, 이벤트, 콜백 라우팅)로 전환한 후 연결하면 된다.




