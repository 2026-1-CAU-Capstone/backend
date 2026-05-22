# Spring AI Anthropic / Spring Boot 4 버전 정합성 수정

## 1. 작업한 내용

이번 작업에서는 애플리케이션 시작 시 발생하던 아래 오류를 해결했다.

- `java.lang.IllegalStateException: Failed to generate bean name for imported class 'org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration'`
- 근본 원인: `org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration` 클래스를 찾지 못함

실제 수정 내용은 다음과 같다.

1. `build.gradle`에 `springAiVersion = '2.0.0-M6'`를 추가했다.
2. `org.springframework.ai:spring-ai-starter-model-anthropic` 버전을 `1.1.6`에서 `2.0.0-M6`로 올렸다.
3. 이미 사용 중이던 `org.springframework.ai:spring-ai-pgvector-store:2.0.0-M6`와 같은 버전 라인으로 통일했다.

즉, **Spring AI Anthropic 스타터와 PgVector 모듈이 서로 다른 메이저/마이너 라인을 사용하던 상태를 하나의 릴리스 라인으로 정리**했다.

---

## 2. 설계 의도

이번 수정의 핵심 의도는 **자동설정 충돌을 근본 원인인 버전 불일치에서 해결**하는 것이다.

에러 분석 결과:

- 프로젝트는 `Spring Boot 4.0.3`을 사용 중이었다.
- 그런데 `spring-ai-starter-model-anthropic:1.1.6`은 런타임에서 `Spring Boot 3.5.14` 계열 자동설정을 전제로 동작하고 있었다.
- 동시에 `spring-ai-pgvector-store`는 이미 `2.0.0-M6`를 쓰고 있었다.

즉, 현재 빌드는 다음처럼 섞여 있었다.

- Spring Boot: `4.0.3`
- Spring AI Anthropic Starter: `1.1.6`
- Spring AI PgVector Store: `2.0.0-M6`

이 조합은 같은 Spring AI 생태계 내부에서도 기대하는 Boot 자동설정 클래스가 달라질 수 있어, 컨텍스트 로딩 시점에 실패할 가능성이 높다.

그래서 이번 수정은 기능 변경이 아니라:

- **Anthropic / PgVector 두 Spring AI 모듈을 동일한 버전 라인으로 맞추고**
- 기존 애플리케이션 코드(`StreamingChatModel`, `EmbeddingModel`, `VectorStore`)는 그대로 유지하는
- **최소 침습적 정합성 수정**

방식으로 진행했다.

---

## 3. 내가 임의로 결정한 부분

명시 지시가 없어서 아래는 작업 중 판단하여 결정했다.

### 3.1 Spring Boot를 내리지 않고 Spring AI 쪽을 맞춤

선택지로는 크게 두 가지가 있었다.

1. Spring Boot를 3.x로 내리기
2. Spring AI 모듈 버전을 Boot 4 프로젝트에 맞게 정리하기

이번에는 **2번을 선택**했다.

이유:

- 현재 프로젝트는 이미 `spring-boot-starter-webmvc`, `springboot4-dotenv` 등 Boot 4 전제를 갖고 있었다.
- Boot를 내리면 웹 스타터, 테스트 스타터, 기타 자동설정 전반을 다시 맞춰야 해서 변경 범위가 커진다.
- 반면 현재 드러난 직접 원인은 `spring-ai-starter-model-anthropic:1.1.6`의 자동설정 참조 불일치였다.

### 3.2 Spring AI BOM 도입 대신 직접 버전 통일만 적용

BOM 도입도 가능했지만, 이번 작업은 **최소 변경**을 우선했다.

결정:

- `ext.springAiVersion` 변수만 추가
- 두 개의 직접 의존성을 같은 버전으로 맞춤

이렇게 하면 현재 문제를 해결하면서도 변경량을 작게 유지할 수 있다.

---

## 4. 개발자가 알아둬야 하는 내용

### 4.1 이번 문제의 본질은 자동설정 클래스 불일치였다

에러 메시지상 `bean name` 생성 실패처럼 보여도, 실제 핵심은 아래다.

- Anthropic auto-configuration이 참조하는 Boot 자동설정 클래스와
- 현재 프로젝트가 사용하는 Boot 버전의 실제 클래스 구성이
- 서로 맞지 않았다.

따라서 비슷한 문제가 다시 생기면 먼저 아래를 확인해야 한다.

- `Spring Boot` 버전
- `spring-ai-*` 모듈 버전이 서로 같은 라인인지
- starter / autoconfigure / core 모듈이 섞여 있지 않은지

### 4.2 Spring AI 모듈은 가급적 같은 버전 라인으로 유지해야 한다

이번처럼 아래 조합은 위험하다.

- 한쪽은 `1.1.x`
- 다른 쪽은 `2.0.0-Mx`

권장:

- `spring-ai-starter-model-anthropic`
- `spring-ai-pgvector-store`
- 추후 추가될 다른 `spring-ai-*` 모듈들

을 **같은 버전 변수**로 관리하는 방식 유지

### 4.3 추후 업그레이드 시에는 Boot / Spring AI 매트릭스를 같이 봐야 한다

현재는 `Spring Boot 4.0.3` + `Spring AI 2.0.0-M6` 조합으로 컴파일 및 핵심 테스트가 통과했다.
다만 Spring AI milestone 버전은 내부적으로 더 높은 Boot 계열을 기준으로 빌드되었을 가능성이 있으므로,
향후 업그레이드 시에는 다음을 같이 확인하는 것이 안전하다.

- Spring AI 릴리스 노트
- Spring Boot 호환 매트릭스
- startup smoke test
- `BackendApplicationTests`

---

## 5. 수정 파일

- `build.gradle`
- `docs/agent/spring-ai-anthropic-boot4-version-alignment.md` (신규)

---

## 6. 검증 내용

실행한 검증:

1. Gradle 의존성 분석
   - `spring-ai-starter-model-anthropic:1.1.6`가 `spring-boot-starter:3.5.14`를 기대하고 있음을 확인
   - 기존 `spring-ai-pgvector-store:2.0.0-M6`와 버전 라인이 어긋나 있음을 확인
2. `get_errors(build.gradle)` 확인
   - 오류 없음
3. Gradle 빌드/테스트 실행
   - `clean`
   - `compileJava`
   - `test --tests "com.jazzify.backend.domain.chat.service.implementation.ClaudeChatStreamerTest"`
   - `test --tests "com.jazzify.backend.domain.rag.service.RagServiceTest"`
   - `test --tests "com.jazzify.backend.BackendApplicationTests"`

최종 결과:

- **BUILD SUCCESSFUL**
- 애플리케이션 컨텍스트 로딩을 포함한 핵심 테스트 통과

---

## 7. 요약

이번 수정은 기능 추가가 아니라 **Spring Boot 4 환경에서 Spring AI Anthropic 자동설정이 깨지던 버전 불일치 문제를 정리한 작업**이다.

핵심 요약:

- 원인: `spring-ai-starter-model-anthropic:1.1.6` + `Boot 4.0.3` 불일치
- 조치: Anthropic / PgVector Spring AI 모듈을 `2.0.0-M6`로 통일
- 결과: 컴파일 및 핵심 테스트 통과, 컨텍스트 로딩 정상 확인


