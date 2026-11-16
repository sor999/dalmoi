# 달모이(Dalmoi) 디자인 가이드

[디자인 보기](https://www.figma.com/design/nJh1EjT7VOKrswZ6zLV3Pw/Dalmoi?node-id=1-1259)

## 1. 디자인 방향

- **검정과 라임**: 검정 버튼·강조 카드와 라임 텍스트·선택 상태를 중심으로 구성한다.
- **따뜻한 배경**: `#F7F6F2` 바탕에 밝은 카드, 가는 경계선, 둥근 모서리를 사용한다.
- **숫자 중심의 러닝 경험**: 거리·경과시간을 크게 보여주고 단위·보조 설명은 작게 구분한다.
- **상태가 보이는 UI**: 오류·선택·지연 상태는 색과 함께 문구·형태로 전달한다.
- **러닝 중 전용 어두운 표현**: 주행 화면은 어두운 배경에 라임 숫자와 경로를 사용한다. 앱 전체 다크 모드가 확정된 것은 아니다.

## 2. 색상 토큰

### 2.1 팔레트

| 이름 | 토큰 | 값 | 용도 |
| --- | --- | --- | --- |
| primary | color.primary | `#141414` | 주요 텍스트, 검정 버튼·강조 면 |
| accent | color.accent | `#C8F03A` | 라임 강조, 주요 수치·선택·버튼 문구 |
| background | color.background | `#F7F6F2` | 일반 화면 배경 |
| routePlanned | color.route.planned | `#5C7A00`, opacity `0.40` | 계획 경로 |
| error | color.error | `#D9472B` | 오류·종료 확인의 경고 행동 |
| me | color.marker.me | `#2F6BFF` | 본인 위치 색 |
| p1 | color.marker.peer1 | `#E8590C` | 참가자 식별 색 1 |
| p2 | color.marker.peer2 | `#C2255C` | 참가자 식별 색 2 |
| p3 | color.marker.peer3 | `#7048E8` | 참가자 식별 색 3 |

`routePlanned`는 RGB 색과 투명도를 분리해 적용한다. 웹 RGBA는 `rgba(92,122,0,0.4)`, Android ARGB는 `0x665C7A00`이다. 색상과 투명도를 개별 토큰으로 관리한다.

본인 마커의 라임/파랑 적용 기준은 추가 정의한다. `p1~p3`는 우선순위 P1/P2가 아니라 참가자 색상 이름이다.

### 2.2 의미 기반 매핑

팔레트를 역할별로 재사용하기 위한 매핑 제안이다.

| 의미 토큰 | 매핑 | 적용 |
| --- | --- | --- |
| color.text.primary | color.primary | 일반 화면 제목·본문 |
| color.action.primary.background | color.primary | 검정 주 행동 버튼 |
| color.action.primary.foreground | color.accent | 검정 버튼 위 라임 문구 |
| color.action.highlight.background | color.accent | 라임 강조 CTA |
| color.action.highlight.foreground | color.primary | 라임 면 위 검정 문구 |
| color.feedback.error | color.error | 오류 문구·배너·확인 행동 |
| color.surface.running | color.primary | 러닝 화면 어두운 기본 면 후보 |
| color.metric.running | color.accent | 러닝 중 강조 수치 |

밝은 카드·보조 텍스트·경계선·수신 지연·스크림은 `surface`, `text.secondary`, `border`, `warning`, `scrim` 토큰으로 추가 정의한다.

## 3. 타이포그래피 토큰

### 3.1 글꼴과 범위

| 토큰 | 값 |
| --- | --- |
| font.family.text | Pretendard |
| font.weight.text | 400~700 |
| font.size.text.range | 12~26sp |
| font.family.numeric | Barlow Condensed |
| font.weight.numeric | 700 |
| font.size.numeric.large | 60sp |
| font.size.numeric.medium | 34sp |

### 3.2 구현을 위한 역할 정리

역할별 크기·굵기 조합 제안이다.

| 역할 토큰 | 글꼴 | 크기 / 굵기 | 적용 |
| --- | --- | --- | --- |
| type.metric.large | Barlow Condensed | 60sp / 700 | 결과의 주요 거리 |
| type.metric.medium | Barlow Condensed | 34sp / 700 | 보조 거리·시간 |
| type.heading.large | Pretendard | 26sp / 700 | 주요 제목 |
| type.heading.screen | Pretendard | 22sp / 700 | 화면 제목 |
| type.title | Pretendard | 18sp / 700 | 카드·다이얼로그 제목 |
| type.body | Pretendard | 14sp / 400 | 본문·입력 내용 |
| type.label | Pretendard | 14sp / 600 | 버튼·라벨 |
| type.caption | Pretendard | 12sp / 400 | 날짜·상태 설명·보조 문구 |

행간과 자간은 역할별로 추가 정의한다. Android에서는 sp를 사용하고 글꼴 확대 시 잘림을 검증한다. 거리와 시간은 숫자의 위계를 유지하되 단위를 작은 별도 텍스트로 표시한다.

## 4. 간격·형태·크기 토큰

### 4.1 간격과 모서리

| 토큰 | 값 | 권장 적용 역할 |
| --- | --- | --- |
| space.1 | 4dp | 아이콘·텍스트의 작은 보정 |
| space.2 | 8dp | 밀접한 요소 사이 |
| space.3 | 12dp | 항목·보조 그룹 사이 |
| space.4 | 16dp | 카드 내부·행 간격 |
| space.5 | 20dp | 화면 좌우 여백·섹션 간격 |
| space.6 | 24dp | 큰 그룹·다이얼로그 내부 |
| radius.button | 14dp | 버튼 |
| radius.card | 18dp | 카드 |
| radius.panel | 24dp | 패널 |
| radius.dialog | 28dp | 다이얼로그 |
| size.touch.min | 48dp | 최소 터치 영역 |

기본 간격을 우선 사용하고, 필요한 예외는 용도에 맞는 공통 토큰으로 추가한다.

그림자·경계선 두께·스크림 투명도는 공통 토큰으로 정의해 일관되게 사용한다.

## 5. 공통 스타일 원칙

| 요소 | 스타일 기준 |
| --- | --- |
| 주 버튼 | 검정 배경과 라임 문구, 모서리 14dp |
| 강조 버튼 | 라임 배경과 검정 문구, 모서리 14dp |
| 보조 버튼 | 밝은 면 또는 외곽선. 주 버튼보다 낮은 강조 수준 |
| 카드 | 밝은 면과 모서리 18dp, 내용 그룹별 일정한 내부 여백 |
| 패널 | 모서리 24dp, 주요 정보와 행동의 위계를 명확히 구분 |
| 다이얼로그 | 모서리 28dp, 제목·본문·행동의 글자 위계 유지 |
| 입력란 | 항상 식별 가능한 라벨, 둥근 입력 면, 오류 문구 |
| 수치 | Barlow Condensed 700으로 강조하고 단위·설명을 작게 구분 |
| 선택 상태 | 색과 함께 외곽선·아이콘·텍스트 등 식별 요소 사용 |
| 지도 경로·마커 | 계획/실제 경로는 선 형태로 구분, 참가자는 색과 이름/이니셜로 구분 |
| 프로필 이미지 | 크기·자르기 규칙 통일. 이미지가 없거나 로딩 실패하면 기본 아바타/이니셜 |

- 색상은 원시 팔레트에서 의미 토큰으로 연결해 사용한다. 같은 역할에 화면마다 다른 값을 만들지 않는다.
- 상태·정보를 색 하나로만 전달하지 않는다. 오류·선택·지연에는 문구나 형태를 함께 제공한다.
- 작은 아이콘도 터치 영역은 최소 48dp를 확보한다.
- 화면 폭·시스템 inset·키보드·글꼴 확대에 맞춰 유연하게 배치한다.
- pressed·disabled·loading 상태는 공통 컴포넌트에서 정의한다. 정확한 상태색·elevation은 추가 확정 대상이다.

## 6. 구현용 토큰 예시

숫자의 단위는 spacing/radius/size=dp, typography=sp다. `routePlanned`는 색상과 투명도를 분리해 적용한다.

```json
{
  "color": {
    "primary": "#141414",
    "accent": "#C8F03A",
    "background": "#F7F6F2",
    "routePlanned": { "hex": "#5C7A00", "opacity": 0.4 },
    "error": "#D9472B",
    "marker": {
      "me": "#2F6BFF",
      "peer1": "#E8590C",
      "peer2": "#C2255C",
      "peer3": "#7048E8"
    }
  },
  "font": {
    "text": { "family": "Pretendard", "weights": [400, 500, 600, 700] },
    "numeric": { "family": "Barlow Condensed", "weight": 700, "sizes": [34, 60] }
  },
  "spacing": [4, 8, 12, 16, 20, 24],
  "radius": { "button": 14, "card": 18, "panel": 24, "dialog": 28 },
  "size": { "minTouchTarget": 48 }
}
```

Android에서는 원시 팔레트와 의미 토큰을 분리하고 공통 theme·typography·shape·dimension 정의에서 재사용한다. 수치가 같아도 오류와 참가자 색처럼 의미가 다른 토큰은 별도로 유지한다. 텍스트 굵기는 400·500·600·700 단계를 사용한다.

## 7. 보완할 토큰

| 항목 | 추가 정의할 내용 |
| --- | --- |
| 색상 | surface, text.secondary, border, warning, scrim 및 상태별 색상 |
| 지도 | 본인 마커 라임/파랑 정책, 경로 두께·dash, 참가자 식별 규칙 |
| 타이포그래피 | line-height, letter-spacing, 역할별 크기·굵기 최종 매핑 |
| 효과 | 그림자·elevation·경계선 두께 |
| 상호작용 | pressed·disabled·loading 표현, 전환 시간·easing |

새 값을 추가할 때는 용도와 이름을 먼저 정하고 공통 토큰으로 관리한다. 글꼴 확대, 텍스트 대비, 최소 터치 영역, 색 이외의 상태 구별도 함께 확인한다.
