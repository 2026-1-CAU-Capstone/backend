# Chord-chart accepted_tokens fallback

## 작업 내용

ChordProject chord-chart OMR callback 완료 후 `ChordInfo`가 일부만 저장되는 문제를 수정했다.

첨부된 MusicVision 응답에서는 `chart_ocr.accepted_tokens`에 코드 후보 4개가 있었지만, 백엔드가 기존에 사용하던 `pages[].systems[].measures[].chords[]`에는 2개만 들어 있었다. 기존 백엔드는 measure chords만 progression으로 변환했기 때문에 최종 `ChordInfo`도 2개만 저장됐다.

이번 변경으로 measure bbox 안에 들어오는 `chart_ocr.accepted_tokens`를 함께 읽고, 같은 measure에서 accepted token 수가 measure chords 수보다 많으면 accepted token 목록을 progression source로 사용한다.

## 설계 의도

ChordProject 저장은 여전히 iRealPro 스타일 progression 문자열을 만들고 `IRealProChordParser`로 `ChordInfo`를 생성하는 기존 흐름을 유지했다. 저장 계층을 바꾸지 않고 OMR 결과 변환 단계에서 누락 보정만 수행해 수정 범위를 좁혔다.

accepted token은 measure에 직접 연결된 필드가 아니므로 bbox 중심점이 measure bbox 내부에 있는 경우에만 해당 measure의 코드로 간주한다. measure chords가 accepted token보다 같거나 많으면 기존 measure chords를 그대로 사용한다.

## 클래스 역할

새 top-level 클래스는 만들지 않았다.

| 클래스/record | 역할 |
| --- | --- |
| `OmrClient` | chord-chart JSON을 progression 문자열로 변환하고 accepted token fallback 적용 |
| `ChordChartResponse` | `chart_ocr` 필드를 추가로 역직렬화 |
| `ChartOcr` | `accepted_tokens` 목록을 담는 내부 DTO |
| `ChartMeasure` | measure `bbox`와 `chords`를 담는 내부 DTO |
| `ChordAssignment` | chord text, beat, bbox를 담는 내부 DTO |
| `Bounds` | bbox 중심점 포함 여부와 x 좌표 정렬 기준 계산 |

## 논리 흐름도

```mermaid
flowchart TD
    A[GET /omr/jobs/{jobId}/chord-chart] --> B[ChordChartResponse 역직렬화]
    B --> C[measure.chords 수집]
    B --> D[chart_ocr.accepted_tokens 수집]
    D --> E[accepted token bbox 중심점이 measure bbox 내부인지 검사]
    C --> F{accepted token 수 > measure chord 수?}
    E --> F
    F -- yes --> G[accepted tokens를 measure 코드로 사용]
    F -- no --> H[기존 measure.chords 사용]
    G --> I[좌표/beat 기준 정렬 후 progression 생성]
    H --> I
    I --> J[IRealProChordParser로 ChordInfo 저장]
```

## 임의로 결정한 부분

accepted token에는 beat가 없을 수 있으므로 bbox의 x 중심값으로 좌우 순서를 정했다. beat가 있는 measure chords는 기존처럼 beat 기준 정렬을 우선한다.

accepted token을 무조건 전체 progression으로 사용하지 않고, measure bbox에 매칭되는 token만 사용한다. 페이지 상단 제목/작곡가/스타일 OCR 텍스트가 chord로 오인되는 경우를 줄이기 위해서다.

## 개발자가 알아둬야 할 내용

- MusicVision 응답에서 `chart_ocr.accepted_tokens`와 `pages[].systems[].measures[].chords[]`의 개수가 다를 수 있다.
- `ChordInfo` 저장 개수는 최종 progression token 개수에 의해 결정된다.
- 이 fallback은 bbox가 있는 chord-chart 응답에서만 동작한다. measure bbox나 token bbox가 없으면 기존 measure chords 기반 처리를 유지한다.

## 검증

다음 테스트를 실행했다.

```text
./gradlew.bat test --tests "com.jazzify.backend.shared.omr.OmrClientTest" --tests "com.jazzify.backend.domain.chordproject.service.implementation.ChordProjectOmrProcessorTest"
```

결과: 성공.
