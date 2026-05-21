# OMR 연동 수정 내역 (임시 문서)

작성일: 2026-05-21

## 왜 수정했는가
OMR 서버 개발자가 전달한 최신 지침에 맞춰, 기존의 OMR 결과 결합 규칙을 수정했다.

핵심 변경점은 두 가지다.

1. 업로드 허용 파일 형식 변경
   - 이전: `png`, `jpg`, `jpeg`, `pdf`
   - 현재: `png`, `jpg`, `jpeg`

2. chord assignment 결합 규칙 변경
   - `aligned`: 전체 마디를 안전하게 결합
   - `partial`: `musicxml_measure_number`가 있는 마디만 결합
   - `mismatch`: 자동 chord-to-MusicXML 결합을 하지 않음

## 실제 반영 내용

### 1) 공통 OMR 클라이언트 수정
파일: `src/main/java/com/jazzify/backend/shared/omr/OmrClient.java`

- `measure_alignment.status == partial` 인 경우 예외를 던지지 않도록 수정
- `musicxml_measure_number`가 있는 마디만 chord map에 반영하도록 유지
- `measure_alignment.status == mismatch` 또는 알 수 없는 상태면 자동 결합을 생략하고 빈 chord map을 반환하도록 수정
- 이 경우 서버 처리 자체는 실패로 보지 않고 MusicXML 파싱은 계속 진행
- PDF media type 전송 로직 제거

### 2) 파일 검증 규칙 수정
파일: `src/main/java/com/jazzify/backend/shared/omr/OmrFileValidator.java`

- 허용 확장자에서 `pdf` 제거

파일: `src/main/java/com/jazzify/backend/shared/exception/code/OmrErrorCode.java`

- `OMR_004` 메시지를 `PNG, JPG, JPEG만 허용`으로 수정

### 3) solo / lick 설명 문구 정리
다음 파일들의 설명/Swagger 문구를 최신 규칙에 맞춰 수정했다.

- `src/main/java/com/jazzify/backend/domain/solo/service/SoloService.java`
- `src/main/java/com/jazzify/backend/domain/solo/service/implementation/SoloOmrProcessor.java`
- `src/main/java/com/jazzify/backend/domain/solo/controller/SoloControllerSpec.java`
- `src/main/java/com/jazzify/backend/domain/lick/service/LickService.java`
- `src/main/java/com/jazzify/backend/domain/lick/service/implementation/LickOmrProcessor.java`
- `src/main/java/com/jazzify/backend/domain/lick/controller/LickControllerSpec.java`
- `src/main/java/com/jazzify/backend/domain/chordproject/controller/ChordProjectControllerSpec.java`

## 테스트 보강 내용

### 추가/수정한 테스트
- `src/test/java/com/jazzify/backend/shared/omr/OmrClientTest.java`
  - `partial`이면 안전하게 매핑된 마디만 결합되는지 검증
  - `mismatch`이면 자동 결합을 생략하는지 검증

- `src/test/java/com/jazzify/backend/shared/omr/OmrFileValidatorTest.java`
  - PNG 허용 검증
  - PDF 거부 검증

- `src/test/java/com/jazzify/backend/domain/solo/service/implementation/SoloOmrProcessorTest.java`
  - PNG 허용 검증
  - PDF 거부 검증
  - partial map 입력 시 일부 마디만 chord가 채워지는지 검증

- `src/test/java/com/jazzify/backend/domain/lick/service/implementation/LickOmrProcessorTest.java`
  - PNG 허용 검증
  - PDF 거부 검증
  - partial map 입력 시 일부 마디만 chord가 채워지는지 검증

- 기존 `ChordProjectOmrProcessorTest`, `SheetProjectOmrProcessorTest`의 허용 파일 형식을 PNG 기준으로 정리

## 검증 결과
아래 검증을 실제로 수행했고 모두 통과했다.

1. OMR 관련 단위 테스트 묶음 실행
2. `*Omr*` 패턴 전체 테스트 실행
3. 전체 `gradlew test` 실행

## 현재 동작 정리
- PNG/JPG/JPEG만 OMR 업로드 가능
- partial 정렬이면 안전한 마디만 chord 결합
- mismatch 정렬이면 chord 자동 결합 없이 MusicXML만 기반으로 계속 처리
- 따라서 solo/lick/chordproject/sheetproject 모두 공유 OMR 클라이언트 기준으로 최신 계약을 따르게 됨

