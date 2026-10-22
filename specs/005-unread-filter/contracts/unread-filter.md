# Contract: 안 읽은 것만 보기 (browse-links 개정안)

> **상태: 승인됨 (2026-10-22, Issue #49)**. 사용자 답변 "승인"(이유 미기재). constitution I에 따라 [browse-links](../../002-browse-links/contracts/browse-links.md)에 반영한다(spec 004와 같은 방식: 본문은 browse-links 한 곳, 이 파일은 승인 기록과 링크). 결정 근거는 공통 grilling Q7·Q8과 [research R1](../research.md).

## browse-links에 반영할 내용

1. 머리 개정 표기: `**개정 (2026-10-22, spec 005 Issue #49)**: GET /api/links에 unread 추가. 사용자 승인 "승인"`.
2. 쿼리 표에 행 추가:

| 쿼리 | 타입 | 기본값 | 규칙 (R-ID) |
|---|---|---|---|
| unread | 문자열 | 없음(= `false`, 거르지 않음) | `true` 또는 `false`, 정확히 같을 때만. 대소문자·앞뒤 공백을 고치지 않는다. `yes`·`on`·`1` 같은 다른 참·거짓 표기와 빈 값(`unread=`)은 400 (R-LINK-22, 24) |

3. 200 설명에 추가: `unread=true`면 `read`가 `false`인 링크만 담는다. `tag`·`order`와 함께 쓰면 두 조건으로 거른 결과를 정렬한 뒤 나눈다. `totalElements`·`totalPages`는 거른 결과만 센다(R-LINK-23). `unread` 생략·`false` 응답은 개정 전과 같다.
4. 400 설명 바꿈: `field`는 `page` / `size` / `tag` / `order` / `unread` 중 하나. `unread` 메시지는 `true 또는 false여야 합니다`. 두 번 보내면 400(`tag`·`order`와 같다).
5. 호환성: 개정 전에는 무시되던 `unread=<아무 값>`이 `true`면 거르고, `true`·`false`가 아니면 400이 된다. 쓰는 곳은 linkshelf 화면뿐이다.

## 화면 계약 (features/README 표에 먼저 넣을 줄)

| PRD | 화면 요소 | `data-testid` | API | E2E 테스트 이름 |
|---|---|---|---|---|
| PRD-06 안 읽음 필터 | "안 읽은 것만" 체크박스(기본 꺼짐, 새로 고치면 꺼짐) | `link-unread-only-toggle` | `GET /api/links?unread=` | `E2E_R_UI_22_toggle_reloads_first_page_keeping_tag_and_order` |

| 바뀐 것 | R-ID |
|---|---|
| `unread` 쿼리(`true`·`false`, 생략 = `false`) | R-LINK-22 |
| `tag`·`order`·쪽 나누기와 함께 동작 | R-LINK-23 |
| 잘못된 값 400 `field=unread`, 다른 오류와 함께 | R-LINK-24 |
| 체크박스 `link-unread-only-toggle` | R-UI-21 |
