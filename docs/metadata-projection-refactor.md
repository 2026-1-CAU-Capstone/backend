# Lick/Solo 메타데이터 Projection 분리

## 변경 배경

기존 구현에서는 repository 의 JPQL constructor projection 이 API 응답 DTO(`dto/response`)를 직접 생성하고 있었다.
이 방식은 다음 문제를 만든다.

- persistence 계층이 web/api 응답 모델을 직접 의존함
- repository 변경이 곧 API 스펙 변경으로 이어질 수 있음
- service 계층이 orchestration 대신 단순 전달 계층으로 약화됨
- projection 재사용 범위가 API 응답 형태에 묶임

## 이번 변경 내용

repository 와 service 사이에서만 사용하는 projection/result DTO를 도메인별 `dto/app/` 패키지로 분리했다.

### 추가된 DTO

- `domain/lick/dto/app/LickMetadataValueCountResult`
- `domain/solo/dto/app/SoloMetadataValueCountResult`

### 계층별 역할

- `repository`
  - JPQL projection 결과를 `dto/app/*Result` 로 반환
- `service/implementation (Reader)`
  - repository 결과를 그대로 받아 service 로 전달
- `service`
  - `dto/app/*Result` 를 API 응답 DTO(`dto/response/*Response`)로 변환
- `controller`
  - 기존과 동일하게 `ApiResponse<List<...Response>>` 반환

## 기대 효과

- repository 가 API 응답 모델을 모르도록 계층 경계가 명확해짐
- projection 구조와 API 응답 구조를 독립적으로 변경 가능
- service 계층이 결과 조합/변환 책임을 명확히 가짐
- 이후 count 외 다른 메타데이터 집계가 추가되어도 같은 패턴으로 확장 가능

## 현재 흐름

### Lick

1. `LickRepository.findComposerCounts/findPerformerCounts`
2. `LickMetadataValueCountResult`
3. `LickService`
4. `LickMetadataValueCountResponse`
5. `LickController`

### Solo

1. `SoloRepository.findComposerCounts/findPerformerCounts`
2. `SoloMetadataValueCountResult`
3. `SoloService`
4. `SoloMetadataValueCountResponse`
5. `SoloController`

