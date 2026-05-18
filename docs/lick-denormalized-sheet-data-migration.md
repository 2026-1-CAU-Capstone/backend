# Lick sheetData 반정규화 전환 및 마이그레이션

## 배경
기존 `Lick` 조회는 다음 구조를 사용했다.
- `Lick`
- `LickMeasure`
- `LickNote`

즉, 응답의 `sheetData`를 만들기 위해 `Lick -> Measure -> Note` 2단계 연관 데이터를 따라가야 했고,
조회 시점에 join / 추가 select 비용이 커졌다.

## 변경 방향
`Lick` 엔티티에 응답과 동일한 구조의 `sheetData`를 JSON으로 통째로 저장하도록 변경했다.

추가 컬럼:
- `sheet_data_json`

이 컬럼에는 `LickResponse.sheetData`와 동일한 구조의 JSON이 저장된다.

## 현재 동작 방식

### 신규 생성 / 수정
- `LickWriter.create(...)`
- `LickWriter.update(...)`

에서 더 이상 `LickMeasure` / `LickNote`를 새로 생성하지 않는다.
대신 `sheetData`를 JSON으로 직렬화해서 `Lick.sheetDataJson`에 저장한다.

### 조회
- `LickMapper.toSheetDataResponse(Lick)`는 우선 `sheetDataJson`을 파싱한다.
- 만약 아직 마이그레이션되지 않은 기존 데이터라서 JSON이 비어 있으면,
  레거시 `measures/notes`를 fallback으로 사용한다.

즉, 배포 직후에도 기존 데이터 조회가 깨지지 않도록 설계했다.

## 기존 데이터 마이그레이션
`LickSheetDataMigrationService`가 애플리케이션 시작 시 자동 실행된다.

### 구현 주의사항
초기 구현에서는 `ApplicationRunner`와 `@Transactional` 메서드가 같은 클래스에 있어서,
`run()` → `migrateMissingSheetDataJson()` 내부 호출이 self-invocation이 되어
트랜잭션 프록시를 타지 못하는 문제가 있었다.

그 결과 `sheet_data_json` 값 변경이 실제 DB에 반영되지 않아,
`sheet_data_json IS NULL` 조회가 같은 `Lick`를 계속 반환하면서 반복 조회가 발생할 수 있었다.

현재는 아래처럼 수정했다.
- `LickSheetDataMigrationRunner`: 시작 시 호출 전용
- `LickSheetDataMigrationService`: 실제 `@Transactional` 마이그레이션 수행

또한 각 엔티티 변경 후 `save` / 배치 `flush`를 통해 DB 반영을 명시적으로 보장한다.

### 마이그레이션 대상
- `sheet_data_json IS NULL` 인 `Lick`

### 마이그레이션 절차
1. `Lick` ID를 배치 단위로 조회
2. 해당 `Lick`의 기존 `LickMeasure` / `LickNote`를 읽음
3. 기존 응답과 동일한 `SheetDataResponse` 구조로 변환
4. JSON 직렬화 후 `sheet_data_json`에 저장

### 안전장치
- 이미 `sheet_data_json`이 채워진 데이터는 건너뜀
- 배치 방식으로 반복 수행
- 마이그레이션 전에도 fallback이 있으므로 조회 장애를 줄임

## 레거시 테이블 처리
현재는 데이터 백필과 fallback 호환성을 위해 아래 엔티티/테이블을 즉시 제거하지 않았다.
- `LickMeasure`
- `LickNote`

하지만 신규/수정 저장 경로에서는 더 이상 이 구조를 사용하지 않는다.

즉, 현재 상태는 다음과 같다.
- **쓰기 경로:** JSON 저장만 사용
- **읽기 경로:** JSON 우선, 레거시 fallback
- **마이그레이션 완료 후:** 레거시 테이블 제거를 후속 작업으로 진행 가능

## 테스트
추가/보강한 테스트:
- `LickWriterTest`
  - 생성 시 `sheetDataJson`이 저장되는지 검증
- `LickSheetDataMigrationServiceTest`
  - 기존 `LickMeasure` / `LickNote` 데이터가 JSON으로 백필되는지 검증

## 기대 효과
- `Lick` 단건/목록 조회 시 `sheetData` 응답 생성 비용 감소
- 다단계 연관 조회 제거로 성능 개선
- 응답 생성 로직 단순화
- 기존 데이터는 자동 마이그레이션으로 안전하게 전환

