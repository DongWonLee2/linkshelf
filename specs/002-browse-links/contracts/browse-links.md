# Contract: 링크 목록 조회·태그 필터·읽음 표시 API

> **상태: 승인 대기**. 새 공개 API 2개와 기존 저장 응답의 필드 추가는 constitution I에 따라 사용자 승인이 필요하다.

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

- 정렬: `createdAt` 내림차순, 같으면 `id` 내림차순(R-LINK-09).
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

- `field`는 `page` / `size` / `tag` 중 하나다. 범위·공백·길이 오류는 한 번에 모두 담는다.
- 숫자가 아닌 값(`page=abc`)은 그 항목 하나만 담는다. 타입 변환이 검증보다 먼저 실패하기 때문이다([research R4](../research.md#r4-쪽-나누기-값-검증과-오류-모양)).

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

## 상태 코드 ↔ 요구사항

| 경로 | 상태 | 경우 | R-ID |
|---|---|---|---|
| GET /api/links | 200 | 목록·거른 목록·빈 목록 | R-LINK-09, 10, 12, 13, 16 |
| GET /api/links | 400 | page·size·tag 규칙 위반 | R-LINK-11, 14 |
| PUT /api/links/{id}/read | 200 | 지정 성공(반복 포함) | R-LINK-15, 16 |
| PUT /api/links/{id}/read | 400 | read 값 오류, 깨진 JSON, 숫자가 아닌 id | R-LINK-17 |
| PUT /api/links/{id}/read | 404 | 없는 링크 | R-LINK-17 |
| POST /api/links | 201 | 응답에 `read: false` 추가 | R-LINK-16, 18 |
