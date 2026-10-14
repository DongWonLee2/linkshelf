# Data Model: 링크 저장 (001-save-link)

> **상태: 승인됨 (2026-10-14)**. 사용자 답변 "A2·A3 승인" (이유 미기재). 변경하려면 다시 승인을 받는다(constitution I).

## 엔티티

### Link

| 필드 | 의미 | 규칙 (R-ID) |
|---|---|---|
| id | 식별자 | DB가 발급하는 정수 |
| normalizedUrl | 정규화된 URL. 저장·응답·중복 판정에 모두 이 값을 쓴다 | 필수. http/https만, host 필수, ≤ 2,048 코드 포인트 (R-LINK-02). 시스템 전체에서 유일 (R-LINK-04) |
| memo | 메모 | 선택. 앞뒤 공백 제거, 공백뿐이면 null. ≤ 500 코드 포인트 (R-LINK-03, 08) |
| tags | 태그 목록, 순서 있음 | 0~10개. 각 태그는 앞뒤 공백 제거 후 1~30 코드 포인트. 대소문자를 무시하고 합친 뒤 첫 표기를 남긴다 (R-LINK-05, 08) |
| createdAt | 저장 시각 (UTC) | 저장할 때 시스템이 정한다 |

- 상태 전이는 없다(이번 기능은 생성만 다룸).
- 원본 URL(정규화 전)은 저장하지 않는다. 필요해지면 별도 승인을 받는다.

## 테이블 초안 (H2, JPA 스키마 자동 생성. ADR-0001)

컬럼 크기는 "코드 포인트 한도 × 2"(UTF-16 단위)로 잡는다. 근거는 [research R1](research.md#r1-길이-세는-단위와-db-컬럼-크기-r-link-08-020305)이다.

```text
link
├── id              BIGINT        PK, IDENTITY
├── normalized_url  VARCHAR(4096) NOT NULL   ── CONSTRAINT uk_link_normalized_url UNIQUE
├── memo            VARCHAR(1000) NULL
└── created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL

link_tag                         (JPA @ElementCollection + @OrderColumn)
├── link_id         BIGINT        NOT NULL  FK → link.id
├── position        INTEGER       NOT NULL  (0부터, 입력 순서)
├── name            VARCHAR(60)   NOT NULL
└── PK (link_id, position)
```

| 결정 | 이유 | 대안 |
|---|---|---|
| 태그를 별도 테이블(`link_tag`)에 둠 | 다음 기능 PRD-03(태그 필터)에서 태그로 조회해야 함 | 한 컬럼에 구분자로 이어 저장: 필터가 어렵고 구분자 이스케이프 문제 |
| 태그 중복은 앱에서만 막음 | 대소문자 무시 고유 제약은 함수 인덱스가 필요해 H2/JPA 자동 생성으로 표현하기 어려움. 입력은 이 API 하나뿐 | `(link_id, lower(name))` 고유 인덱스: 마이그레이션 도구 도입(ADR-0001에서 미룸) 때 검토 |
| 고유 제약 이름을 명시(`uk_link_normalized_url`) | 409 변환 때 이 제약 위반만 골라내기 위해 (research R3) | 이름 없는 제약: 자동 생성 이름이라 판정할 수 없음 |
| `normalized_url` 하나만 저장 | 사용자 결정 Q2 "저장도 맞춘 형태로" | 원본·정규화 두 컬럼: 쓸 곳이 아직 없음 |
