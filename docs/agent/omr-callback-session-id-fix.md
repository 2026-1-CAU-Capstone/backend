# OMR callback session_id null 오류 수정 기록

## 작업 일자

2026-06-04

## 작업 배경

ChordProject OMR callback 처리 중 `Column 'session_id' cannot be null` 오류가 발생했다.
콜백 완료 시 OMR 결과 progression을 `ChordInfo`로 저장하는데, 운영 DB의 `tb_chord_info.session_id` 컬럼이 NOT NULL 상태로 남아 있으면 `ChordInfo.session == null` 저장에서 실패할 수 있다.

## 작업한 내용

- ChordProject OMR pending 생성 시 `Session`을 함께 생성하고 `ChordProject.session`에 연결하도록 수정했다.
- SheetProject도 동일하게 OMR 결과를 `ChordInfo`로 저장하므로 OMR pending 생성 시 `Session`을 함께 연결하도록 수정했다.
- `IRealProChordParser`가 `ChordProject` 또는 `SheetProject`의 session을 새 `ChordInfo`에 전파하도록 수정했다.
- 같은 마디 안에서 동일 코드가 병합되는 경우에도 `ChordInfo.session`이 유지되도록 수정했다.
- OMR writer 계층에서 JPA `EntityManager`로 `Session`을 저장하도록 했다.
- `IRealProChordParserTest`를 추가해 ChordProject/SheetProject 양쪽에서 session 전파를 검증했다.

## OMR 사용 도메인 검토

| 도메인 | OMR 결과 저장 방식 | session_id null 위험 | 조치 |
| --- | --- | --- | --- |
| ChordProject | 콜백 완료 시 `ChordInfo` 저장 | 있음 | OMR pending session 생성 및 `ChordInfo` 전파 |
| SheetProject | 콜백 완료 시 `ChordInfo` 저장 | 있음 | OMR pending session 생성 및 `ChordInfo` 전파 |
| Lick | OMR 결과를 lick 본문/`sheetDataJson`에 저장 | 없음 | 코드 변경 없음 |
| Solo | OMR 결과를 solo 본문/`sheetDataJson`에 저장 | 없음 | 코드 변경 없음 |

## 생성/변경 클래스 역할

| 클래스 | 역할 |
| --- | --- |
| `ChordProjectOmrWriter` | ChordProject OMR pending 생성/상태 변경/완료 저장을 담당하며, 이제 pending 생성 시 session도 생성 |
| `SheetProjectOmrWriter` | SheetProject OMR pending 생성/상태 변경/완료 저장을 담당하며, 이제 pending 생성 시 session도 생성 |
| `SheetProjectWriter` | 일반 SheetProject 저장 컴포넌트. OMR writer가 session을 넘길 수 있도록 overload 추가 |
| `IRealProChordParser` | progression 문자열을 `ChordInfo` 목록으로 변환하며, 이제 프로젝트 session을 `ChordInfo`에 전파 |
| `IRealProChordParserTest` | ChordProject/SheetProject 파싱 결과의 session 전파 회귀 테스트 |

## 클래스간 논리 흐름도

```text
ChordProjectService.createFromOmr
  -> ChordProjectOmrWriter.createPending
      -> EntityManager.persist(Session)
      -> ChordProjectRepository.save(session 연결)
  -> OmrClient.submitChordChartJob
  -> ChordProjectService.handleOmrCallback
      -> ChordProjectOmrProcessor.processJobResult
      -> ChordProjectOmrWriter.complete
          -> IRealProChordParser.parse(project.session 전파)
          -> ChordInfoWriter.saveAll

SheetProjectService.createFromOmr
  -> SheetProjectOmrWriter.createPending
      -> StorageFileService.upload
      -> SheetFileWriter.create
      -> EntityManager.persist(Session)
      -> SheetProjectWriter.create(session 연결)
  -> OmrClient.submitJob
  -> SheetProjectService.handleOmrCallback
      -> SheetProjectOmrProcessor.processJobResult
      -> SheetProjectOmrWriter.complete
          -> IRealProChordParser.parseForSheetProject(project.session 전파)
          -> ChordInfoWriter.saveAll
```

## 설계 의도

현재 엔티티 코드는 `ChordInfo.session`을 nullable로 표현하지만, 운영 DB는 과거 스키마 때문에 NOT NULL 제약을 유지할 수 있다.
`ddl-auto=update`는 기존 NOT NULL 제약을 안정적으로 nullable로 되돌리지 않으므로, OMR로 생성되는 `ChordInfo`에 실제 session을 넣는 방식으로 런타임 오류를 방지했다.

`ChordProject`와 `SheetProject`는 이미 session 연관 필드를 가지고 있어 새로운 도메인 모델을 만들지 않고 기존 관계를 사용했다.
별도 repository 클래스 추가 없이 writer의 기존 트랜잭션 안에서 `EntityManager.persist`로 session을 저장했다.
Lick/Solo는 `ChordInfo`를 만들지 않기 때문에 동일한 보정이 필요하지 않다.

## 임의로 결정한 부분

- OMR pending 생성 시 만들어지는 session 제목은 pending 프로젝트 제목과 동일하게 저장했다.
- 일반 ChordProject/SheetProject 생성 흐름은 이번 오류 범위가 OMR callback이므로 변경하지 않았다.
- 운영 DB의 `tb_chord_info.session_id` NOT NULL 제약을 직접 변경하는 자동 DDL 코드는 추가하지 않았다. 애플리케이션 코드에서 OMR 저장 데이터를 보강하는 방식으로 해결했다.

## 개발자가 알아둘 내용

- 기존에 session 없이 만들어진 ChordProject/SheetProject가 있고 그 프로젝트에 직접 chord를 저장하면, 운영 DB 스키마가 여전히 `tb_chord_info.session_id NOT NULL`인 경우 같은 문제가 발생할 수 있다.
- 장기적으로는 실제 DB 스키마와 엔티티 의도를 맞추는 migration을 별도로 검토하는 것이 좋다.
- 이번 수정 후 새 OMR 요청으로 생성되는 ChordProject/SheetProject는 callback 완료 시 `ChordInfo.session`이 채워진다.

## 테스트

```text
./gradlew.bat test --no-daemon
BUILD SUCCESSFUL
```
