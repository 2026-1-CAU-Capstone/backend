# Spring AI 시작 오류 조사 기록 (2026-05-22)

## 1. 작업한 내용

이번 작업에서는 아래 시작 오류를 조사했다.

- `Failed to generate bean name for imported class 'org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration'`
- 근본 예외: `Could not find class [org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration]`

조사 과정에서 확인한 내용은 다음과 같다.

1. `build.gradle`의 현재 의존성은 아래와 같다.
   - `org.springframework.boot` plugin: `4.0.3`
   - `org.springframework.ai:spring-ai-starter-model-anthropic:2.0.0-M6`
   - `org.springframework.ai:spring-ai-pgvector-store:2.0.0-M6`
2. `dependencyInsight` 결과, `spring-ai-starter-model-anthropic:2.0.0-M6`는 내부적으로 `spring-boot-starter:4.1.0-RC1` 라인을 전제로 하고 있었다.
3. `spring-boot-autoconfigure:4.0.3` JAR 안에는 실제로 `org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration` 클래스가 존재하지 않았다.
4. 다만 현재 워크스페이스 기준으로는 `BackendApplicationTests`가 통과하여, **소스/Gradle 설정 자체로는 현재 오류가 재현되지 않았다.**

즉, 이번 조사 시점의 결론은 다음과 같다.

- 사용자 로그의 오류는 **Spring AI / Spring Boot 자동설정 클래스 기대치 차이에서 발생한 것이 맞다.**
- 그러나 현재 워크스페이스의 실제 테스트 기준으로는 **이미 수정된 상태이거나, IDE/Gradle 동기화가 반영되지 않은 실행 클래스패스 문제일 가능성이 높다.**

---

## 2. 설계 의도

이번 작업의 의도는 불필요한 소스 변경 없이, **현재 워크스페이스에서 실제로 오류가 재현되는지 먼저 검증하고 원인을 문서화**하는 것이었다.

특히 다음 원칙을 유지했다.

- 이미 통과하는 테스트를 깨뜨릴 수 있는 과도한 의존성 업그레이드는 하지 않는다.
- 런타임 예외 메시지에 나온 클래스(`RestClientAutoConfiguration`)의 실제 존재 여부를 확인해 로그의 신뢰성을 검증한다.
- 단순 추측 대신 `dependencyInsight`와 테스트 결과를 함께 본다.

---

## 3. 제대로 지시되지 않아 임의로 결정하고 행동한 부분

명시 지시가 없어서 아래는 작업 중 판단하여 결정했다.

### 3.1 소스 코드는 수정하지 않았다

이유:

- 현재 `build.gradle`은 이미 `spring-ai-starter-model-anthropic:2.0.0-M6`와 `spring-ai-pgvector-store:2.0.0-M6`로 정렬되어 있었다.
- `BackendApplicationTests`가 실제로 통과했다.
- 즉시 Boot 버전을 `4.1.0-RC1`로 올리는 변경은 가능하지만, 그 경우 다른 라이브러리와의 호환성 리스크가 커질 수 있다.

그래서 이번에는 **원인 확인 + 현재 상태 검증 + 후속 조치 가이드 제공**까지를 우선했다.

### 3.2 기존 문서 결론을 그대로 신뢰하지 않고 실제 의존성을 다시 확인했다

기존 `docs/agent/spring-ai-anthropic-boot4-version-alignment.md`에는 `Boot 4.0.3 + Spring AI 2.0.0-M6` 조합이 정합하다고 정리되어 있었지만,
실제 `dependencyInsight` 결과상 `Spring AI 2.0.0-M6` 모듈은 내부적으로 `Boot 4.1.0-RC1` 계열 스타터를 참조하고 있었다.

따라서 문서보다 **실제 해석된 의존성 그래프를 우선**해 판단했다.

---

## 4. 개발자가 알아둬야 하는 내용

### 4.1 현재 오류는 코드 문제일 수도 있지만, IDE 실행 클래스패스 문제일 가능성도 높다

현재 워크스페이스에서는 컨텍스트 테스트가 통과했다.
즉 아래 가능성을 함께 봐야 한다.

- IntelliJ Gradle 프로젝트가 최신 `build.gradle` 변경을 아직 반영하지 못함
- 이전 의존성 캐시/런 설정이 남아 있음
- IDE에서 Gradle이 아닌 IntelliJ 자체 클래스패스로 실행 중임

권장 확인 순서:

1. Gradle 프로젝트 리로드
2. `clean` 후 재실행
3. IntelliJ의 Run Configuration이 **Gradle classpath**를 사용 중인지 확인
4. 필요 시 `.gradle` 캐시/IDE 캐시 정리 후 재동기화

### 4.2 `spring-ai-*` 2.0.0-M6는 내부적으로 Boot 4.1 계열을 기대한다

이번 조사에서 확인된 핵심 포인트:

- `spring-ai-starter-model-anthropic:2.0.0-M6`
- `spring-ai-pgvector-store:2.0.0-M6`

둘 다 transitive dependency로 `spring-boot-starter:4.1.0-RC1` 또는 `spring-boot-starter-jdbc:4.1.0-RC1`를 참조하고 있었다.

현재 프로젝트는 Boot plugin이 `4.0.3`이므로, 장기적으로는 아래 둘 중 하나로 정리하는 것이 더 안전하다.

1. **Spring Boot를 Spring AI가 기대하는 4.1 라인으로 올리기**
2. **Spring Boot 4.0.x와 공식 호환이 확인된 Spring AI 버전으로 내리기**

단, 이번 조사 시점에는 현재 테스트가 통과하고 있어 즉시 변경하지 않았다.

### 4.3 현재 소스에서 Spring AI 사용 지점

주요 사용 지점은 다음과 같다.

| 경로 | 역할 |
|---|---|
| `src/main/java/com/jazzify/backend/shared/llm/AnthropicStreamingClient.java` | `StreamingChatModel`을 받아 Claude 스트리밍 응답을 처리 |
| `src/main/java/com/jazzify/backend/domain/rag/config/RagVectorStoreConfig.java` | `PgVectorStore`를 수동 생성 |
| `src/main/java/com/jazzify/backend/domain/rag/repository/RagVectorStoreRepository.java` | `VectorStore`를 사용해 저장/검색 |
| `src/main/java/com/jazzify/backend/domain/rag/service/implementation/RagEmbeddingModel.java` | 커스텀 `EmbeddingModel` 구현 |

즉, Anthropic 쪽은 자동설정 영향이 크고, PgVector 쪽은 이미 일부를 수동 구성하고 있다.

---

## 5. 수정 파일

이번 작업에서 소스 코드는 변경하지 않았고, 아래 문서만 추가했다.

- `docs/agent/spring-ai-startup-error-investigation-2026-05-22.md`

---

## 6. 검증 내용

실행/확인한 검증:

1. `build.gradle` 확인
   - 현재 Boot는 `4.0.3`
   - Spring AI 관련 모듈은 `2.0.0-M6`
2. `dependencyInsight --dependency spring-ai-starter-model-anthropic --configuration runtimeClasspath`
   - Anthropic starter가 `spring-boot-starter:4.1.0-RC1`를 기대함을 확인
3. `dependencyInsight --dependency spring-boot-autoconfigure --configuration runtimeClasspath`
   - 실제 프로젝트는 `spring-boot-autoconfigure:4.0.3` 사용 중임을 확인
4. Gradle 캐시의 `spring-boot-autoconfigure-4.0.3.jar` 내부 클래스 확인
   - `RestClientAutoConfiguration` 부재 확인
5. `BackendApplicationTests` 실행
   - **통과 확인**

검증 결론:

- 사용자 로그의 원인 분석은 타당하다.
- 하지만 현재 워크스페이스에서는 같은 오류가 테스트 기준으로 재현되지 않는다.
- 따라서 지금 시점에서 가장 가능성이 높은 원인은 **IDE/Gradle 동기화 불일치 또는 이전 클래스패스 사용**이다.

---

## 7. 요약

이번 조사 결과는 다음과 같다.

- 오류의 직접 원인: `AnthropicChatAutoConfiguration`이 기대하는 Boot 자동설정 클래스와 현재 실행 클래스패스 불일치
- 현재 워크스페이스 상태: `BackendApplicationTests` 통과, 재현 불가
- 실무 판단: **우선 Gradle 리로드/클린 재빌드/실행 클래스패스 확인이 필요**
- 장기 대응: Boot와 Spring AI를 공식 호환 라인으로 더 명확하게 맞추는 후속 정리가 권장됨

