# Data Model: 안 읽은 것만 보기 필터

DB 스키마와 저장 데이터는 바뀌지 않는다. 링크 엔티티와 읽음 여부(`is_read`)는 spec 002 [data-model](../002-browse-links/data-model.md) 그대로다.

## 안 읽음 조건 (요청 조건, 저장하지 않음)

| 값(API) | 내부 | 거르기 | R-ID |
|---|---|---|---|
| 생략, `false` | `unreadOnly = false` | 없음(기존 응답 그대로) | R-LINK-22 |
| `true` | `unreadOnly = true` | `read = false`인 링크만 | R-LINK-22, 23 |
| 그 밖(빈 값, `yes`·`on`·`1`, 대소문자·공백 다름, 두 번 보냄) | — | 400 `field=unread` | R-LINK-24 |

## 조건 조합 (R-LINK-23)

| 태그 | 안 읽음 | 저장소 경로 | 전체 개수 |
|---|---|---|---|
| 없음 | 끔 | `findPage`(기존) | 전체 |
| 있음 | 끔 | `findPageFiltered(tag, false)` | 태그 일치 |
| 없음 | 켬 | `findPageFiltered(null, true)` | 안 읽음 |
| 있음 | 켬 | `findPageFiltered(tag, true)` | 태그 일치 ∩ 안 읽음 |

정렬은 모든 경로에서 `Sort` 인자(R-LINK-19)로 같다.

## 화면 상태 (spec 003·004에 더함)

| 상태 | 기본값 | 바뀌는 때 | 유지되는 때 |
|---|---|---|---|
| `unread` | `false` | 체크박스(R-UI-22), 저장 성공 → `false`(R-UI-23), 새로 고침 → `false`(R-UI-21) | 쪽 넘김, 태그 적용·해제, 정렬 변경(R-UI-22) |
| `requested`(ref, T003에서 `requestedTag`를 바꿈) | `{ tag: null, order: 'newest', unread: false }` | 목록 요청을 보낼 때마다. 실패하면 `tag`만 화면에 적용된 값으로 돌아감(정렬·안 읽음은 유지, R-UI-24) | — |

새 요청은 바뀌지 않는 조건을 `requested*`에서 읽는다(R-UI-24, research R4).
