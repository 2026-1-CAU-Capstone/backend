# OMR Swagger description 업데이트

## 작업 내용

OMR 관련 API의 Swagger `@Operation.description`과 OMR 요청 DTO JavaDoc을 현재 구현 기준으로 수정했다.

수정 대상:

- `ChordProjectControllerSpec`
- `SheetProjectControllerSpec`
- `LickControllerSpec`
- `SoloControllerSpec`
- `ChordProjectOmrCallbackControllerSpec`
- `SheetProjectOmrCallbackControllerSpec`
- `LickOmrCallbackControllerSpec`
- `SoloOmrCallbackControllerSpec`
- `LickOmrRequest`
- `SoloOmrRequest`

## 반영한 현재 동작

| 도메인 | 생성 API 설명 반영 내용 |
| --- | --- |
| ChordProject | `sourceType`에 따라 `/chords/chart/{dev|prod}/process` 또는 `/chords/sheet-music/{dev|prod}/process`로 제출 |
| SheetProject | 일반 악보 OMR `/omr/{dev|prod}/process` 제출 및 callback 완료 후 MusicXML/chord assignments 처리 |
| Lick | 비동기 PENDING 생성, `/omr/{dev|prod}/process` 제출, callback 후 sheetData/features 채움 |
| Solo | Lick과 같은 비동기 흐름, title 입력 시 중복 검사 안내 |

## 주요 수정 포인트

- OMR 생성 API가 최종 결과를 즉시 반환하는 것처럼 보이던 설명을 비동기 callback/polling 흐름으로 수정했다.
- “이벤트 리스너에서 비동기 수행”처럼 현재 구현과 맞지 않는 표현을 callback 기반 처리로 수정했다.
- ChordProject OMR의 `sourceType` 필드와 MusicVision endpoint 분기를 Swagger에 반영했다.
- ChordProject/SheetProject status API의 `progress`가 진행 중에는 MusicVision `GET /omr/jobs/{jobId}` 값을 우선 사용한다는 점을 명시했다.
- Lick/Solo OMR의 `source` 미입력 기본값을 실제 enum 파서 기준인 `unknown`으로 수정했다.
- Lick/Solo OMR이 전용 status API 없이 단건 조회 응답의 `omrStatus`, `omrProgress`, `omrFailureReason`를 사용해야 한다는 점을 명시했다.
- 내부 callback API는 프론트엔드 직접 호출 대상이 아니며, JWT 없이 `X-OMR-Callback-API-Key`로 검증한다는 점을 도메인별로 정리했다.

## 설계 의도

코드 동작은 바꾸지 않고 Swagger와 DTO 설명만 현재 구현에 맞췄다. 프론트엔드 개발자가 Swagger만 보고도 다음을 오해하지 않도록 하는 것이 목적이다.

- OMR 생성 응답은 완료 결과가 아니라 처리 시작 상태다.
- 완료 데이터 반영은 MusicVision callback 이후다.
- ChordProject는 입력 이미지 유형에 따라 서로 다른 MusicVision endpoint를 사용한다.
- Lick/Solo는 `GET /{publicId}`로 상태를 확인한다.

## 임의로 결정한 부분

- 설명에는 public API 기준 경로를 `/v1/...`로 표기했다. 배포 환경에서 `server.servlet.context-path=/api`가 붙으면 실제 외부 호출은 `/api/v1/...`가 된다.
- Swagger 설명만 갱신했고, 요청/응답 DTO 스키마나 컨트롤러 매핑은 변경하지 않았다.
- Lick/Solo의 오래된 일반 CRUD 설명에 남아 있는 `sourceUrl` 표현은 OMR API 범위 밖이라 이번 수정에서 건드리지 않았다.

## 개발자가 알아둬야 할 내용

- ChordProject OMR에서 `sourceType` 미입력은 기존 호환을 위해 `chart`로 설명했다.
- Lick/Solo OMR은 진행률을 MusicVision status endpoint에서 동적으로 가져오는 전용 status API가 없다. 단건 조회는 DB에 저장된 OMR progress를 반환한다.
- callback API 설명은 내부용 명세다. Swagger에 노출되더라도 프론트엔드 호출 대상이 아니다.

## 검증

다음 명령으로 Java 컴파일을 확인했다.

```text
./gradlew.bat compileJava
```

결과: 성공.
