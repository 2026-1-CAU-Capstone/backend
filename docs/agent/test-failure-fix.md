# 테스트 실패 수정

## 작업 내용

두 가지 테스트 실패를 수정하였다.

1. `RagVectorStoreRepositoryTest` – 컴파일 오류 수정
2. `BackendApplicationTests` – 컨텍스트 로드 실패 수정

---

## 1. RagVectorStoreRepositoryTest 컴파일 오류

### 원인

`RagProperties`가 리팩토링되면서 내부에 존재하던 `Embedding` 중첩 레코드가 제거되었고,
생성자 시그니처도 6개 파라미터에서 5개 파라미터로 변경되었다.

**이전 (삭제된) 시그니처:**
```java
new RagProperties(
    boolean enabled,
    @Nullable ?,          // null
    @Nullable ?,          // null
    new RagProperties.Embedding(...),   // ← 제거된 inner record
    new RagProperties.Retrieval(...),
    new RagProperties.VectorStore(...)
)
```

**현재 시그니처:**
```java
new RagProperties(
    boolean enabled,
    @Nullable Datasource datasource,
    @Nullable Bootstrap bootstrap,
    @Nullable Retrieval retrieval,
    @Nullable VectorStore vectorStore
)
```

### 수정 내용

`RagVectorStoreRepositoryTest#setUp()`에서 `new RagProperties.Embedding(...)` 인수를 제거하고
현재 생성자 시그니처에 맞게 5개 파라미터로 수정하였다.

```java
// 수정 후
new RagProperties(
    true,
    null,
    null,
    new RagProperties.Retrieval(5, 3, 60),
    new RagProperties.VectorStore("public", "rag_chunk_store", false, false, 768)
)
```

---

## 2. BackendApplicationTests 컨텍스트 로드 실패

### 원인

`MainDataSourceConfig`가 `@Value("${spring.datasource.url}")` 어노테이션으로 MySQL URL을 직접 읽는다.
테스트 환경의 `src/test/resources/application.yml`에는 `spring.datasource.url`이 설정되지 않아
`PlaceholderResolutionException`이 발생했다.

과거 주석에는 "H2가 클래스패스에 있으면 datasource URL 없이도 자동 구성된다"고 되어 있었으나,
`MainDataSourceConfig`가 `DataSource`를 명시적으로 등록하면서 Spring Boot의 자동 구성(`DataSourceAutoConfiguration`)이
`@ConditionalOnMissingBean` 조건에 의해 백오프(back-off)되어 H2 자동 설정도 동작하지 않게 되었다.

### 수정 내용

`src/test/resources/application.yml`에 H2 인메모리 datasource를 명시적으로 추가하였다.

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL
    username: sa
    password: ""
    driver-class-name: org.h2.Driver
    hikari:
      maximum-pool-size: 5
      minimum-idle: 1
```

`MODE=MySQL`을 사용하여 H2가 MySQL 호환 모드로 동작하게 설정하였다.

---

## 임의로 결정하고 행동한 부분

- `MainDataSourceConfig` 자체에 `@ConditionalOnProperty(name = "spring.datasource.url")` 조건을 추가하는 방법도 가능하지만,
  기존 설계 의도(`MainDataSourceConfig`는 항상 MySQL DataSource를 명시적으로 등록한다)를 유지하기 위해
  테스트 yml 에 H2 설정을 추가하는 방향을 선택하였다.
- H2 URL에 `MODE=MySQL`을 추가하여 MySQL 방언을 기대하는 Hibernate DDL 구문과의 호환성을 높였다.

---

## 개발자가 알아둬야 하는 내용

### 1. `MainDataSourceConfig`와 테스트 환경

`MainDataSourceConfig`가 `@Value`로 datasource 프로퍼티를 직접 읽기 때문에,
새로운 `@SpringBootTest` 기반 통합 테스트를 작성할 때는 반드시
`src/test/resources/application.yml`에 `spring.datasource.url`이 설정되어 있어야 한다.

### 2. RagProperties inner record 변경 시 테스트 동기화

`RagProperties`의 중첩 레코드(inner record)나 생성자 시그니처가 변경될 경우,
`RagVectorStoreRepositoryTest`의 `setUp()` 메서드도 함께 업데이트해야 한다.

### 3. H2 MySQL 호환 모드

테스트용 H2 URL에 `MODE=MySQL`이 포함되어 있으므로, MySQL 전용 함수나 키워드를 사용하는 경우에도
대부분 H2에서 동작한다. 단, `JSON` 타입 같이 MySQL 특화 기능은 별도 확인이 필요하다.

