# OMR profile 기반 dev/prod 분기 변경

## 작업한 내용

- OMR 서버 작업 제출 endpoint 선택 기준을 `omr.callback-url` 유무에서 Spring active profile로 변경했다.
- `prod` profile이 활성화되어 있으면 callback URL 설정 여부와 무관하게 prod endpoint로 요청한다.
- `prod` profile이 활성화되어 있지 않으면 callback URL 설정 여부와 무관하게 dev endpoint로 요청한다.
- callback URL은 endpoint 선택 조건에서 제외하고, 값이 있을 때만 OMR 서버에 전달하는 보조 multipart 필드로 유지했다.
- 기존 테스트 더블 호환성을 위해 `OmrClient(OmrProperties)` 생성자는 유지하고, Spring 주입용 생성자는 `Environment`를 받도록 분리했다.

## 변경 후 동작

| Spring active profile | 일반 악보 OMR endpoint | 코드 차트 OMR endpoint | `callback_url` 전달 |
| --- | --- | --- | --- |
| `prod` 포함 | `/omr/prod/process` | `/chords/chart/prod/process` | `omr.callback-url` 값이 있으면 전달 |
| `prod` 미포함 | `/omr/dev/process` | `/chords/chart/dev/process` | `omr.callback-url` 값이 있으면 전달 |

## 설계 의도

- 배포 환경 여부는 callback URL 존재 여부보다 Spring profile이 더 명확한 런타임 신호다.
- callback URL은 "어느 endpoint로 보낼지"를 결정하지 않고, OMR 서버가 처리 완료 후 어느 백엔드 URL로 콜백할지 알려주는 데이터로만 사용한다.
- 여러 profile이 동시에 활성화된 경우 `prod`가 하나라도 있으면 prod endpoint를 사용하도록 했다.

## 변경 파일

| 파일 | 변경 내용 |
| --- | --- |
| `src/main/java/com/jazzify/backend/shared/omr/OmrClient.java` | `Environment.getActiveProfiles()` 기준으로 prod/dev endpoint를 결정하도록 변경 |
| `src/main/java/com/jazzify/backend/shared/omr/OmrProperties.java` | callback URL 설명을 endpoint 분기 기준이 아닌 콜백 베이스 URL로 수정 |
| `src/main/resources/application-dev.yml` | dev 설정 주석을 profile 기반 분기 설명으로 수정 |
| `src/main/resources/application-prod.yml` | prod에서도 `OMR_CALLBACK_URL`을 읽을 수 있게 하고, endpoint는 profile 기준임을 명시 |
| `src/test/java/com/jazzify/backend/shared/omr/OmrClientTest.java` | prod profile + callback URL, dev profile + callback URL 없음 케이스 테스트 추가 |

## 임의로 결정한 부분

- active profile 목록에 `prod`가 포함되면 다른 profile이 함께 있어도 prod endpoint로 보내도록 했다.
- 테스트나 직접 생성 시 `Environment`가 없는 `OmrClient`는 prod profile이 없다고 보고 dev endpoint로 보내도록 했다.
- callback URL이 설정된 경우 prod profile에서도 `callback_url` multipart part를 계속 전달하도록 했다.

## 개발자가 알아둬야 할 내용

- `OMR_CALLBACK_URL`은 더 이상 dev/prod endpoint 선택 기준이 아니다.
- prod 배포에서 `SPRING_PROFILES_ACTIVE=prod`가 빠지면 callback URL이 있어도 dev endpoint로 요청한다.
- prod profile에서 `OMR_CALLBACK_URL`이 설정되어 있으면 prod endpoint로 보내면서 `callback_url`도 함께 전달한다.
- prod profile에서 `OMR_CALLBACK_URL`이 비어 있으면 prod endpoint로 보내지만 `callback_url`은 전달하지 않는다.

## 검증

다음 테스트를 실행해 통과를 확인했다.

```bash
./gradlew.bat test --tests com.jazzify.backend.shared.omr.OmrClientTest
```
