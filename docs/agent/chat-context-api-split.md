# 상황별 Chat API 분리 작업 문서

## 작업한 내용

- 채팅 스트리밍 API를 상황별로 분리했다.
  - `POST /v1/chat/global/stream`
  - `POST /v1/chat/chord-project/stream`
  - `POST /v1/chat/sheet-project/stream`
- 기존 `POST /v1/chat/stream`은 호환용으로 유지했다.
- 모든 API는 동일한 `Chat` 엔티티를 사용하며, 신규 채팅은 `Chat.type`으로 세션 출처를 구분한다.
  - `global`
  - `chordProject`
  - `sheetProject`
- 프론트 목록/상세 복원에 필요한 출처 메타데이터를 저장하고 반환하도록 확장했다.
  - `category`: `direct | chord | sheet`
  - `songTitle`
  - `projectPublicId`
- `GET /v1/chat`, `GET /v1/chat/{publicId}` 응답에 문자열 `projectPublicId`를 추가했다.
- 코드/악보 스트림 요청에 문자열 `projectPublicId`를 받도록 했다.
- 채팅 출처 `category`는 요청에서 받지 않고 호출 URL로만 결정한다.
  - `/global/stream`, 기존 `/stream` -> `direct`
  - `/chord-project/stream` -> `chord`
  - `/sheet-project/stream` -> `sheet`
- 분석 프롬프트 포커스가 필요한 경우에는 별도 요청 필드 `analysisCategory`를 사용한다.
- 스트리밍 완료 후 `user` 메시지와 최종 `assistant` 응답을 저장하는 기존 `persistTurn` 흐름은 유지했다.
- Swagger 문서용 `ChatControllerSpec`에 신규 API 3개와 호환용 API 설명을 추가했다.

## 설계 의도

- `type`은 API 컨텍스트를 나타내도록 확장했다.
  - 기존 `DIRECT/RAG`는 이전 데이터 호환을 위해 enum에 남겨뒀다.
  - 신규 생성 row는 더 이상 `DIRECT/RAG`를 저장하지 않고 `GLOBAL/CHORD_PROJECT/SHEET_PROJECT`를 저장한다.
- `RAG` 여부는 채팅 세션의 출처가 아니라 답변 생성 방식이므로, 새 `type`에는 반영하지 않았다.
- 프론트가 localStorage 없이 최근 채팅 목록을 복원할 수 있도록 `sourceCategory`, `songTitle`, `projectPublicId`를 서버 메타데이터로 저장한다.
- 기존 DB의 `category` 컬럼은 분석 포커스(`ChatAnalysisCategory`) 용도로 유지하고, 출처 표시는 `source_category` 컬럼으로 분리했다.
- 기존 통합 API로 만들어진 `DIRECT/RAG` 채팅은 새 endpoint로 이어서 요청되면 해당 새 `type`으로 갱신되도록 했다.
- 채팅 출처는 클라이언트 요청값을 신뢰하지 않고 endpoint가 고정한다. 동일한 요청 body라도 호출 URL에 따라 저장되는 `type/category`가 결정된다.
- 연결 프로젝트의 종류를 나타내는 별도 `linkedProjectType`은 만들지 않았다. 응답의 `category=chord|sheet`가 프로젝트 종류를 나타내고 `projectPublicId`가 대상을 식별한다.

## 임의로 결정한 부분

- 분리 API 경로는 채팅 도메인 하위로 배치했다.
  - `/v1/chat/global/stream`
  - `/v1/chat/chord-project/stream`
  - `/v1/chat/sheet-project/stream`
- 프로젝트 publicId는 UUID 타입으로 파싱하거나 JPA 연관관계로 묶지 않고 문자열 컬럼에 저장한다.
- 차트 채팅의 `title`은 `songTitle`이 있으면 곡명으로 잡고, 없으면 기존처럼 사용자 질문 기반 제목을 사용한다.
- 호환용 `POST /v1/chat/stream`은 요청의 컨텍스트와 관계없이 항상 `type=global`, `category=direct`로 저장한다.
- 코드/악보 채팅은 복원 가능한 연결 정보를 보장하기 위해 `projectPublicId`가 비어 있으면 `CHAT_003` 오류를 반환한다.

## 생성/변경 클래스 역할

| 클래스 | 구분 | 역할 |
| --- | --- | --- |
| `ChatSourceCategory` | 신규 enum | 응답 `category`와 DB `source_category`에 저장되는 채팅 출처 값(`direct/chord/sheet`)을 표현한다. |
| `ChatType` | 변경 enum | 기존 `direct/rag`에 더해 신규 세션 타입(`global/chordProject/sheetProject`)을 표현한다. |
| `Chat` | 변경 Entity | `sourceCategory`, 문자열 `projectPublicId`를 저장하고, 기존 채팅 메타데이터 갱신 시 새 `type`도 함께 반영한다. |
| `ChatStreamRequest` | 변경 DTO | 출처 `category`는 받지 않으며, 프롬프트용 `analysisCategory`와 연결 대상 `projectPublicId`를 받는다. |
| `ChatSummaryResponse` | 변경 DTO | 목록 응답에 `category`, `songTitle`, `projectPublicId`를 포함한다. |
| `ChatDetailResponse` | 변경 DTO | 상세 응답에 `category`, `songTitle`, `projectPublicId`, `messages`를 포함한다. |
| `ChatController` | 변경 Controller | 세 분리 스트림 API와 호환용 통합 API를 같은 저장/스트림 로직으로 라우팅한다. |
| `ChatService` | 변경 Service | endpoint별 `type/category`를 받아 채팅 생성/갱신과 메시지 영속화를 오케스트레이션한다. |
| `ChatWriter` | 변경 Component | 신규 메타데이터를 포함해 `Chat` 생성/갱신을 수행한다. |
| `ChatMapper` | 변경 Mapper | `Chat` 엔티티를 목록/상세 응답으로 변환하며, 과거 row의 출처 fallback을 처리한다. |

## 논리 흐름도

```text
Frontend
  ├─ POST /v1/chat/global/stream
  │    └─ ChatController: type=global, category=direct
  ├─ POST /v1/chat/chord-project/stream
  │    └─ ChatController: type=chordProject, category=chord
  └─ POST /v1/chat/sheet-project/stream
       └─ ChatController: type=sheetProject, category=sheet

ChatController
  ├─ URL에 따라 type/category 고정
  │    ├─ /stream, /global/stream -> global/direct
  │    ├─ /chord-project/stream   -> chordProject/chord
  │    └─ /sheet-project/stream   -> sheetProject/sheet
  ├─ useRag=false -> ChatService.prepareDirectStream(...)
  └─ useRag=true  -> ChatService.prepareRagStream(...) -> RagService.streamChat(...)

ChatService.prepare*
  ├─ chatPublicId 없음 -> ChatWriter.create(...)
  │    └─ Chat(type, sourceCategory, songTitle, projectPublicId) 저장
  └─ chatPublicId 있음 -> ChatWriter.updateMetadata(...)
       └─ 기존 Chat의 type/sourceCategory/songTitle/projectPublicId 갱신

Streaming 완료
  └─ ChatService.persistTurn(...)
       ├─ ChatMessage(role=user) 저장
       └─ ChatMessage(role=assistant) 저장

복원
  ├─ GET /v1/chat -> ChatSummaryResponse(category, songTitle, projectPublicId)
  └─ GET /v1/chat/{publicId} -> ChatDetailResponse + messages[]
```

## 개발자가 알아둬야 할 내용

- DB 자동 업데이트 환경에서는 `tb_chat`에 `source_category`, `project_public_id` 컬럼이 추가된다.
- 기존 `tb_chat.category` 컬럼은 분석 포커스 용도로 유지된다. 프론트 표시용 `category`는 응답에서 `source_category`를 매핑한 값이다.
- 새 요청에서 분석 포커스를 명확히 보내려면 `analysisCategory`를 사용해야 한다.
- 메시지 복원은 `ChatMessage.sortOrder` 기준 시간순 반환 흐름을 유지한다.
- 이전 구현에서 생성된 `chart_route` 컬럼은 더 이상 엔티티에 매핑되지 않는다. 운영 DB에서 불필요한 컬럼을 제거하려면 별도 스키마 마이그레이션으로 삭제해야 한다.
- 브라우저가 스트림 응답의 `X-Chat-Public-Id`를 읽을 수 있도록 CORS exposed header에 해당 헤더를 추가했다.

## projectPublicId 의미

`projectPublicId`는 채팅이 시작된 코드 프로젝트 또는 악보 프로젝트의 외부 식별자다.

- 요청과 DB에는 UUID 객체가 아닌 문자열로 저장한다.
- 코드 프로젝트 endpoint에서는 코드 프로젝트 publicId를, 악보 프로젝트 endpoint에서는 악보 프로젝트 publicId를 전달한다.
- 백엔드는 앞뒤 공백만 제거하고 UUID 형식으로 파싱하지 않는다.
- 전역 채팅은 연결 프로젝트가 없으므로 `projectPublicId=null`로 저장한다.
- 코드/악보 endpoint는 `projectPublicId`가 필수다.
- 프론트는 응답의 `category`로 프로젝트 종류를 판단하고 `projectPublicId`로 이동 경로를 만든다.

```text
category=chord + projectPublicId=abc
  -> 프론트 코드 프로젝트 route 생성

category=sheet + projectPublicId=def
  -> 프론트 악보 프로젝트 route 생성

category=direct + projectPublicId=null
  -> 전역 채팅 화면에서 대화만 복원
```

- `projectPublicId` 문자열 자체는 접근 권한을 보장하지 않는다. 실제 프로젝트 조회 API에서 사용자 소유권을 다시 검증해야 한다.
- 프로젝트가 삭제된 경우 채팅 이력은 남아 있어도 해당 프로젝트 화면 이동은 실패할 수 있다.

## 검증

- 실행한 테스트:
  - `.\gradlew.bat test --tests "com.jazzify.backend.domain.chat.*" --tests "com.jazzify.backend.domain.rag.service.implementation.RagChatStreamerTest"`
  - `.\gradlew.bat test`
- 결과:
  - `BUILD SUCCESSFUL`
