# Data Model: 링크 목록 정렬 옵션

DB 스키마와 저장 데이터는 바뀌지 않는다. 링크 엔티티는 spec 002 [data-model](../002-browse-links/data-model.md) 그대로다.

## 정렬 방향 (요청 조건, 저장하지 않음)

| 값(API) | 내부 | 정렬 | R-ID |
|---|---|---|---|
| 생략, `newest` | `LinkOrder.NEWEST` | `createdAt desc, id desc` | R-LINK-09, 19 |
| `oldest` | `LinkOrder.OLDEST` | `createdAt asc, id asc` | R-LINK-19 |
| 그 밖(빈 값 포함) | — | 400 `field=order` | R-LINK-21 |

## 화면 상태 (spec 003에 더함)

| 상태 | 기본값 | 바뀌는 때 | 유지되는 때 |
|---|---|---|---|
| `order` | `newest` | 선택 상자 변경(R-UI-19), 저장 성공 → `newest`(R-UI-20), 새로 고침 → `newest`(R-UI-18) | 쪽 넘김, 필터 적용·해제(R-UI-19) |
