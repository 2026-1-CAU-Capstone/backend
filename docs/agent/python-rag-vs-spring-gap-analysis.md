# Python RAG+LLM 구현 vs 현재 Spring 구현 비교 분석

## 1. 작업한 내용

이번 문서는 다음 대상을 기준으로 비교했다.

- Python RAG/LLM 구현
  - `rag/server.py`
  - `rag/agent.py`
  - `rag/retrieve.py`
  - `rag/1_chunk.py`
  - `rag/2_embed.py`
- 설계/운영 문서
  - `docs/LLM_Guide.md`
  - `docs/Rag_guide.md`
- 현재 Spring 구현
  - `src/main/java/com/jazzify/backend/domain/chat/**`
  - `src/main/java/com/jazzify/backend/domain/rag/**`
  - `src/main/java/com/jazzify/backend/shared/llm/**`
  - `src/main/resources/application-dev.yml`
  - `src/main/resources/application-prod.yml`
  - `src/main/java/com/jazzify/backend/core/security/SecurityConfig.java`
- 프론트 연동 상태 확인용
  - `api/claude.ts`
  - `api/harmorag.ts`

목표는 **Python 서버/문서 기준으로 현재 Spring 코드가 어디까지 반영되었는지**, **무엇이 달라졌는지**, **실제로 수정할 가치가 높은 지점이 무엇인지**를 정리하는 것이다.

---

## 2. 설계의도

이 문서는 단순 diff 목록이 아니라, 앞으로의 의사결정을 돕기 위한 **갭 분석 문서**로 작성했다.

특히 아래 3가지를 중심으로 봤다.

1. **기능 동등성**: Python RAG가 하던 일을 Spring이 얼마나 재현했는가
2. **운영 적합성**: 인증, 저장, 배포, 설정, 장애 포인트가 개선되었는가
3. **마이그레이션 완결성**: 문서에서 제안한 구조가 실제로 end-to-end로 연결되었는가

---

## 3. 결론 요약

한 줄 요약:

> **Spring 쪽은 Python RAG를 상당 부분 잘 옮겼고, 오히려 문서 CRUD/채팅 저장/보안 측면에서는 더 앞서 있다. 다만 프롬프트 소스 분산, 프론트 미전환, 임베딩 서버 계약 미확정, URL/보안 정책 불일치가 현재 가장 큰 갭이다.**

핵심만 먼저 정리하면:

- **이미 잘 옮겨진 부분**
  - RAG 쿼리 분해 + RRF 융합 로직
  - RAG debug block 선전송 후 토큰 스트리밍
  - 문서 chunking/metadata/tag 구조
  - `/v1/chat/stream`, `/v1/rag/chat` 형태의 백엔드 LLM 스트리밍 엔드포인트
  - 채팅 이력 저장
  - RAG 문서 CRUD

- **아직 큰 차이가 남아 있는 부분**
  - 프론트가 아직 `api/claude.ts`에서 브라우저 직통 Anthropic 호출을 유지 중
  - Direct Chat용 프롬프트와 RAG용 프롬프트가 서로 다름
  - Spring RAG는 외부 임베딩 서버 전제인데, Python은 로컬 임베딩 + ChromaDB라 운영 모델이 다름
  - 문서에는 `/v1/...`로 적혀 있지만 실제 서버는 `context-path: /api`가 걸려 있음
  - 인증 정책은 문서의 “혼합/옵션형”이 아니라 현재는 사실상 **로그인 필수**

---

## 4. 영역별 상세 비교

### 4.1 전체 아키텍처

| 항목 | Python / 문서 기준 | 현재 Spring 구현 | 판단 |
|---|---|---|---|
| Direct Chat 엔드포인트 | `docs/LLM_Guide.md`에서 `POST /v1/chat/stream` 제안 | `ChatController`에 `POST /v1/chat/stream` 존재 | **구현됨** |
| RAG Chat 엔드포인트 | Python `POST /chat` | `RagController`에 `POST /v1/rag/chat` 존재 | **구현됨** |
| 문서 CRUD | Python에는 없음, `Rag_guide.md` Part B에서 향후 설계 | Spring에 `/v1/rag/documents` CRUD 있음 | **Spring이 더 앞섬** |
| 채팅 저장 | Python 없음 | Spring `ChatService`에서 direct/rag 모두 저장 | **Spring이 더 앞섬** |
| 인증 | Python 없음 | Spring Security로 대부분 인증 필요 | **Spring이 더 운영 친화적** |

### 4.2 RAG 검색 파이프라인

#### Python 기준

- `rag/agent.py`
  - chord context → sub-query 분해
  - query별 검색
  - RRF 융합
  - debug 정보 구성
- `rag/retrieve.py`
  - `SentenceTransformer`로 직접 임베딩
  - ChromaDB에서 cosine 검색

#### Spring 기준

- `RagAgent`
  - `decomposeQuery()` 구조가 Python과 거의 동일
  - `routeAndRetrieve()`에서 RRF 수행
  - debug JSON 구조도 Python과 거의 동일
- `RagReader` + `RagChunkRepository`
  - 임베딩 벡터를 받아 pgvector 검색

#### 차이점

1. **검색 저장소가 다름**
   - Python: ChromaDB 파일 기반
   - Spring: PostgreSQL + pgvector

2. **임베딩 생성 방식이 다름**
   - Python: `sentence-transformers/paraphrase-multilingual-mpnet-base-v2`를 프로세스 내에서 직접 사용
   - Spring: `RagEmbeddingClient`가 외부 임베딩 서버에 HTTP 호출

3. **운영 리스크 위치가 다름**
   - Python은 단일 프로세스형이라 단순
   - Spring은 DB와 별도 임베딩 서버까지 필요해 구성 요소가 늘어남

#### 판단

- **로직 동등성은 높다.**
- 하지만 **운영 토폴로지는 크게 달라졌다.**
- 특히 `RagEmbeddingClient`에 `TODO: 실제 임베딩 서버 요청 포맷이 확정되면...`가 남아 있어, 현재 Spring RAG의 가장 큰 불확실성은 **retrieval 로직이 아니라 embedding contract**다.

### 4.3 chunking / bootstrap

#### Python 기준

- `1_chunk.py`
  - `data/explanation/{standards,lessons}` 파싱
  - metadata 추출
  - section 분리
  - instruction/response 분리
  - topic tag 부여
- `2_embed.py`
  - chunks.json → 임베딩 → ChromaDB 적재

#### Spring 기준

- `RagFileChunker`
  - Python의 파싱 로직을 거의 동일하게 옮김
- `RagWriter`
  - 문서 저장 시 chunk 재생성 + 재임베딩
- `RagBootstrapRunner`
  - 앱 시작 시 schema 생성/파일 bootstrap 옵션 실행

#### 차이점

1. **Spring은 bootstrap + 운영 CRUD가 결합되어 있음**
   - Python은 오프라인 파이프라인
   - Spring은 런타임에 문서 생성/수정 즉시 재임베딩

2. **Spring 쪽이 더 제품형 구조**
   - `embeddingVersion`, `slug`, `publicId`, `topicTags`, `metadata` 관리가 더 명시적

3. **DB 차원 검증은 약함**
   - Python은 모델이 고정이라 사실상 768차원 전제
   - Spring의 `rag_chunk.embedding vector`는 차원 고정 선언이 없어, DB 레벨에서 벡터 차원 일관성 보호가 약하다

#### 판단

- chunking 이식은 잘 되어 있다.
- 오히려 현재 구조는 문서에서 말한 “Part B 방향”을 일부 선반영한 상태다.
- 다만 **임베딩 차원/계약 검증 강화**가 필요하다.

### 4.4 LLM 호출과 스트리밍

#### Python 기준

- `rag/server.py`
  - `RAG_DEBUG` 블록 먼저 전송
  - 이후 Claude text stream 전달
  - `text/plain`

#### Spring 기준

- `RagChatStreamer`
  - debug block 먼저 작성
  - `AnthropicStreamingClient`로 plain text 토큰 전송
- `ChatController`, `RagController`
  - `StreamingResponseBody`
  - `X-Accel-Buffering: no`
  - `Cache-Control: no-cache, no-transform`

#### 차이점

1. **스트리밍 보존 헤더는 Spring이 더 명확함**
2. **채팅 public id 헤더를 Spring이 추가 제공**
3. **Spring direct chat과 RAG chat이 동일한 스트리밍 패턴을 공유**

#### 판단

- 스트리밍 구현은 Spring이 더 성숙하다.
- Python 대비 후퇴한 부분은 거의 없다.

### 4.5 시스템 프롬프트 / 응답 규칙

이 부분이 현재 **가장 중요한 차이점**이다.

#### Python / 문서 기준

- `rag/server.py`의 `BASE_SYSTEM`은 한국어 중심의 긴 프롬프트
- `docs/LLM_Guide.md`는 direct chat도 이 규칙을 백엔드로 옮기라고 제안
- 문서에서는 **single source of truth** 권장

#### 현재 Spring 기준

- `RagChatStreamer.BASE_SYSTEM`
  - Python `rag/server.py` 프롬프트와 매우 유사
- `ClaudeChatStreamer.BASE_SYSTEM`
  - 현재 `api/claude.ts`의 구버전 direct Claude 프롬프트와 유사
  - 훨씬 짧고, 페르소나/출력 규칙이 Python RAG 프롬프트만큼 상세하지 않음

#### 실제 의미

현재는 프롬프트가 최소 4곳에 나뉘어 있다.

1. `rag/server.py`
2. `RagChatStreamer`
3. `ClaudeChatStreamer`
4. `api/claude.ts`

즉, **RAG 응답과 Direct Chat 응답의 성격이 달라질 수 있다.**

예를 들면:

- RAG 쪽은 한국어 페르소나/차트 출력 규칙/출처 은닉/대화 톤 가이드가 더 풍부함
- Direct Chat 쪽은 보다 단순한 English-base 규칙 중심

#### 판단

- 기능적 버그보다 더 위험한 **품질 일관성 문제**다.
- 문서 기준으로는 **Prompt Catalog를 한 곳으로 통합하는 작업이 가장 우선순위가 높다.**

### 4.6 category 분석 프롬프트

#### Python / 문서 기준

- `docs/LLM_Guide.md`에서 `overview`, `functional`, `iiVI`, `secondary`, `modal`, `improv` 6개 카테고리 제안
- 원본은 `api/claude.ts`

#### Spring 기준

- `ChatAnalysisCategory`가 6개 카테고리를 정확히 갖고 있음
- `ClaudeChatStreamer.buildSystem()`에서 category prompt 조립

#### 판단

- 이 부분은 **Spring이 잘 반영했다.**
- 다만 category는 direct chat 쪽에만 있고, RAG 쪽에는 직접적인 category 개념이 아직 없다.
- 만약 RAG 응답도 동일한 분석 모드 UX를 제공해야 한다면, `RagChatRequest`에도 category 확장이 필요하다.

### 4.7 chord context 처리

#### Python RAG

- `chord_context_text`를 system prompt에 `[Rule-based 분석 결과]`로 삽입
- `chord_context`는 query decomposition용 구조화 데이터

#### Spring RAG

- Python과 동일하게 반영됨
- `RagChatRequest`
  - `chordContext` : 구조화 맵
  - `chordContextText` : 문자열

#### Spring Direct Chat

- `ChatStreamRequest.chordContext`를 user message 안의 `[Chord Analysis Context]` 블록으로 감쌈
- 문서 제안과 일치

#### 판단

- RAG와 Direct Chat의 context 주입 방식은 서로 다르지만, 각각 의도에 맞게 구현되어 있다.
- 큰 문제는 아니다.
- 다만 문서화가 더 필요하다. 같은 “chord context”라도 direct/rag가 사용 위치가 다르기 때문이다.

### 4.8 인증 / 접근 정책

#### Python / 문서 기준

- Python 서버는 인증 없음
- `docs/LLM_Guide.md`는 다음 정책을 옵션으로 제안
  - 로그인 필수
  - 익명 허용 + rate limit
  - 혼합 정책

#### Spring 기준

- `SecurityConfig`에서 `/v1/chat/**`, `/v1/rag/chat`, `/v1/rag/search`, `/v1/rag/documents` 등은 기본적으로 인증 필요
- 예외는 `/v1/rag/health` 정도만 공개

#### 판단

- 보안 관점에서는 Spring이 더 낫다.
- 하지만 **현재 프론트 동작 가정과는 어긋날 가능성**이 있다.
- 특히 `docs/LLM_Guide.md`의 “비로그인도 단기 허용 가능”과는 다르게, 현 상태는 사실상 **로그인 필수 정책**이다.

### 4.9 채팅 저장 / 대화 이력

#### Python 기준

- 별도 저장 없음
- 요청 body의 history만 신뢰

#### Spring 기준

- `ChatService`가 direct/rag 모두 `Chat` + `ChatMessage`로 영속화
- 기존 chatPublicId가 있으면 과거 메시지를 DB에서 로드
- 응답 완료 후 user/assistant turn 저장

#### 판단

- 제품 관점에서 Spring 구현이 훨씬 낫다.
- 다만 프론트가 여전히 과거 Python식 history 전달 중심으로 동작하면, “서버 저장 히스토리 vs 클라이언트 전송 히스토리”의 책임 경계가 애매할 수 있다.
- 장기적으로는 **서버 저장 이력을 정본(source of truth)으로 삼는 방향**이 좋다.

### 4.10 프론트 연동 상태

이 항목은 문서 대비 현재 상태를 볼 때 매우 중요하다.

#### 문서 기대

- `docs/LLM_Guide.md`는 프론트의 `api/claude.ts` 직접 Anthropic 호출을 제거하고 Spring `/v1/chat/stream`으로 바꾸라고 함

#### 현재 실제 상태

- `api/claude.ts`
  - 여전히 `VITE_ANTHROPIC_API_KEY`
  - 브라우저에서 `https://api.anthropic.com/v1/messages` 직접 호출
- `api/harmorag.ts`
  - 여전히 `VITE_RAG_BASE`로 별도 RAG 서버를 바라봄
  - 실패 시 direct Claude 브라우저 호출로 폴백

#### 의미

- **백엔드 엔드포인트는 생겼지만 프론트 전환은 아직 안 끝났다.**
- 즉, 문서의 핵심 목적이었던 “브라우저 키 제거”는 현재 코드베이스 전체 기준으로는 아직 완료되지 않았다.

#### 판단

- 이건 분석상 가장 명확한 “미완료 항목”이다.
- 백엔드 구현보다 프론트 전환이 지금 더 급하다.

### 4.11 URL / 배포 문서 불일치

#### 문서 기준

- `POST /v1/chat/stream`
- `POST /v1/rag/chat`

#### 실제 설정

- `application-dev.yml`, `application-prod.yml` 모두 `server.servlet.context-path: /api`
- 실제 런타임 URL은 일반적으로 `/api/v1/chat/stream`, `/api/v1/rag/chat`

#### 판단

- 프록시에서 `/api`를 제거하는 구성이 없다면, 문서와 실제 URL이 다르다.
- 이 차이는 배포/프론트 연결 시 매우 자주 장애를 만든다.
- **문서 또는 서버 설정 중 하나를 기준으로 통일할 필요가 있다.**

### 4.12 모델 / 토큰 설정

#### Python 기준

- `rag/server.py`: `claude-sonnet-4-6`, `max_tokens=4096`

#### Spring 기준

- dev: `claude-sonnet-4-6`, `max-tokens=16384`
- prod도 Anthropic 설정은 있으나 dev와 프로퍼티 형태가 다소 다름

#### 판단

- 모델명은 어느 정도 통일됐지만, 토큰 크기와 설정 키 형태는 정리 필요성이 있다.
- 최소한 **dev/prod Anthropic 설정 구조는 하나로 맞추는 편이 안전**하다.
- 응답 길이/비용/latency 기대치도 direct/rag 간 맞춰야 한다.

---

## 5. 현재 Spring 구현이 Python/문서보다 나은 점

아래는 단순 “차이”가 아니라 **Spring이 실제로 더 좋아진 부분**이다.

1. **채팅 저장 구조 도입**
   - Python에는 없던 `Chat`, `ChatMessage` 영속화가 있음

2. **문서 CRUD 제공**
   - Python은 파일 중심이고 관리 UI/운영 API가 없음
   - Spring은 `/v1/rag/documents`로 운영 가능성이 높음

3. **스트리밍 응답 헤더 보강**
   - `X-Accel-Buffering: no`, `Cache-Control: no-cache, no-transform`

4. **보안 체계 통합**
   - JWT 기반 인증과 같은 백엔드 정책에 자연스럽게 편입됨

5. **pgvector 기반 중앙 저장소**
   - ChromaDB 파일 기반보다 서버 운영/백업/관리 측면에서 일관성이 좋음

---

## 6. 실제 수정 우선순위 제안

### P0 — 가장 먼저 손봐야 할 것

#### 1) 프론트의 direct Claude 호출 제거

대상:
- `api/claude.ts`
- `api/harmorag.ts`

이유:
- 지금도 `VITE_ANTHROPIC_API_KEY`가 브라우저 번들로 노출될 수 있음
- `docs/LLM_Guide.md`의 핵심 목표가 아직 완료되지 않음

권장 방향:
- `api/claude.ts` → Spring `POST /api/v1/chat/stream` 호출로 교체
- `api/harmorag.ts` → Spring `POST /api/v1/rag/chat` 또는 프록시 경로 사용
- 프론트 `.env`에서 `VITE_ANTHROPIC_API_KEY` 제거

#### 2) 프롬프트 단일화

대상:
- `RagChatStreamer`
- `ClaudeChatStreamer`
- 필요 시 프론트 상수 제거

이유:
- direct/rag 응답 톤과 규칙이 달라질 수 있음
- 프롬프트가 여러 군데 흩어져 수정 누락 위험이 큼

권장 방향:
- 예: `shared/llm/prompt/JazzifyPromptCatalog` 같은 공용 구성으로 추출
- 최소 구성
  - base system
  - context suffix
  - analysis category prompt
  - user content builder 규칙

#### 3) 임베딩 서버 계약 확정

대상:
- `RagEmbeddingClient`
- 설정 문서

이유:
- 지금 Spring RAG에서 가장 큰 불확실성
- TODO 상태라 운영 투입 시 장애 가능성이 큼

선택지:
- **A. Python처럼 백엔드 내부 임베딩 내장**
- **B. 지금 구조 유지하되 임베딩 서버 API 계약 확정 + 헬스체크 + 실패 처리 강화**

### P1 — 다음으로 정리할 것

#### 4) URL 기준 통일 (`/v1` vs `/api/v1`)

대상:
- `docs/LLM_Guide.md`
- 프론트 base URL 설정
- 필요 시 reverse proxy

이유:
- 문서와 런타임 경로가 다르면 운영 중 혼선이 큼

#### 5) 인증 정책 명문화

대상:
- `SecurityConfig`
- 개발/운영 문서

이유:
- 현재는 사실상 로그인 필수
- 그런데 문서와 프론트 UX는 익명 허용 가능성을 전제로 설명하는 부분이 있음

결정 포인트:
- 완전 로그인 필수
- 익명 허용 + rate limiting
- direct만 제한 / rag만 제한 등 분리 정책

#### 6) Direct Chat / RAG Chat 기능 경계 문서화

이유:
- direct는 category + image 지원
- rag는 debug + retrieval 컨텍스트 강점
- 둘의 차이를 팀이 명확히 알아야 프론트 설계가 흔들리지 않음

### P2 — 중장기 개선

#### 7) RAG에도 category 개념 도입 여부 검토

이유:
- 현재 category 프롬프트는 direct chat 전용
- 사용자가 “RAG 기반 functional analysis” 같은 UX를 원하면 rag에도 필요

#### 8) 벡터 차원/스키마 검증 강화

이유:
- 768차원 불일치가 나도 DB가 늦게 문제를 드러낼 수 있음
- schema, bootstrap, health에서 명시 검증하는 편이 안전

#### 9) 테스트 보강

추가되면 좋은 테스트:
- direct/rag 프롬프트 조립 결과가 의도대로 같은 핵심 규칙을 포함하는지
- `/chat/stream`, `/rag/chat` 스트림 포맷 호환성
- 임베딩 서버 응답 포맷 검증
- 프론트가 `RAG_DEBUG` 블록을 계속 파싱 가능한지

---

## 7. 추천 작업 순서

### 추천 시나리오 A — 실서비스 안정화 우선

1. 프론트 `api/claude.ts`를 Spring `/api/v1/chat/stream`으로 전환
2. `VITE_ANTHROPIC_API_KEY` 제거
3. 프롬프트 공통화
4. URL 기준 정리
5. 인증 정책 문서화

### 추천 시나리오 B — RAG 품질/운영 완성도 우선

1. 임베딩 서버 계약 확정
2. health 체크에 embedding server reachability 추가
3. 벡터 차원 검증 추가
4. bootstrap/reindex 운영 절차 문서화
5. 이후 프론트 전환

실제 우선순위는 **A → B**를 추천한다.

이유:
- 지금 가장 큰 리스크는 보안/프론트 직통 호출
- 그 다음이 운영 구조의 불명확성(임베딩 서버)

---

## 8. 개발자가 알아둬야 하는 내용

### 8.1 지금 상태는 “백엔드는 준비됐지만 전환은 덜 끝난 상태”에 가깝다

`ChatController`와 `RagController`를 보면 백엔드 엔드포인트는 꽤 잘 준비돼 있다.

하지만 프론트는 아직 옛 경로를 사용하므로, 팀이 “이미 마이그레이션 끝났다”고 생각하면 안 된다.

### 8.2 Direct Chat과 RAG Chat은 현재 품질 특성이 다르다

둘 다 Claude를 호출하지만:

- Direct Chat: category 중심, 이미지 지원, 프롬프트는 더 단순
- RAG Chat: retrieval/debug 강함, 프롬프트는 더 상세

같은 “Jazzify AI” 경험으로 보이려면 공통화가 필요하다.

### 8.3 문서 기준 endpoint와 실제 런타임 경로를 꼭 다시 확인해야 한다

현재 설정상 `context-path: /api`가 있으므로, 프론트/문서/프록시가 같은 기준을 바라보는지 꼭 맞춰야 한다.

### 8.4 Spring RAG는 Python보다 더 많은 인프라를 요구한다

Python은 단일 프로세스형에 가깝지만 Spring RAG는 사실상 아래 조합이다.

- Spring 앱
- PostgreSQL + pgvector
- 임베딩 서버
- Anthropic API

즉, 단순히 Java로 옮겼다고 끝이 아니라 **운영 의존성이 하나 더 늘어난 구조**다.

---

## 9. 스스로 결정하고 정리한 부분

이번 비교에서 명시 지시가 없어서 아래 기준은 내가 정해서 판단했다.

1. **비교 범위는 RAG+LLM 경로에 한정**했다.
   - auth, analysis, chordproject 전체를 다 비교하지 않고 chat/rag/llm 관련 파일만 집중적으로 봤다.

2. **문서의 제안 사항과 실제 구현을 함께 비교**했다.
   - 단순히 Python vs Spring이 아니라, `docs/LLM_Guide.md`, `docs/Rag_guide.md`에서 “의도한 목표 상태”와도 대조했다.

3. **프론트 코드도 일부 확인**했다.
   - user 요청은 Spring 비교였지만, 실제 마이그레이션 완료 여부는 프론트가 어디를 호출하는지 봐야만 정확히 판단 가능해서 `api/claude.ts`, `api/harmorag.ts`까지 확인했다.

---

## 10. 최종 판단

현재 상태를 단계로 표현하면:

- **Python RAG 로직의 Spring 이식도**: 높음
- **운영형 백엔드화 수준**: 높음
- **문서가 목표한 end-to-end 마이그레이션 완성도**: 중간

따라서 다음 한 문장으로 정리할 수 있다.

> **Spring 쪽 백엔드 구현은 이미 Python보다 더 제품형으로 발전했지만, 프론트 전환과 프롬프트 통합이 끝나지 않아 전체 시스템 관점에서는 아직 “과도기” 상태다.**

---

## 11. 바로 실행할 액션 아이템

- [ ] `api/claude.ts`를 Spring `/api/v1/chat/stream` 호출로 교체
- [ ] `VITE_ANTHROPIC_API_KEY` 제거
- [ ] `RagChatStreamer` / `ClaudeChatStreamer` 프롬프트 공통화
- [ ] `/v1/...` vs `/api/v1/...` 경로 기준 통일
- [ ] `RagEmbeddingClient` 외부 임베딩 서버 계약 확정
- [ ] direct/rag 인증 정책을 문서와 코드에 동시에 반영
- [ ] direct/rag 스트림 및 프롬프트 parity 테스트 추가

