# Embedding API 통합 작업 문서

## 작업 내용

Jazzify Embedding Worker(외부 FastAPI 서버)를 Java 백엔드에서 HTTP 클라이언트로 호출하는 통합 작업.
기존에 구현된 스텁 코드(`EmbeddingClient`, `EmbeddingProperties`)가 API 명세와 일치하는지 검증하고,
Docker 배포 환경에서 서비스 간 URL이 자동 주입되도록 `docker-compose-server.yml`을 수정하였다.

---

## 수정 파일 목록

| 파일 | 변경 내용 |
|------|-----------|
| `docker-compose-server.yml` | `embedding-worker` 서비스 제거 (외부 서버에 이미 배포됨), `backend`의 `depends_on`에서도 제거, `EMBEDDING_SERVER_URL`은 `.env`에서 주입 |
| `src/main/resources/application-prod.yml` | 로깅 패키지명 플레이스홀더(`com.jazzify..package`) → `com.jazzify.backend` 수정, 임베딩 클라이언트 INFO 로그 추가 |

---

## 관련 클래스 역할 표

| 클래스 | 패키지 | 역할 |
|--------|--------|------|
| `EmbeddingClient` | `shared/embedding` | Embedding Worker의 `POST /embed`를 WebFlux WebClient로 호출하는 HTTP 클라이언트 |
| `EmbeddingProperties` | `shared/embedding` | `embedding.server-url`, `embedding.api-key` 설정값 바인딩 (`@ConfigurationProperties`) |
| `EmbeddingErrorCode` | `shared/exception/code` | 임베딩 서버 연동 에러 코드 (`EMB_001` 미설정, `EMB_002` 요청 실패) |
| `RagEmbeddingClient` | `domain/rag/service/implementation` | RAG 도메인 전용 래퍼 — `EmbeddingClient`에 위임, 직접 참조 캡슐화 |
| `RagEmbeddingModel` | `domain/rag/service/implementation` | Spring AI `EmbeddingModel` 구현체 — `EmbeddingClient` 결과를 Spring AI 형식으로 변환 |

---

## 클래스 간 논리 흐름도

```
[Spring Application]
        │
        ▼
EmbeddingProperties  ← application-{profile}.yml의 embedding.* 바인딩
        │
        ▼
  EmbeddingClient  (@Component, 항상 등록)
   ┌────────────────────────────────┐
   │  embed(text)                   │   POST /embed  →  Embedding Worker
   │  embedBatch(texts)             │   {"texts": [...]}
   │  isConfigured()                │   ← {"embeddings": [[...]], ...}
   └────────────────────────────────┘
        │
        ├──────────────────────────────────┐
        │  (rag.enabled=true 일 때만)        │
        ▼                                  ▼
RagEmbeddingClient               RagEmbeddingModel
  (도메인 래퍼)               (Spring AI EmbeddingModel 구현)
        │                                  │
        ▼                                  ▼
    RagReader                       pgvector VectorStore
  (벡터 유사도 검색)              (임베딩 저장·검색)
```

---

## 설계 의도

### EmbeddingClient — 항상 `texts` 배치 형식 사용

API 명세는 단일 입력 `{"text": "..."}` 과 배치 입력 `{"texts": [...]}` 를 모두 지원한다.
구현에서는 단일 호출도 `embedBatch(List.of(text))` 로 위임하여 **항상 배치 형식만 사용**한다.
이유:
- 서버가 두 형식 모두 지원하므로 클라이언트 코드 분기 불필요
- 향후 배치 확장 시 API 변경 없이 사용 가능

### docker-compose — embedding-worker 제거

임베딩 서버는 OMR 서버와 **동일한 외부 호스트**에 이미 배포되어 있으므로
`docker-compose-server.yml`에 서비스를 추가하지 않는다.
`EMBEDDING_SERVER_URL`은 OMR 서버와 마찬가지로 `.env` 파일에서 주입한다.

```
# .env 예시
OMR_SERVER_URL=http://<external-host>:8000
EMBEDDING_SERVER_URL=http://<external-host>:8001  # OMR과 동일 호스트, 포트만 다름
```

### RAG 비활성화 시에도 EmbeddingClient 등록

`EmbeddingClient`는 `@Component`로 항상 빈으로 등록된다.
`rag.enabled=false` 상태에서는 이 빈을 참조하는 `RagEmbeddingClient`, `RagEmbeddingModel`이
`@ConditionalOnProperty`에 의해 로드되지 않아 실제 임베딩 호출은 발생하지 않는다.
향후 RAG가 아닌 다른 도메인(예: 릭 유사도 검색)에서도 `EmbeddingClient`를 바로 주입받아 사용할 수 있다.

---

## 임의로 결정한 사항

| 결정 내용 | 이유 |
|-----------|------|
| `EMBEDDING_API_KEY`는 빈값 기본(`${EMBEDDING_API_KEY:}`) | EmbeddingAPIDocs에서 "내부망 호출이면 비워도 됨"이라고 명시 |
| prod 로깅 레벨을 `EmbeddingClient: INFO` | DEBUG는 운영에서 과도한 로그 발생, ERROR는 너무 조용함 — INFO가 적절 |
| `usage` 필드 파싱 생략 | `@JsonIgnoreProperties(ignoreUnknown = true)` 처리로 충분, 현재 사용처 없음 |

---

## 개발자가 알아야 할 내용

### 환경변수 설정 요약

| 환경 | 설정 방법 |
|------|-----------|
| **로컬 개발** | `.env` 파일에 `EMBEDDING_SERVER_URL=http://<external-host>:8001` 추가 |
| **서버 배포** | `.env` 파일에 `EMBEDDING_SERVER_URL=http://<external-host>:8001` 추가 (OMR과 동일 호스트, 포트만 다름) |
| **임베딩 서버 미사용** | `EMBEDDING_SERVER_URL` 미설정 시 `isConfigured()`가 `false` 반환, RAG 도메인도 비활성화이므로 오류 없음 |

### 포트 정리

임베딩 서버는 외부 서버(OMR과 동일 호스트)에서 운영된다.

| 접근 위치 | URL |
|-----------|-----|
| 백엔드 → 임베딩 서버 | `http://<external-host>:8001` (.env의 `EMBEDDING_SERVER_URL`) |
| OMR 서버 | `http://<external-host>:8000` (.env의 `OMR_SERVER_URL`) |

### RAG 활성화 방법

현재 `rag.enabled: false`. RAG를 활성화하려면:
1. PostgreSQL + pgvector 설정 (`RAG_DB_URL`, `RAG_DB_USERNAME`, `RAG_DB_PASSWORD`)
2. `rag.enabled: true` 로 변경
3. `EMBEDDING_SERVER_URL` 설정 확인
4. `RagBootstrapRunner`가 `data/explanation/` 데이터를 자동 청킹·임베딩·저장

### 임베딩 서버 헬스체크 엔드포인트

- `GET /health` — 경량 생존 확인 (모델 로드 안 함)
- `GET /ready` — 모델 로드 후 메타데이터 반환

docker-compose의 healthcheck는 `/health`를 사용하여 빠른 시작을 보장한다.




