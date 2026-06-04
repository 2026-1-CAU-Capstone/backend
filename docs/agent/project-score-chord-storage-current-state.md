# SheetProject / ChordProject 저장 방식 현황

작성일: 2026-06-04

## 목적

`SheetProject`와 `ChordProject`가 현재 악보 정보와 코드 정보를 어떻게 저장하는지 코드 기준으로 정리한다.

이 문서는 코드 수정 없이 현황만 설명한다.

## 요약

| 도메인 | 악보 정보 저장 방식 | 코드 정보 저장 방식 | 원본 JSON 저장 여부 | 물리 파일 저장 여부 |
| --- | --- | --- | --- | --- |
| `ChordProject` | 별도 악보 파일 없음. 프로젝트 메타데이터만 `tb_chord_project`에 저장 | `tb_chord_info`에 코드별 정규화 row로 저장 | 저장하지 않음 | 저장하지 않음 |
| `SheetProject` | `SheetFile`과 `StorageFile`을 통해 업로드 파일 메타데이터와 실제 파일을 저장 | OMR 결과를 progression 문자열로 변환한 뒤 `tb_chord_info`에 코드별 정규화 row로 저장 | 저장하지 않음 | 저장함 |

현재 구현은 악보/코드 정보를 JSON 하나로 통째 저장하는 구조가 아니다. 특히 `SheetProject`는 OMR에서 받은 MusicXML과 chord assignments를 파싱한 뒤 필요한 코드 진행만 `ChordInfo` row로 저장하고, 악보 전체 구조나 원본 OMR JSON은 DB에 보존하지 않는다.

## 주요 클래스 역할

| 클래스 | 역할 |
| --- | --- |
| `ChordProject` | 코드 프로젝트의 제목, 조성, 박자, OMR 상태, 분석 통계를 저장하는 JPA 엔티티 |
| `SheetProject` | 악보 프로젝트의 제목, 조성, OMR 상태, 사용자, `SheetFile` 연결을 저장하는 JPA 엔티티 |
| `SheetFile` | 악보 파일 단위를 표현하는 JPA 엔티티. 파일 타입과 `StorageFile` 목록을 연결 |
| `StorageFile` | 업로드 파일의 원본명, 저장명, 경로, 크기, MIME 타입을 저장하는 JPA 엔티티 |
| `ChordInfo` | 코드 하나의 위치 정보(`bar`, `beat`, `durationBeats`)와 코드 문자열을 저장하는 공용 엔티티 |
| `ChordProjectService` | 코드 프로젝트 생성, 코드 입력, OMR 콜백, 분석 실행을 오케스트레이션 |
| `SheetProjectService` | 악보 프로젝트 생성, OMR 제출/콜백 처리를 오케스트레이션 |
| `ChordProjectOmrProcessor` | chord-chart OMR 결과를 가져와 progression 문자열로 변환 |
| `SheetProjectOmrProcessor` | MusicXML과 chord assignments를 가져와 `ParsedSheetData`로 파싱 후 progression 문자열로 변환 |
| `IRealProChordParser` | progression 문자열을 `ChordInfo` 리스트로 변환 |
| `StorageFileService` | 업로드 파일 메타데이터를 DB에 저장하고 파일 저장 이벤트를 발행 |
| `LocalFileStorageService` | 실제 파일 바이트를 사용자 홈의 `jazzify` 디렉터리에 저장 |

## ChordProject 저장 방식

### 프로젝트 메타데이터

`ChordProject`는 `tb_chord_project` 테이블에 저장된다.

주요 저장 값:

| 컬럼 성격 | 필드 |
| --- | --- |
| 기본 메타데이터 | `title`, `keySignature`, `timeSignature` |
| OMR 상태 | `omrStatus`, `omrProgress`, `omrFailureReason`, `omrJobId` |
| OMR 요청 보정값 | `omrRequestedTitle`, `omrRequestedKey`, `omrRequestedTimeSignature` |
| 소유자/세션 | `user`, `session` |
| 분석 통계 | `lastAnalyzedAt`, `totalChords`, `highConfidenceCount`, `ambiguousCount`, `meanAmbiguityScore`, `maxAmbiguityScore` |

`ChordProject` 엔티티 안에는 현재 코드 진행 JSON 또는 악보 JSON을 담는 컬럼이 없다.

### 코드 정보

코드 진행은 `ChordProject` 내부 JSON이 아니라 `ChordInfo` row 목록으로 저장된다.

흐름:

```text
POST /v1/chord-projects/{publicId}/chords
  -> AddChordsRequest.progression
  -> 기존 ChordInfo 삭제
  -> IRealProChordParser.parse(...)
  -> tb_chord_info row 여러 개 저장
```

`ChordInfo`에 저장되는 값:

| 필드 | 의미 |
| --- | --- |
| `chord` | 코드 심볼. `N.C.`는 `null`로 정규화 |
| `bar` | 마디 번호 |
| `beat` | 마디 안 시작 박 |
| `durationBeats` | 지속 박수 |
| `chordProject` | 연결된 `ChordProject` |
| `sheetProject` | `ChordProject` 코드인 경우 `null` |

예를 들어 `Cmaj7 | Dm7 G7` 입력은 마디와 박 단위로 쪼개져 `tb_chord_info`에 여러 row로 저장된다.

### ChordProject OMR 저장 흐름

```text
createFromOmr(file)
  -> OMR 서버 /chords/chart/{dev|prod}/process 제출
  -> 콜백 수신
  -> OmrClient.fetchChordChart(jobId)
  -> ChordProjectOmrProcessor가 progression 문자열 생성
  -> ChordProjectOmrWriter.complete(...)
  -> 기존 ChordInfo 삭제
  -> IRealProChordParser.parse(...)
  -> tb_chord_info 저장
```

현재 저장되는 것은 파싱된 `ChordInfo` row다. OMR 서버가 반환한 chord-chart 원본 JSON은 그대로 저장하지 않는다.

## SheetProject 저장 방식

### 프로젝트 메타데이터

`SheetProject`는 `tb_sheet_project` 테이블에 저장된다.

주요 저장 값:

| 컬럼 성격 | 필드 |
| --- | --- |
| 기본 메타데이터 | `title`, `keySignature` |
| OMR 상태 | `omrStatus`, `omrProgress`, `omrFailureReason`, `omrJobId` |
| 소유자/세션 | `user`, `session` |
| 파일 연결 | `sheetFile` |
| 코드 연결 | `chordInfos` |

`SheetProject` 엔티티 안에도 현재 악보 전체 JSON을 담는 컬럼이 없다.

### 파일 정보

`SheetProject`는 `SheetFile`을 반드시 가진다.

```text
SheetProject
  -> SheetFile
    -> StorageFile 목록
      -> 실제 파일 경로
```

`SheetProjectResponse.filePublicId`는 실제 `StorageFile.publicId`가 아니라 `SheetFile.publicId`다.

### 물리 파일 저장

일반 생성과 OMR 생성 모두 업로드 파일을 물리적으로 저장하는 흐름이 존재한다.

```text
StorageFileService.upload(...)
  -> tb_storage_file row 생성
  -> StorageFileSavedEvent 발행
  -> 트랜잭션 커밋 후 StorageFileEventListener 실행
  -> LocalFileStorageService.store(...)
  -> {user.home}/jazzify/yyyy/MM/dd/{uuid}.{ext} 에 파일 저장
```

즉 현재 `SheetProject`는 악보 이미지를 물리 파일로 보존한다.

### 일반 SheetProject 생성 흐름

```text
POST /v1/sheet-projects
  -> SheetProjectCreateRequest.storageFileIds
  -> StorageFile 조회
  -> 첫 번째 파일명으로 FileType 결정
  -> SheetFile 생성
  -> StorageFile.linkToSheetFile(sheetFile)
  -> SheetProject 생성
```

이 흐름은 이미 업로드되어 있는 `StorageFile`을 `SheetFile`에 연결한다. 악보 정보 JSON은 생성하지 않는다.

### SheetProject OMR 저장 흐름

```text
createFromOmr(file)
  -> 업로드 파일 바이트 읽기
  -> SheetProjectOmrWriter.createPending(...)
      -> StorageFileService.upload(...)
      -> 실제 파일 저장 이벤트 발행
      -> SheetFile 생성
      -> StorageFile과 SheetFile 연결
      -> SheetProject PENDING 생성
  -> OMR 서버 /omr/{dev|prod}/process 제출
  -> 콜백 수신
  -> OmrClient.fetchMusicXml(jobId)
  -> OmrClient.fetchChordAssignments(jobId)
  -> MusicXmlParser.parse(...)
  -> ParsedSheetData 생성
  -> ParsedSheetData.measures를 progression 문자열로 변환
  -> 기존 SheetProject ChordInfo 삭제
  -> IRealProChordParser.parseForSheetProject(...)
  -> tb_chord_info 저장
```

OMR 처리 중 메모리에서는 `ParsedSheetData`가 만들어진다. 이 객체는 제목, 작곡가, 조성, 박자, 템포, 마디, 음표 정보를 포함할 수 있지만 현재 DB에는 저장하지 않는다.

최종적으로 DB에 남는 것은 다음뿐이다.

| 저장 대상 | 저장 위치 |
| --- | --- |
| 프로젝트 제목/조성/OMR 상태 | `tb_sheet_project` |
| 파일 타입 | `tb_sheet_file` |
| 파일 메타데이터 | `tb_storage_file` |
| 실제 업로드 파일 | `{user.home}/jazzify/...` |
| 파싱된 코드 정보 | `tb_chord_info` |

MusicXML 원문, chord assignments 원본 JSON, `ParsedSheetData` 전체 JSON은 저장되지 않는다.

## 논리 흐름도

### ChordProject

```text
사용자 입력 또는 Chord Chart OMR
  -> progression 문자열
  -> IRealProChordParser
  -> ChordInfo row 목록
  -> tb_chord_info
  -> 분석 시 ChordInfo row를 다시 progression 텍스트로 조립
```

### SheetProject

```text
악보 파일 업로드
  -> StorageFile metadata DB 저장
  -> 실제 파일 디스크 저장
  -> SheetFile 생성 및 연결
  -> SheetProject 생성

OMR 완료 후
  -> MusicXML + chord assignments 조회
  -> ParsedSheetData로 파싱
  -> progression 문자열 추출
  -> ChordInfo row 목록 저장
```

## 현재 구조에서 알아둘 점

1. `ChordProject`와 `SheetProject` 모두 JSON 통째 저장 방식이 아니다.
2. 코드 정보는 공통적으로 `ChordInfo` 엔티티에 row 단위로 정규화되어 저장된다.
3. `SheetProject`는 악보 파일을 물리적으로 저장한다.
4. `SheetProject`는 OMR로 얻은 악보의 음표/마디 전체 구조를 DB에 저장하지 않는다.
5. OMR 원본 응답 JSON은 두 도메인 모두 저장하지 않는다.
6. `ChordProject` 분석 기능은 현재 `ChordInfo` row 조회를 전제로 동작한다.
7. `SheetFile`은 현재 `SheetProject` 생성에 필수 관계로 모델링되어 있다.

## 향후 JSON 통째 저장으로 변경할 때 영향이 큰 지점

현황상 다음 부분이 변경 대상이 될 가능성이 크다.

| 영역 | 현재 방식 | 변경 시 고려점 |
| --- | --- | --- |
| `ChordProject` | `ChordInfo` row 저장 | 프로젝트 엔티티에 코드 JSON 컬럼 추가, 분석 입력 생성 방식 변경 필요 |
| `SheetProject` | `SheetFile` + `StorageFile` + 물리 파일 저장 | 파일 저장 제거, 악보 JSON 컬럼 추가 필요 |
| OMR 처리 | OMR 결과를 progression으로 축약 | OMR 결과가 기존 JSON 형식과 다르면 변환기 필요 |
| 분석 기능 | `ChordInfoReader`로 코드 row 조회 | JSON에서 분석 입력을 재구성하거나 분석용 view/model 필요 |
| 응답 DTO | `filePublicId` 또는 메타데이터 중심 | JSON 본문 응답 필드 추가/변경 필요 |

