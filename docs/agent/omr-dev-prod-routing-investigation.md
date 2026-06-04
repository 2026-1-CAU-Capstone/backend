# OMR dev/prod 요청 분기 조사 기록

> 이 문서는 2026-06-04 이전 구현을 조사한 기록이다.
> 이후 OMR 제출 분기는 `callback-url` 유무가 아니라 Spring active profile 기준으로 변경되었다.
> 현재 동작은 `docs/agent/omr-profile-routing-change.md`를 기준으로 확인한다.

## 작업한 내용

- OMR 작업 제출 시 Spring Boot 백엔드가 MusicVision OMR 서버의 dev endpoint와 prod endpoint 중 어디로 요청하는지 조사했다.
- 조사 대상은 `OmrClient`, `OmrProperties`, OMR 관련 profile 설정, 도메인별 제출 호출부, 기존 통합 문서와 테스트다.
- 코드 변경은 하지 않았다.

## 결론

현재 백엔드 구현에서 dev/prod 제출 endpoint는 Spring profile 이름으로 직접 결정되지 않는다.

실제 분기 조건은 `omr.callback-url` 값의 존재 여부다.

| 조건 | 일반 악보 OMR 제출 endpoint | 코드 차트 OMR 제출 endpoint | `callback_url` multipart 포함 여부 |
| --- | --- | --- | --- |
| `omr.callback-url`이 null/blank가 아님 | `POST /omr/dev/process` | `POST /chords/chart/dev/process` | 포함 |
| `omr.callback-url`이 null 또는 blank | `POST /omr/prod/process` | `POST /chords/chart/prod/process` | 미포함 |

## 구현 근거

`OmrClient.submitJob(...)` 내부에서 다음 순서로 endpoint를 만든다.

1. `omrProperties.callbackUrl()`을 `callbackBaseUrl`로 읽는다.
2. `callbackBaseUrl != null && !callbackBaseUrl.isBlank()`이면 `isDevMode = true`가 된다.
3. endpoint는 `endpointPrefix + (isDevMode ? "/dev/process" : "/prod/process")`로 결정된다.
4. callback URL이 있으면 `callback_url` multipart part를 추가한다.

일반 악보 OMR은 endpoint prefix가 `/omr`이다.

- Solo, Lick, SheetProject는 `omrClient.submitJob(...)`을 호출한다.
- 따라서 dev는 `/omr/dev/process`, prod는 `/omr/prod/process`로 간다.

코드 프로젝트의 코드 차트 OMR은 endpoint prefix가 `/chords/chart`이다.

- ChordProject는 `omrClient.submitChordChartJob(...)`을 호출한다.
- 따라서 dev는 `/chords/chart/dev/process`, prod는 `/chords/chart/prod/process`로 간다.

## 설정 근거

`application-dev.yml`에서는 다음처럼 `OMR_CALLBACK_URL` 환경변수를 `omr.callback-url`에 연결한다.

```yaml
omr:
  server-url: ${OMR_SERVER_URL:}
  api-key: ${OMR_API_KEY:}
  callback-api-key: ${OMR_CALLBACK_API_KEY:}
  callback-url: ${OMR_CALLBACK_URL:}
```

따라서 dev profile이라도 `OMR_CALLBACK_URL`이 비어 있으면 실제 제출은 prod endpoint로 간다.

`application-prod.yml`에서는 `callback-url:`이 빈 값으로 고정되어 있다.

```yaml
omr:
  server-url: ${OMR_SERVER_URL:}
  api-key: ${OMR_API_KEY:}
  callback-api-key: ${OMR_CALLBACK_API_KEY:}
  callback-url:
```

따라서 prod profile에서는 기본적으로 prod endpoint로 간다.

또한 `docker-compose-server.yml`은 서버 컨테이너에 `SPRING_PROFILES_ACTIVE=prod`를 지정한다. 이 조합에서는 `application-prod.yml`이 적용되고 `omr.callback-url`이 비어 있으므로 OMR 제출은 prod endpoint로 간다.

## 요청 대상 서버와 dev/prod endpoint의 차이

`OMR_SERVER_URL`은 dev/prod endpoint를 고르는 값이 아니라 MusicVision 서버의 base URL이다.

예를 들어:

- `OMR_SERVER_URL=http://localhost:8001`
- `omr.callback-url=http://localhost:8080`

이면 최종 제출 URL은 `http://localhost:8001/omr/dev/process`가 된다.

반대로:

- `OMR_SERVER_URL=http://localhost:8001`
- `omr.callback-url=` blank

이면 최종 제출 URL은 `http://localhost:8001/omr/prod/process`가 된다.

## 도메인별 callback URL

`omr.callback-url`이 설정된 경우에만 `callback_url`을 OMR 서버에 보낸다.
이때 `OmrCallbackDomain`의 경로가 base URL 뒤에 붙는다.

| 도메인 | callback 경로 |
| --- | --- |
| Solo | `/api/v1/solos/omr/callback` |
| Lick | `/api/v1/licks/omr/callback` |
| ChordProject | `/api/v1/chord-projects/omr/callback` |
| SheetProject | `/api/v1/sheet-projects/omr/callback` |

예를 들어 `omr.callback-url=http://localhost:8080`이고 Lick OMR 제출이면 `callback_url`은 `http://localhost:8080/api/v1/licks/omr/callback`이 된다.

## 기존 문서와 현재 코드의 차이

`docs/spring_boot_backend.md`의 MusicVision 계약 문서는 prod endpoint가 `callback_url`을 요구한다고 설명한다.
하지만 현재 백엔드 구현은 `omr.callback-url`이 비어 있는 경우 prod endpoint로 보내면서 `callback_url`을 multipart에 넣지 않는다.

즉 현재 코드 기준으로는 다음 차이가 있다.

- 문서: `/omr/prod/process`, `/chords/chart/prod/process`는 `callback_url` 필수
- 코드: prod endpoint 사용 시 `callback_url` 미포함

이 부분은 MusicVision 서버의 실제 prod API가 문서대로 동작한다면 제출 실패 원인이 될 수 있다.

## 설계 의도 추정

코드 주석과 설정 주석을 종합하면 의도는 다음으로 보인다.

- 로컬/개발 테스트에서는 백엔드가 요청마다 callback URL을 명시한다.
- 이 경우 OMR 서버의 dev endpoint를 사용한다.
- 운영에서는 OMR 서버 측에 정적 callback URL을 등록해두고, 백엔드는 callback URL을 보내지 않는다.
- 이 경우 OMR 서버의 prod endpoint를 사용한다.

다만 이 의도는 `docs/spring_boot_backend.md`의 현재 계약 설명과 충돌한다.

## 임의로 판단한 부분

- "dev로 요청"과 "prod로 요청"은 OMR 서버 base URL이 아니라 MusicVision OMR 서버 path의 `/dev/process`, `/prod/process` 선택을 의미한다고 해석했다.
- 코드를 수정하지 않고 조사 문서만 작성했다.
- 실제 외부 MusicVision 서버에는 요청을 보내지 않았고, 로컬 코드/설정/테스트/문서만 근거로 판단했다.

## 개발자가 알아둬야 할 내용

- `SPRING_PROFILES_ACTIVE=dev`라고 해서 항상 OMR dev endpoint로 가지 않는다. `OMR_CALLBACK_URL`이 설정되어 있어야 dev endpoint로 간다.
- dev profile에서 `OMR_CALLBACK_URL`을 비워두면 prod endpoint로 요청한다.
- prod profile은 `OMR_CALLBACK_URL` 환경변수를 넣어도 현재 `application-prod.yml`에서 `callback-url:`이 빈 값으로 고정되어 있어 prod endpoint로 간다.
- `OMR_SERVER_URL`은 base URL이며, dev/prod endpoint 선택 조건이 아니다.
- OMR 서버 계약이 `prod endpoint도 callback_url 필수`라면 현재 백엔드 구현 또는 문서 중 하나를 맞춰야 한다.
