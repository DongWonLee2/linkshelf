# Contract: 링크 목록 조회·태그 필터·읽음 표시 API

> **상태: 승인됨 (2026-10-14)**. 사용자 답변 "추천으로 가" (B1~B5 승인, 이유 미기재). 변경하려면 다시 승인을 받는다(constitution I).
>
> **개정 (2026-10-21, spec 004 Issue #43)**: `GET /api/links`에 `order` 추가. 사용자 승인 "1) 승인"(이유 미기재). 근거는 [spec 004 research R1](../../004-sort-links/research.md).
>
> **개정 (2026-10-22, spec 005 Issue #49)**: `GET /api/links`에 `unread` 추가. 사용자 승인 "승인"(이유 미기재). 근거는 [spec 005 research R1](../../005-unread-filter/research.md), 승인 기록은 [spec 005 contract](../../005-unread-filter/contracts/unread-filter.md).

오류 응답 형식은 [001 contract](../001-save-link/contracts/save-link.md)와 같다(`application/problem+json`, `type` 없음, 입력 오류는 `errors: [{field, message}]`).

## 기존 응답 변경: 링크 표현에 `read` 추가

`POST /api/links` 201 응답과 아래 모든 링크 항목에 `read`가 더해진다. 필드 추가만 있고 기존 필드는 그대로다(R-LINK-16, 18).

```json
{
  "id": 1,
  "url": "https://example.com/articles/1",
  "memo": "주말에 읽기 😀",
  "tags": ["Java", "스프링"],
  "createdAt": "2026-10-14T03:00:00.123456Z",
  "read": false
}
```

## `GET /api/links` — 목록 조회와 태그 필터

| 쿼리 | 타입 | 기본값 | 규칙 (R-ID) |
|---|---|---|---|
| page | 정수 | 0 | 0 이상 (R-LINK-10, 11) |
| size | 정수 | 20 | 1~100 (R-LINK-10, 11) |
| tag | 문자열 | 없음(거르지 않음) | 앞뒤 공백 제거 후 1~30 코드 포인트. 대소문자 무시, 태그 전체 일치 (R-LINK-12~14) |
| order | 문자열 | 없음(= `newest`) | `newest` 또는 `oldest`, 정확히 같을 때만. 대소문자·앞뒤 공백을 고치지 않는다. 빈 값(`order=`)은 400 (R-LINK-19, 21) |
| unread | 문자열 | 없음(= `false`, 거르지 않음) | `true` 또는 `false`, 정확히 같을 때만. 대소문자·앞뒤 공백을 고치지 않는다. `yes`·`on`·`1` 같은 다른 참·거짓 표기와 빈 값(`unread=`)은 400 (R-LINK-22, 24) |

### 200 OK

```json
{
  "items": [
    { "id": 3, "url": "https://example.com/c", "memo": null, "tags": ["java", "스프링"], "createdAt": "2026-10-14T03:00:02Z", "read": false },
    { "id": 1, "url": "https://example.com/a", "memo": "메모", "tags": ["Java"], "createdAt": "2026-10-14T03:00:00Z", "read": true }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 2,
  "totalPages": 1,
  "hasNext": false
}
```

- 정렬: `order` 생략 또는 `newest`면 `createdAt` 내림차순, 같으면 `id` 내림차순(R-LINK-09). `oldest`면 `createdAt` 오름차순, 같으면 `id` 오름차순(R-LINK-19).
- `tag`와 함께 쓰면 거른 결과를 정렬한 뒤 나눈다. `totalElements`·`totalPages`는 정렬과 관계없다(R-LINK-20).
- `unread=true`면 `read`가 `false`인 링크만 담는다. `tag`·`order`와 함께 쓰면 두 조건으로 거른 결과를 정렬한 뒤 나눈다. `totalElements`·`totalPages`는 거른 결과만 센다(R-LINK-23). `unread` 생략·`false` 응답은 개정 전과 같다(R-LINK-22).
- 링크가 없거나, 거른 결과가 없거나, 마지막 쪽 다음을 요청하면 `items: []`로 200이다. 링크가 하나도 없을 때 `totalPages`는 0이다.

### 400 Bad Request

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request parameters.",
  "instance": "/api/links",
  "errors": [
    { "field": "page", "message": "0 이상이어야 합니다" },
    { "field": "size", "message": "1 이상 100 이하여야 합니다" }
  ]
}
```

- `field`는 `page` / `size` / `tag` / `order` / `unread` 중 하나다. 범위·공백·길이·`order`·`unread` 값 오류는 한 번에 모두 담는다. `errors` 항목의 순서는 정하지 않는다.
- `order` 메시지는 `newest 또는 oldest여야 합니다`다. 두 번 보내면(`order=newest&order=oldest`) 값이 합쳐져 400이다(`tag`도 같다).
- `unread` 메시지는 `true 또는 false여야 합니다`다. 두 번 보내면(`unread=true&unread=false`) 400이다(R-LINK-24).
- 호환성(2026-10-21 개정): 개정 전에는 무시되던 `order=<아무 값>`이 400이 된다. 쓰는 곳은 linkshelf 화면뿐이다.
- 호환성(2026-10-22 개정): 개정 전에는 무시되던 `unread=<아무 값>`이 `true`면 거르고, `true`·`false`가 아니면 400이 된다. 쓰는 곳은 linkshelf 화면뿐이다.
- 숫자가 아닌 값(`page=abc`)은 그 항목 하나만 담는다. 타입 변환이 검증보다 먼저 실패하기 때문이다([research R4](../research.md#r4-쪽-나누기-값-검증과-오류-모양)). 승인 B5.
- `detail` 문구 `Invalid request parameters.`는 Spring 기본값이 아니라 직접 지정하고 테스트로 고정한다.
- `tag`를 생략하면 거르지 않는다(200). 빈 문자열이나 공백만 보내면 400이다.

## `PUT /api/links/{id}/read` — 읽음 지정

요청 `Content-Type: application/json; charset=UTF-8`

```json
{ "read": true }
```

| 필드 | 타입 | 필수 | 규칙 (R-ID) |
|---|---|---|---|
| read | boolean | O | `true` 또는 `false`만. 없음·`null`·문자열·숫자는 400 (R-LINK-17) |

### 200 OK

바뀐 링크 표현 하나(위 "링크 표현"과 같은 모양). 같은 요청을 반복해도 같은 응답이다(R-LINK-15).

### 400 Bad Request

`errors: [{ "field": "read", "message": "..." }]`. 깨진 JSON은 `errors` 없이 400.

### 404 Not Found

```json
{
  "title": "Not Found",
  "status": 404,
  "detail": "링크를 찾을 수 없습니다",
  "instance": "/api/links/999/read",
  "id": 999
}
```

- `id`가 숫자가 아니면 400(`field: id`).
- 없는 id와 잘못된 본문이 함께 오면 400이 먼저다(본문 검증이 조회보다 먼저 돈다).
- `Content-Type`이 JSON이 아니면 415(Spring 기본).

## 문서 동기화

- B3 적용 PR(US3)에서 [001 contract](../../001-save-link/contracts/save-link.md)의 201 예시에 `read`를 더하고 이 문서를 참조하는 개정 표기를 단다.
- 응답 record 구성요소 이름은 `read`로 한다(`isRead`로 지으면 JSON 키도 `isRead`가 된다).

## 상태 코드 ↔ 요구사항

| 경로 | 상태 | 경우 | R-ID |
|---|---|---|---|
| GET /api/links | 200 | 목록·거른 목록·빈 목록, `order` 생략·`newest`·`oldest` | R-LINK-09, 10, 12, 13, 16, 19, 20 |
| GET /api/links | 400 | page·size·tag·order 규칙 위반 | R-LINK-11, 14, 21 |
| PUT /api/links/{id}/read | 200 | 지정 성공(반복 포함) | R-LINK-15, 16 |
| PUT /api/links/{id}/read | 400 | read 값 오류, 깨진 JSON, 숫자가 아닌 id | R-LINK-17 |
| PUT /api/links/{id}/read | 404 | 없는 링크 | R-LINK-17 |
| POST /api/links | 201 | 응답에 `read: false` 추가 | R-LINK-16, 18 |
