# Lick / Solo 레거시 코드 제거용 프롬프트

작성일: 2026-05-20

아래 프롬프트를 그대로 복붙해서 사용하면 됩니다.

---

## Prompt

```text
Jazzify 백엔드 프로젝트에서 Lick / Solo의 sheetData JSON 마이그레이션이 운영상 완전히 끝났다고 가정하고,
이제 legacy 코드(정규화 measure/note 저장 구조, migration 코드, fallback 코드 등)를 안전하게 제거해줘.

작업 목표:
- Lick / Solo 모두에서 이제 sheetData는 `sheetDataJson`만 사용한다.
- legacy `measure` / `note` 기반 저장/조회/마이그레이션/폴백 로직을 제거한다.
- 제거 후에도 현재 API 계약과 테스트가 유지되어야 한다.
- 불필요해진 엔티티, 리포지토리, 서비스, 테스트, 문서, Swagger 설명까지 함께 정리한다.

반드시 따라야 할 작업 원칙:
1. 먼저 현재 코드베이스에서 Lick / Solo의 legacy 의존 지점을 전부 추적해라.
2. 제거 전에 어떤 파일/심볼을 삭제 또는 수정할지 명확히 정리해라.
3. 무작정 삭제하지 말고, compile/test 기준으로 안전하게 단계별 수정해라.
4. public API가 바뀌면 안 된다. 특히 request/response 구조와 endpoint 계약은 유지해라.
5. 현재는 `composer`가 performance metadata에 있고 `sheetData`에는 없다. 이 기준을 절대 되돌리지 마라.
6. 최종적으로 dead code, 사용되지 않는 import, 테스트, 문서 설명까지 정리해라.
7. 변경 후에는 반드시 관련 테스트 + 전체 테스트를 실행해서 검증해라.
8. 코드 생성/수정 후에는 사람이 이해할 수 있는 설명 문서를 임시 파일로 생성해라.

구체 작업 범위:

[1] Lick legacy 제거
- `Lick` 엔티티에서 migration only 용도로 남겨둔 legacy measures 연관관계 제거 여부 검토 후 제거
- `LickMeasure`, `LickNote` 엔티티 제거
- `LickMeasureRepository` 제거
- `LickMapper`에서 legacy fallback 관련 메서드 제거
  - `toLegacySheetDataResponse(...)`
  - `toSheetDataResponse(Lick)` 내부의 legacy fallback 분기 제거
- `LickSheetDataMigrationService`, `LickSheetDataMigrationRunner` 제거
- 관련 테스트 제거 또는 현행 구조에 맞게 교체
  - migration test
  - runner test
  - legacy fallback 전제 테스트
- Swagger / 주석 / 문서에서 legacy 또는 migration-only 설명 제거

[2] Solo legacy 제거
- `Solo` 엔티티에서 migration only 용도로 남겨둔 legacy measures 연관관계 제거 여부 검토 후 제거
- `SoloMeasure`, `SoloNote` 엔티티 제거
- `SoloMeasureRepository` 제거
- `SoloMapper`에서 legacy fallback 관련 메서드 제거
  - `toLegacySheetDataResponse(...)`
  - `toSheetDataResponse(Solo)` 내부의 legacy fallback 분기 제거
- `SoloSheetDataMigrationService`, `SoloSheetDataMigrationRunner` 제거
- 관련 테스트 제거 또는 현행 구조에 맞게 교체
  - migration test
  - runner test
  - legacy fallback 전제 테스트
- Swagger / 주석 / 문서에서 legacy 또는 migration-only 설명 제거

[3] 영속성/DB 관점 정리
- JPA 매핑 제거로 인해 더 이상 참조되지 않는 테이블/연관관계가 없는지 확인해라.
- 현재 프로젝트에 DB migration 도구(Flyway/Liquibase 등)가 없다면, 코드 레벨 정리만 우선 수행하고,
  실제 운영 DB에서 어떤 테이블/컬럼을 추후 삭제해야 하는지 문서로 정리해라.
- 만약 DB migration 파일 체계가 이미 존재한다면,
  `tb_lick_measure`, `tb_lick_note`, `tb_solo_measure`, `tb_solo_note` 제거용 migration도 함께 작성해라.

[4] 테스트/검증
- 최소한 다음을 검증해라:
  - Lick 관련 테스트
  - Solo 관련 테스트
  - 전체 테스트
- 테스트가 깨지면 현행 JSON 저장 구조 기준으로 수정해라.
- 레거시 제거 후 unused warning 수준의 코드도 최대한 정리해라.

[5] 산출물 형식
- 실제 코드 수정까지 수행해라. 설명만 하지 말고 바로 수정해라.
- 마지막에는 아래 형식으로 보고해라:
  1. 제거한 legacy 구성요소 목록
  2. 수정한 핵심 파일 목록
  3. DB 차원에서 추후 제거해야 할 테이블/컬럼 목록
  4. 실행한 테스트와 결과
  5. 생성한 임시 설명 문서 경로

추가 주의사항:
- `sheetDataJson` 저장 방식은 유지해야 한다.
- request/response DTO에서 `sheetData` 구조는 현재 계약 그대로 유지해야 한다.
- `composer`는 performance metadata에 남겨야 하며 `sheetData`로 되돌리면 안 된다.
- OMR 생성 플로우도 현재 구조대로 유지해야 한다.
- 삭제가 가능한 항목은 과감하게 제거하되, 혹시 확신이 부족한 부분은 먼저 코드베이스를 끝까지 추적해서 확인한 뒤 수정해라.

작업이 끝나면 반드시 테스트까지 직접 실행해서 성공 여부를 확인해라.
```

---

## 사용 시점

이 프롬프트는 아래 조건이 모두 만족된 뒤 사용하는 것이 좋습니다.

- 운영 DB에서 `sheetDataJson` 마이그레이션이 100% 완료됨
- 더 이상 `tb_lick_measure`, `tb_lick_note`, `tb_solo_measure`, `tb_solo_note`를 읽는 런타임 경로가 필요 없음
- 롤백 계획 또는 백업이 확보됨

---

## 기대 효과

이 프롬프트를 사용하면 다음을 한 번에 정리하도록 유도할 수 있습니다.

- legacy 엔티티 제거
- migration 코드 제거
- fallback 제거
- 관련 테스트/문서/주석 정리
- DB 후속 정리 포인트 문서화

