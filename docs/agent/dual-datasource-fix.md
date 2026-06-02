# Dual DataSource 설정 버그 수정

## 작업 내용

- `MainDataSourceConfig.java` 신규 생성 (`core/config/`)
- `application-dev.yml` JPA 프로퍼티에 `non_contextual_creation: true` 추가

---

## 문제 원인

### 증상
```
Caused by: org.postgresql.util.PSQLException: ERROR: relation "tb_sheet_file" does not exist
```
```
java.sql.SQLFeatureNotSupportedException: Method org.postgresql.jdbc.PgConnection.createClob() is not yet implemented.
```

메인 DB는 MySQL임에도 불구하고 Hibernate가 PostgreSQL에 연결을 시도한다.

### 원인 분석

Spring Boot의 `DataSourceAutoConfiguration`은 아래 조건을 가진다.

```java
@ConditionalOnMissingBean({ DataSource.class, XADataSource.class })
```

`RagDataSourceConfig`가 `ragDataSource` 빈(`HikariDataSource implements DataSource`)을 등록하면,
이 조건이 `true`로 평가되어 **자동 구성이 백오프(back-off)**된다.

결과적으로:

1. Spring Boot가 `spring.datasource.*` 프로퍼티로 MySQL DataSource를 **생성하지 않는다**
2. 컨텍스트에 남은 유일한 `DataSource` 빈이 `ragDataSource` (PostgreSQL)
3. Hibernate JPA Auto-Configuration이 이 PostgreSQL DataSource를 사용
4. `ddl-auto: update` 설정에 의해 MySQL 엔티티 테이블을 PostgreSQL에서 조회/생성 시도 → 오류

---

## 해결 방법

### `MainDataSourceConfig.java`

`core/config/`에 MySQL DataSource를 `@Primary`로 **명시적 등록**하는 설정 클래스를 추가한다.

```java
@Primary
@Bean(name = "dataSource")
public HikariDataSource dataSource() { ... }
```

`@Primary`가 붙으면 Hibernate JPA Auto-Configuration이 이 빈을 우선 선택하고,
`ragDataSource`(PostgreSQL)는 RAG 전용으로만 사용된다.

### `application-dev.yml`

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          lob:
            non_contextual_creation: true
```

Hibernate가 LOB 타입 지원 여부 확인 시 PostgreSQL JDBC의 `createClob()` 미구현 경고를 억제한다.
RAG DataSource 초기화 시 PostgreSQL 연결에서 발생하는 경고성 예외를 방지한다.

---

## 클래스 역할 표

| 클래스 | 위치 | 역할 |
|---|---|---|
| `MainDataSourceConfig` | `core/config/` | MySQL 메인 DataSource를 `@Primary`로 명시적 등록 |
| `RagDataSourceConfig` | `domain/rag/config/` | PostgreSQL RAG DataSource, JdbcTemplate, TransactionManager 등록 |
| `RagVectorStoreConfig` | `domain/rag/config/` | `ragJdbcTemplate`을 사용해 pgvector VectorStore 빈 등록 |

## 논리 흐름도

```
Spring Boot 시작
       │
       ├─ MainDataSourceConfig
       │       └─ @Primary dataSource (MySQL, HikariCP)
       │               └─ JPA EntityManagerFactory (Hibernate) ──▶ MySQL
       │
       └─ RagDataSourceConfig  (@ConditionalOnProperty rag.enabled=true)
               ├─ ragDataSource (PostgreSQL, HikariCP)
               ├─ ragJdbcTemplate ──────────────────────────────▶ PostgreSQL
               ├─ ragJdbcClient
               └─ ragTransactionManager
                       │
                       └─ RagVectorStoreConfig
                               └─ ragVectorStore (PgVectorStore) ─▶ PostgreSQL
```

---

## 개발자가 알아둬야 하는 내용

### 1. `@ConditionalOnMissingBean` 백오프 주의
Spring Boot 자동 구성에서 `DataSource`, `JdbcTemplate`, `TransactionManager` 등 공통 인프라 빈을 추가 등록할 때는 반드시 기본 빈이 `@Primary`로 명시되어 있어야 한다. 그렇지 않으면 자동 구성이 백오프되어 메인 인프라 빈이 아예 생성되지 않는다.

### 2. Multi-DataSource 시 `@Qualifier` 필수
RAG 관련 Repository(`RagDocumentRepository`, `RagChunkRepository`)는 `@Qualifier("ragJdbcTemplate")`로 PostgreSQL JdbcTemplate을 명시적으로 주입받고 있다. 이 패턴이 깨지지 않도록 신규 Repository 작성 시 주의한다.

### 3. JPA 엔티티는 MySQL 전용
모든 `@Entity` 클래스는 `@Primary` DataSource(MySQL)를 통해 Hibernate가 관리한다. PostgreSQL은 순수 JDBC(`JdbcTemplate`)로만 접근하며 JPA 엔티티가 없다.

### 4. `rag.enabled=false` 시 동작
`rag.enabled=false`이면 `RagDataSourceConfig`가 활성화되지 않아 PostgreSQL DataSource가 등록되지 않는다. 이 경우에도 `MainDataSourceConfig`가 MySQL DataSource를 정상 등록하므로 문제없다.

