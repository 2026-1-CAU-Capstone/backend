# Embedding Probe & RAG CRUD API 구현 문서

## 작업 내용

임베딩 서버 연동을 직접 확인할 수 있는 **임베딩 프로브 API**를 신규 구현하고,
기존에 이미 구현되어 있던 **RAG 문서 CRUD API** 의 존재를 확인 및 문서화하였다.

---

## 신규 구현: Embedding 도메인

### 클래스 역할 표

| 클래스 | 패키지 | 역할 |
|--------|--------|------|
| `EmbeddingControllerSpec` | `domain/embedding/controller` | Swagger 문서용 인터페이스 — Operation 어노테이션만 보유 |
| `EmbeddingController` | `domain/embedding/controller` | `POST /v1/embedding/probe`, `GET /v1/embedding/health` 처리 |
| `EmbeddingProbeRequest` | `domain/embedding/dto/request` | 임베딩 대상 텍스트 목록 (1~64개, 유효성 검사 포함) |
| `EmbeddingProbeResponse` | `domain/embedding/dto/response` | 텍스트-벡터 쌍 목록 + 차원(dimension) + 개수(count) |
| `EmbeddingHealthResponse` | `domain/embedding/dto/response` | 서버 설정 여부·URL·연결 가능 여부 |
| `EmbeddingService` | `domain/embedding/service` | `EmbeddingClient`에 위임, 응답 DTO 조립 |

### 수정된 공유 클래스

| 클래스 | 변경 내용 |
|--------|-----------|
| `EmbeddingClient` (shared) | `checkHealth()` 메서드 추가 — `GET /health` 호출, 3초 타임아웃 |
| `EmbeddingClient.EmbeddingHealthStatus` | `checkHealth()` 반환용 inner record 추가 |
| `SecurityConfig` | `GET /v1/embedding/health` → permitAll, `POST /v1/embedding/**` → ADMIN/MANAGE |

---

## API 엔드포인트

### `POST /v1/embedding/probe`

- **권한**: ADMIN 또는 MANAGE 역할 필요
- **요청 본문**:
```json
{
  "texts": ["ii-V-I in C major", "blues scale over Bb7"]
}
```
- **응답**:
```json
{
  "data": {
    "dimension": 768,
    "count": 2,
    "results": [
      {
        "text": "ii-V-I in C major",
        "vector": [0.0123, -0.0456, ...]
      },
      {
        "text": "blues scale over Bb7",
        "vector": [0.0789, 0.0012, ...]
      }
    ]
  }
}
```

### `GET /v1/embedding/health`

- **권한**: 인증 불필요 (public)
- **응답**:
```json
{
  "data": {
    "configured": true,
    "serverUrl": "http://external-host:8001",
    "reachable": true
  }
}
```

---

## 클래스 간 논리 흐름도

```
[HTTP 요청]
    │
    ▼
EmbeddingController
    │
    ├── POST /probe ──→ EmbeddingService.probe()
    │                       │
    │                       ▼
    │                  EmbeddingClient.embedBatch()
    │                       │
    │                       ▼
    │                  Embedding Worker (POST /embed)
    │                       │
    │                       ▼
    │                  EmbeddingProbeResponse 조립
    │
    └── GET /health ──→ EmbeddingService.health()
                            │
                            ▼
                       EmbeddingClient.checkHealth()
                            │
                            ▼
                       Embedding Worker (GET /health)
                            │
                            ▼
                       EmbeddingHealthResponse 조립
```

---

## 기존 구현 확인: RAG 문서 CRUD

`RagController` (`/v1/rag/documents`)에 이미 완전히 구현되어 있으며,
`rag.enabled=true` 조건 하에서 활성화된다.

### RAG CRUD 엔드포인트 요약

| 메서드 | 경로 | 설명 |
|--------|------|------|
| `GET` | `/v1/rag/documents` | 문서 목록 페이지 조회 (`?sourceType=`, `?q=` 필터 지원) |
| `GET` | `/v1/rag/documents/{publicId}` | 단건 조회 |
| `POST` | `/v1/rag/documents` | 문서 생성 + 청크 분할 + 임베딩 자동 색인 |
| `PUT` | `/v1/rag/documents/{publicId}` | 문서 수정 + 임베딩 재색인 |
| `DELETE` | `/v1/rag/documents/{publicId}` | 문서 + 벡터 청크 함께 삭제 |

### RAG CRUD 활성화 조건

```yaml
# application-dev.yml 또는 application-prod.yml
rag:
  enabled: true          # false → RagController 비활성화
  datasource:
    url: ${RAG_DB_URL}   # PostgreSQL + pgvector
    ...
```

---

## 설계 의도

### 임베딩 프로브를 별도 도메인으로 분리한 이유

- `rag.enabled` 여부와 **무관하게** 임베딩 서버 연동을 확인해야 하는 경우가 있음
- RAG 도메인은 pgvector 인프라에 의존하지만, 임베딩 서버 테스트는 단순 HTTP 호출이므로 분리가 맞음
- 향후 다른 도메인(예: 릭 유사도 검색)에서도 임베딩 서버를 사용할 때 동일한 헬스체크 엔드포인트를 재사용할 수 있음

### `GET /health` 타임아웃 3초

임베딩 서버 모델이 로드되어 있지 않으면 `/health`가 느릴 수 있음.
`/health` 문서상 "Does not load the model"이므로 3초면 충분하다고 판단.
모델 로드 상태까지 확인하려면 `/ready` 엔드포인트를 사용해야 하지만,
헬스체크 용도로는 서버 프로세스 생존 여부만 확인하면 충분하다.

---

## 개발자가 알아야 할 내용

### 임베딩 프로브 테스트 방법

```bash
# 헬스 체크 (인증 불필요)
curl http://localhost:8080/api/v1/embedding/health

# 임베딩 프로브 (ADMIN/MANAGE 토큰 필요)
curl -X POST http://localhost:8080/api/v1/embedding/probe \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"texts": ["ii-V-I in C major", "bebop scale"]}'
```

### RAG CRUD 사용 전 체크리스트

1. `.env`에 `EMBEDDING_SERVER_URL` 설정
2. `rag.enabled: true` 설정
3. pgvector (`RAG_DB_URL`, `RAG_DB_USERNAME`, `RAG_DB_PASSWORD`) 설정
4. `rag.bootstrap.enabled: true` → 앱 시작 시 `data/explanation/` 자동 색인

