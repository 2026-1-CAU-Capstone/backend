# OMR metadata 적용 문제 수정 기록

## 작업 일자

2026-06-04

## 작업 배경

Solo를 OMR로 생성할 때 사용자가 multipart form-data로 입력한 metadata가 최종 결과에 일관되게 반영되지 않는 문제가 있었다. 같은 OMR 생성 흐름을 사용하는 Lick, ChordProject, SheetProject에도 동일 또는 유사 문제가 있는지 점검했다.

## 수정한 내용

### 공통 metadata 기본값

- Solo/Lick 저장 시 `performer`, `composer`, `title`이 `null` 또는 blank이면 `"Unknown"`으로 정규화하도록 수정했다.
- DB default만 수정하지 않은 이유는 JPA가 `null` 값을 명시적으로 insert/update할 수 있어 DB default가 항상 적용되지 않기 때문이다.
- Writer 계층에서 먼저 정규화해 저장 직후 응답 객체가 `"Unknown"`을 보도록 했고, 엔티티의 `@PrePersist`, `@PreUpdate`에서도 한 번 더 정규화해 직접 repository save 경로도 방어했다.
- OMR PENDING 단계에서 composer가 `"Unknown"`으로 저장되더라도, 완료 콜백에서는 이를 사용자 입력값으로 보지 않고 MusicXML composer fallback을 계속 허용한다.

### Solo

- `SoloOmrRequest`에 누락되어 있던 `timeSignature` 필드를 추가했다.
- OMR 요청 metadata 문자열을 `trimToNull`로 정규화해 blank 문자열이 실제 값처럼 저장되지 않게 했다.
- 사용자가 제목을 입력하지 않은 경우 PENDING 제목을 파일명이 아니라 `OMR Processing`으로 저장하도록 변경했다.
- OMR 완료 콜백에서 최종 `SoloCreateRequest`를 만들 때 사용자 입력 metadata를 우선 적용하도록 정리했다.
- top-level 응답 필드뿐 아니라 저장되는 `sheetDataJson` 내부의 `title`, `key`, `timeSignature`, `tempo`에도 같은 우선순위를 적용했다.

### Lick

- Solo와 동일하게 `LickOmrRequest`에 `timeSignature`를 추가했다.
- PENDING 저장 시 사용자 입력 `timeSignature`도 보존하도록 `LickWriter.createPending` 시그니처를 확장했다.
- OMR 완료 후 `LickCreateRequest`와 `sheetDataJson`이 같은 metadata 값을 갖도록 보정했다.
- 제목 미입력 시 파일명 대신 MusicXML 파싱 제목을 최종 제목으로 사용할 수 있게 했다.

### SheetProject

- 사용자가 OMR 생성 요청에서 제목을 입력하지 않았을 때 파일명을 임시 제목으로 저장하던 동작을 중단했다.
- 제목 미입력 시 PENDING 제목은 `OMR Processing`으로 저장하고, 완료 콜백에서 MusicXML 제목을 최종 제목으로 적용하게 했다.

### ChordProject 점검 결과

- ChordProject는 이미 `omrRequestedTitle`, `omrRequestedKey`, `omrRequestedTimeSignature` 필드를 별도로 저장한다.
- 완료 콜백에서도 해당 요청 필드를 OMR 결과보다 우선 사용하고 있어 Solo/SheetProject와 같은 제목 fallback 오인 문제는 없었다.
- 이번 변경에서는 ChordProject 코드를 수정하지 않았다.

## 설계 의도

OMR metadata 우선순위는 다음처럼 통일했다.

1. 사용자가 명시한 metadata
2. OMR/MusicXML 파싱 결과
3. 최후 fallback 값

Solo와 Lick은 top-level metadata와 `sheetDataJson`을 함께 저장한다. 그래서 top-level만 고치면 프론트가 `sheetData`를 기준으로 읽을 때 여전히 OMR 파싱값이 보일 수 있다. 이번 수정은 두 저장 위치의 metadata를 같은 값으로 맞추는 데 초점을 뒀다.

## 임의로 결정한 부분

- 제목을 입력하지 않은 OMR PENDING 엔티티의 임시 제목은 기존 상수인 `OMR Processing`을 사용했다.
- 사용자가 제목을 입력하지 않은 상태에서는 Solo 중복 제목 검사를 건너뛰도록 했다. 이전처럼 파일명 또는 공통 임시 제목으로 중복 검사를 하면 실제 최종 제목과 무관하게 OMR 요청이 막힐 수 있기 때문이다.
- 사용자가 실제 제목으로 `OMR Processing`을 입력하는 극단적인 경우에는 제목 미입력 fallback과 구분되지 않는다. 별도 요청 metadata 컬럼을 Solo/Lick에 추가하면 더 엄밀히 구분할 수 있지만, 이번 수정은 DB 스키마 변경 없이 문제를 해결하는 범위로 제한했다.

## 개발자가 알아둘 내용

- Solo/Lick OMR multipart form-data에서 이제 `timeSignature` 필드가 정상적으로 바인딩된다.
- Solo/Lick의 `sheetData.title/key/timeSignature/tempo`는 OMR 완료 후 사용자 입력값이 있으면 top-level metadata와 동일하게 저장된다.
- SheetProject 제목 미입력 요청의 생성 직후 응답 제목은 `OMR Processing`이며, 완료 후 OMR 제목으로 바뀐다.
- ChordProject는 기존 요청 보정 컬럼 구조를 유지한다.

## 테스트

- `SoloWriterTest`에 OMR 완료 요청 재구성 시 사용자 metadata가 `sheetData`까지 반영되는지 검증을 추가했다.
- `SoloWriterTest`에 사용자 제목 미입력 시 OMR 파싱 제목을 사용하는 검증을 추가했다.
- `SoloWriterTest`에 blank/null performer, composer, title이 `"Unknown"`으로 저장되는 검증을 추가했다.
- `LickWriterTest`에 Solo와 동일한 두 가지 회귀 테스트를 추가했다.
- `LickWriterTest`에 blank/null performer, composer, title이 `"Unknown"`으로 저장되는 검증을 추가했다.
- 전체 테스트 실행 결과:

```text
./gradlew.bat test --no-daemon
BUILD SUCCESSFUL
```
