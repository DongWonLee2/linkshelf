# Contract: 링크 저장 API

> **상태: 승인됨 (2026-10-14)**. 사용자 답변 "A2·A3 승인" (이유 미기재). 변경하려면 다시 승인을 받는다(constitution I).
>
> **개정 (2026-10-14, spec 002 승인 B3 "추천으로 가")**: 201 응답에 `read`(boolean)를 더했다. 기존 필드는 그대로다.
>
> **개정 (2026-10-14, 사용자 승인 "계약 문서 수정")**: 400/409 응답에서 `type` 필드를 뺐다. Spring은 기본값 `about:blank`이면 `type`을 응답하지 않고, RFC 9457 §3.1.1에 따라 `type`이 없으면 `about:blank`로 간주한다. converge(T049)에서 실제 응답과 다른 점으로 발견됐다.

## `POST /api/links`

요청 `Content-Type: application/json; charset=UTF-8`

```json
{
  "url": "HTTPS://Example.COM:443/articles/1",
  "memo": "주말에 읽기 😀",
  "tags": ["Java", " java ", "스프링"]
}
```

| 필드 | 타입 | 필수 | 규칙 |
|---|---|---|---|
| url | string | O | R-LINK-02, 04 |
| memo | string \| null | X | R-LINK-03. 없거나 공백뿐이면 null |
| tags | string[] \| null | X | R-LINK-05. 없으면 `[]` |

### 201 Created

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

- `url`은 정규화된 값이다.
- `read`는 spec 002에서 더해졌다. 새 링크는 항상 `false`다([002 contract](../../002-browse-links/contracts/browse-links.md)).
- `Location` 헤더는 넣지 않는다. 단건 조회 API가 아직 없기 때문이며, PRD-02 때 검토한다.

### 400 Bad Request: 입력 오류 (`application/problem+json`)

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request content.",
  "instance": "/api/links",
  "errors": [
    { "field": "url", "message": "http 또는 https URL이어야 합니다" },
    { "field": "memo", "message": "500자 이하여야 합니다" }
  ]
}
```

- `type`은 응답에 없다(= `about:blank`, RFC 9457 §3.1.1). 409도 같다.
- 틀린 항목을 모두 한 번에 담는다. `field`는 `url` / `memo` / `tags` 중 하나다.
- 깨진 JSON은 `errors` 없이 400을 돌려준다.

### 409 Conflict: 중복 URL (`application/problem+json`)

```json
{
  "title": "Conflict",
  "status": 409,
  "detail": "이미 저장된 URL입니다",
  "instance": "/api/links",
  "url": "https://example.com/articles/1"
}
```

- `url` 확장 필드는 충돌한 정규화 URL이다.
- 사전 확인으로 걸리든 DB 고유 제약으로 걸리든 응답이 같다.

## 상태 코드 ↔ 요구사항

| 상태 | 경우 | R-ID |
|---|---|---|
| 201 | 저장 성공 | R-LINK-01, 07 |
| 400 | URL·메모·태그 규칙 위반, 깨진 JSON | R-LINK-02, 03, 05, 06, 08 |
| 409 | 정규화 URL이 이미 있음 | R-LINK-04, 06 |
