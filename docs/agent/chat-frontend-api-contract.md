# Jazzify Chat API 프론트엔드 계약 명세

- 기준일: 2026-06-08
- API base path: `/api/v1/chat`
- 인증: `Authorization: Bearer <accessToken>`
- JSON 요청 Content-Type: `application/json`

## 1. 핵심 계약

채팅 생성/이어가기는 화면 종류에 맞는 스트리밍 URL을 사용한다.

| 사용 화면 | Method | Endpoint | 저장 type | 응답 category | projectPublicId |
| --- | --- | --- | --- | --- | --- |
| Home/전역 채팅 | POST | `/api/v1/chat/global/stream` | `global` | `direct` | 사용하지 않음 |
| 코드 프로젝트 채팅 | POST | `/api/v1/chat/chord-project/stream` | `chordProject` | `chord` | 필수 |
| 악보 프로젝트 채팅 | POST | `/api/v1/chat/sheet-project/stream` | `sheetProject` | `sheet` | 필수 |
| 기존 통합 API | POST | `/api/v1/chat/stream` | `global` | `direct` | 사용하지 않음 |

중요:

- 요청 body에 채팅 출처용 `type` 또는 `category`를 보내지 않는다.
- 채팅 출처는 endpoint URL이 결정한다.
- `category`는 목록/상세 응답에서만 사용한다.
- 코드/악보 채팅에서는 해당 프로젝트의 publicId를 문자열 `projectPublicId`로 전달한다.
- `projectPublicId`는 UUID 객체가 아니라 JSON 문자열이다.

## 2. 스트리밍 요청

### 2.1 공통 요청 body

```json
{
  "message": "이 곡의 ii-V-I를 설명해줘",
  "history": [],
  "chordContext": null,
  "analysisCategory": null,
  "songTitle": null,
  "projectPublicId": null,
  "images": [],
  "chatPublicId": null,
  "useRag": false,
  "chordContextText": null,
  "suppressInlineChart": false
}
```

| 필드 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `message` | string | 예 | 현재 사용자의 질문. 공백 문자열 불가 |
| `history` | ChatMessageRequest[] | 아니오 | 신규 채팅에 seed할 이전 대화. 미전달/null은 빈 배열 처리 |
| `chordContext` | string/object/null | 아니오 | 현재 차트 컨텍스트. 일반 Claude 호출에서는 문자열/JSON 모두 전달 가능 |
| `analysisCategory` | string/null | 아니오 | 일반 Claude 호출의 코드 분석 프롬프트 포커스. `useRag=true`에서는 사용하지 않음 |
| `songTitle` | string/null | 아니오 | 곡명. 프로젝트 채팅 목록 표시와 제목 생성에 사용 |
| `projectPublicId` | string/null | 조건부 | 코드/악보 endpoint에서는 필수, 전역 endpoint에서는 저장하지 않음 |
| `images` | ChatImageRequest[] | 아니오 | 일반 Claude 호출 이미지 첨부. 미전달/null은 빈 배열 처리. `useRag=true`에서는 사용하지 않음 |
| `chatPublicId` | UUID string/null | 아니오 | 기존 채팅을 이어갈 때 Chat의 publicId 전달 |
| `useRag` | boolean | 아니오 | `true`이면 RAG 답변 생성 사용. 기본 JSON boolean 값은 `false` 권장 |
| `chordContextText` | string/null | 아니오 | `useRag=true`일 때 전달할 규칙 기반 화성 분석 텍스트 |
| `suppressInlineChart` | boolean | 아니오 | `useRag=true`일 때 현재 차트를 AI 답변에 다시 출력하지 않도록 요청 |

### 2.2 history 원소

```json
{
  "role": "user",
  "content": "이전 질문"
}
```

| 필드 | 허용 값 |
| --- | --- |
| `role` | `user`, `assistant` |
| `content` | 비어 있지 않은 문자열 |

이미 저장된 채팅을 이어갈 때는 `chatPublicId`를 전달하면 서버가 DB 메시지를 다시 읽는다. 이 경우 프론트가 보낸 `history`보다 저장된 메시지 이력이 기준이 된다.

### 2.3 이미지 원소

```json
{
  "mediaType": "image/png",
  "data": "iVBORw0KGgoAAA..."
}
```

- `data`는 `data:image/png;base64,` prefix를 제외한 raw Base64 문자열을 전달한다.
- `mediaType` 예: `image/png`, `image/jpeg`.
- 이미지가 있으면 `useRag=false`로 호출한다.

### 2.4 analysisCategory 허용 값

```text
overview
functional
iiVI
secondary
modal
improv
```

## 3. 화면별 요청 예시

### 3.1 전역 채팅 신규 생성

```http
POST /api/v1/chat/global/stream
Content-Type: application/json
Authorization: Bearer <token>
```

```json
{
  "message": "재즈에서 트라이톤 대리란 뭐야?",
  "history": [],
  "images": [],
  "chatPublicId": null,
  "useRag": false,
  "suppressInlineChart": false
}
```

`projectPublicId`를 보내더라도 전역 채팅에는 저장되지 않는다.

### 3.2 코드 프로젝트 채팅 신규 생성

```http
POST /api/v1/chat/chord-project/stream
```

```json
{
  "message": "이 곡의 화성 진행을 설명해줘",
  "history": [],
  "chordContext": "Song: Giant Steps\nKey: B...",
  "analysisCategory": "overview",
  "songTitle": "Giant Steps",
  "projectPublicId": "3f077f7e-85aa-4ec3-b552-3c37d6b096e4",
  "images": [],
  "chatPublicId": null,
  "useRag": false,
  "suppressInlineChart": true
}
```

### 3.3 악보 프로젝트 채팅 신규 생성

```http
POST /api/v1/chat/sheet-project/stream
```

```json
{
  "message": "이 악보의 코드와 멜로디 관계를 설명해줘",
  "history": [],
  "songTitle": "Autumn Leaves",
  "projectPublicId": "4aa6ddfa-40de-4710-a5b1-05043829440a",
  "images": [],
  "chatPublicId": null,
  "useRag": false,
  "suppressInlineChart": true
}
```

### 3.4 기존 채팅 이어가기

최초 스트림 응답 헤더 또는 목록/상세 API에서 받은 Chat publicId를 `chatPublicId`로 다시 보낸다.

```json
{
  "message": "그 부분을 더 자세히 설명해줘",
  "history": [],
  "songTitle": "Giant Steps",
  "projectPublicId": "3f077f7e-85aa-4ec3-b552-3c37d6b096e4",
  "images": [],
  "chatPublicId": "97109980-b391-45de-b35f-baf65f5839bf",
  "useRag": false,
  "suppressInlineChart": true
}
```

기존 채팅은 처음 생성된 것과 같은 종류의 endpoint로 이어가야 한다.

- `global` 채팅 -> `/global/stream`
- `chordProject` 채팅 -> `/chord-project/stream`
- `sheetProject` 채팅 -> `/sheet-project/stream`

다른 종류의 endpoint로 이어가면 `CHAT_002`가 발생한다.

## 4. 스트리밍 응답 처리

### 4.1 응답 형식

```http
HTTP/1.1 200 OK
Content-Type: text/plain
X-Chat-Public-Id: 97109980-b391-45de-b35f-baf65f5839bf
Cache-Control: no-cache, no-transform
X-Accel-Buffering: no
```

- body는 `text/plain` chunk stream이다.
- SSE가 아니므로 `data:` framing이나 `EventSource`를 사용하지 않는다.
- `fetch()` 후 `response.body.getReader()`로 읽는다.
- `X-Chat-Public-Id`는 신규/기존 채팅 모두 반환된다.
- 백엔드는 CORS `Access-Control-Expose-Headers`에 `X-Chat-Public-Id`를 포함한다.

### 4.2 프론트 처리 예시

```ts
const response = await fetch("/api/v1/chat/chord-project/stream", {
  method: "POST",
  headers: {
    "Content-Type": "application/json",
    Authorization: `Bearer ${accessToken}`,
  },
  body: JSON.stringify(request),
});

if (!response.ok) {
  throw await response.json();
}

const chatPublicId = response.headers.get("X-Chat-Public-Id");
const reader = response.body?.getReader();
const decoder = new TextDecoder();
let answer = "";

while (reader) {
  const { done, value } = await reader.read();
  if (done) break;
  answer += decoder.decode(value, { stream: true });
  renderAssistantMessage(answer);
}
```

스트림이 종료된 뒤 해당 turn의 `user` 메시지와 최종 `assistant` 텍스트가 DB에 저장된다. 대화 상세를 바로 다시 조회하려면 stream reader가 `done=true`가 된 이후 호출한다.

### 4.3 RAG 스트림 주의사항

`useRag=true`이면 응답 앞부분에 RAG 디버그 블록이 포함될 수 있다.

```text
\u0000RAG_DEBUG\u0000{...json...}\u0000END_DEBUG\u0000
```

- 디버그 블록은 화면에 AI 답변으로 출력하지 않는다.
- `RAG_DEBUG`와 `END_DEBUG` 사이 JSON이 모두 수신될 때까지 버퍼링한 뒤 제거한다.
- 이후 텍스트만 assistant 답변으로 누적한다.
- DB에는 디버그 블록을 제외한 최종 assistant 텍스트가 저장된다.

## 5. 채팅 목록 조회

```http
GET /api/v1/chat?page=0&size=20&sort=updatedAt,desc
```

- 기본 size: `20`
- 기본 정렬: `updatedAt DESC`

응답은 `ApiResponse<Spring Page<ChatSummary>>` 형식이다.

```json
{
  "data": {
    "content": [
      {
        "publicId": "97109980-b391-45de-b35f-baf65f5839bf",
        "type": "chordProject",
        "title": "Giant Steps",
        "category": "chord",
        "songTitle": "Giant Steps",
        "projectPublicId": "3f077f7e-85aa-4ec3-b552-3c37d6b096e4",
        "createdAt": "2026-06-08T16:30:00",
        "updatedAt": "2026-06-08T16:35:00"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "size": 20,
    "number": 0,
    "numberOfElements": 1,
    "first": true,
    "last": true,
    "empty": false
  }
}
```

Spring Data 버전에 따라 `pageable`, `sort` 같은 페이지 메타 필드가 추가로 포함될 수 있다. 프론트는 필요한 `content`, `totalElements`, `totalPages`, `number`, `size`를 기준으로 처리한다.

### 5.1 type 값

신규 채팅:

```text
global
chordProject
sheetProject
```

이전 데이터는 마이그레이션 전까지 다음 legacy 값이 보일 수 있다.

```text
direct
rag
```

프론트 이동 판단은 `type`보다 `category`를 기준으로 한다.

### 5.2 category와 이동 규칙

| category | projectPublicId | 표시/클릭 처리 |
| --- | --- | --- |
| `direct` | `null` | `title`로 표시하고 전역 채팅 화면에서 대화 복원 |
| `chord` | string | 코드 아이콘 + `songTitle ?? title`, 클릭 시 코드 프로젝트 route 생성 |
| `sheet` | string | 악보 아이콘 + `songTitle ?? title`, 클릭 시 악보 프로젝트 route 생성 |

프론트 route는 서버가 반환하지 않는다. 프론트 라우팅 정책에 따라 `category + projectPublicId`로 생성한다.

## 6. 채팅 상세 및 대화 복원

```http
GET /api/v1/chat/{chatPublicId}
```

```json
{
  "data": {
    "publicId": "97109980-b391-45de-b35f-baf65f5839bf",
    "type": "chordProject",
    "title": "Giant Steps",
    "category": "chord",
    "songTitle": "Giant Steps",
    "projectPublicId": "3f077f7e-85aa-4ec3-b552-3c37d6b096e4",
    "createdAt": "2026-06-08T16:30:00",
    "updatedAt": "2026-06-08T16:35:00",
    "messages": [
      {
        "publicId": "5fb48d82-28bb-4260-b2d9-6d3833dc5460",
        "role": "user",
        "content": "이 곡의 화성 진행을 설명해줘",
        "sortOrder": 0,
        "createdAt": "2026-06-08T16:30:01"
      },
      {
        "publicId": "1714747a-cb2f-4a54-a2fe-4dbb975edc48",
        "role": "assistant",
        "content": "이 곡은 장3도 관계의 조성 이동이 핵심입니다.",
        "sortOrder": 1,
        "createdAt": "2026-06-08T16:30:05"
      }
    ]
  }
}
```

- `messages`는 `sortOrder ASC`로 반환된다.
- 프론트는 응답 순서 그대로 렌더링할 수 있다.
- `role` 값은 `user`, `assistant`다.
- `createdAt`, `updatedAt`은 timezone offset이 없는 ISO LocalDateTime 문자열이다.

### 6.1 최근 채팅 클릭 흐름

```text
목록 항목 클릭
  -> GET /api/v1/chat/{chatPublicId}
  -> category 확인
     -> direct: 전역 채팅 화면
     -> chord: projectPublicId로 코드 프로젝트 화면
     -> sheet: projectPublicId로 악보 프로젝트 화면
  -> messages를 sortOrder 순서로 렌더링
  -> 이후 질문에는 같은 chatPublicId 사용
```

## 7. 채팅 삭제

```http
DELETE /api/v1/chat/{chatPublicId}
```

- 성공: `204 No Content`
- Chat과 해당 ChatMessage 전체가 삭제된다.
- 로그인한 사용자 소유 채팅만 삭제할 수 있다.

## 8. 오류 계약

오류 body:

```json
{
  "code": "CHAT_003",
  "message": "프로젝트 채팅에는 projectPublicId가 필요합니다."
}
```

`detail`은 값이 없으면 JSON에서 생략될 수 있다.

| HTTP | code | 발생 조건 |
| --- | --- | --- |
| 400 | `CHAT_002` | 기존 채팅을 다른 종류의 endpoint로 이어감 |
| 400 | `CHAT_003` | 코드/악보 endpoint에서 `projectPublicId` 누락 또는 공백 |
| 404 | `CHAT_001` | 채팅이 없거나 로그인 사용자 소유가 아님 |
| 503 | `RAG_001` | `useRag=true`이지만 서버에서 RAG가 비활성화됨 |
| 400 | `GLOBAL_002` | message/history/images validation 실패 |
| 401 | `GLOBAL_006` | access token 없음/만료 |

## 9. 프론트 구현 체크리스트

- [ ] Home은 `/global/stream` 사용
- [ ] 코드 프로젝트는 `/chord-project/stream` 사용
- [ ] 악보 프로젝트는 `/sheet-project/stream` 사용
- [ ] 요청 body에 출처용 `type`, `category`를 보내지 않음
- [ ] 코드/악보 요청에는 `projectPublicId` 문자열 포함
- [ ] 응답 헤더 `X-Chat-Public-Id` 저장
- [ ] 후속 요청에는 동일한 `chatPublicId`와 동일 종류 endpoint 사용
- [ ] stream 종료 후 상세 조회
- [ ] 목록 클릭 시 `category + projectPublicId`로 route 생성
- [ ] 상세 `messages`를 반환 순서대로 복원
- [ ] RAG 디버그 블록을 assistant 본문에서 제거
- [ ] 삭제 성공 `204`는 JSON body 없이 처리
