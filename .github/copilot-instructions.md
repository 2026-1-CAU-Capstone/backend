# Jazzify Backend – Copilot Instructions

## 프로젝트 개요

재즈 음악 학습·분석 플랫폼의 백엔드 API 서버.
코드 진행(chord progression) 관리, 악보 프로젝트 관리, 규칙 기반 화성 분석 엔진을 제공한다.

## 도메인 패키지 내부 구조 (컨벤션)

모든 도메인 패키지는 아래 하위 구조를 따른다.

```
domain/{도메인}/
├── controller/
│   ├── {Domain}Controller.java         # @RestController, implements Spec
│   └── {Domain}ControllerSpec.java     # Swagger 문서용 interface
├── dto/
│   ├── request/                        # 요청 DTO (record)
│   └── response/                       # 응답 DTO (record)
├── entity/                             # JPA @Entity (DB 사용 시)
├── model/                              # 내부 도메인 모델 (DB 미사용 시)
├── repository/                         # Spring Data Repository (DB 사용 시)
├── config/                             # 도메인 전용 설정 (필요 시)
├── event/                              # 이벤트 클래스 (필요 시)
├── service/
│   ├── {Domain}Service.java            # @Service – 비즈니스 로직 오케스트레이터
│   └── implementation/                 # @Component – 세부 구현 단위
│       ├── {Domain}Reader.java         # 조회 전용
│       └── {Domain}Writer.java         # 쓰기 전용
└── util/
    └── {Domain}Mapper.java             # DTO ↔ Entity 변환 (static 메서드)
```

### 핵심 규칙

- **Service** (`@Service`): 공개 비즈니스 메서드만 노출. `implementation/` 내부 컴포넌트를 조합하여 유스케이스를 구성한다.
- **implementation** (`@Component`): Service가 위임하는 세부 단위. Reader(조회), Writer(쓰기), Detector, Scorer 등 역할별로 분리한다. Service 바깥에서 직접 사용하지 않는다.
implementation계층간 의존은 동일 도메인 내에서만 허용하며 순환 의존에 주의 한다.
- **Controller**: `{Domain}ControllerSpec` 인터페이스를 구현한다. Swagger 어노테이션은 Spec 인터페이스에만 둔다.
- **DTO**: 모두 Java `record`로 작성한다. 요청 DTO에 Jakarta Validation 어노테이션을 사용한다.
- **Mapper**: `util/` 패키지에 `@NoArgsConstructor(access = PRIVATE) final class`로 작성하고, `static` 변환 메서드만 둔다.

## 코드 스타일

### 필수 어노테이션

- 모든 클래스에 `@NullMarked` (jspecify)를 붙인다.
- nullable 필드/파라미터/리턴에는 `@Nullable`을 명시한다.
- Lombok: `@Getter`, `@RequiredArgsConstructor`, `@Builder`, `@Data` 활용.
- Entity: `@Builder`, `@Getter`, `@NoArgsConstructor(access = PROTECTED)`, `@AllArgsConstructor(access = PRIVATE)`.

### REST API

- URL 접두사: `/v1/{도메인}` (e.g., `/v1/chord-projects`, `/v1/analysis`).
- 응답 래퍼: 항상 `ApiResponse<T>`로 감싼다.
- 에러: `CustomException` + `BaseErrorCode` enum

### Entity

- 모든 JPA 엔티티는 `BaseEntity`를 상속한다 (`id`, `publicId`, `createdAt`, `updatedAt` 자동 관리).
- 외부 식별자로 `UUID publicId`를 사용하고, 내부 PK는 `Long id` (auto-increment).

