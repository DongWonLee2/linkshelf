# Data Model: 링크 목록 조회·태그 필터·읽음 표시 (002-browse-links)

> **상태: 승인됨 (2026-10-14)**. 사용자 답변 "추천으로 가" (B1~B5 승인, 이유 미기재). 변경하려면 다시 승인을 받는다(constitution I).

## 엔티티

### Link (spec 001에서 바뀌는 점만)

| 필드 | 의미 | 규칙 (R-ID) |
|---|---|---|
| read | 읽음 여부 | 필수 참/거짓. 새 링크와 기존 링크 모두 기본값 false(안 읽음) (R-LINK-15, 16). 읽은 시각은 남기지 않는다 (clarify Q4) |

나머지 필드(id, normalizedUrl, memo, tags, createdAt)와 규칙은 [001 data-model](../001-save-link/data-model.md) 그대로다.

### 상태 전이 (읽음)

```text
false ──지정 read=true──▶ true
true  ──지정 read=false─▶ false
같은 값 지정 ─────────────▶ 그대로 (반복해도 결과가 같다)
```

### LinkPage (응답 전용, 저장하지 않음)

| 필드 | 의미 |
|---|---|
| items | 이 쪽의 링크 목록. 정렬: createdAt 내림차순, 같으면 id 내림차순 (R-LINK-09) |
| page | 쪽 번호(0부터) |
| size | 쪽 크기(1~100, 기본 20) |
| totalElements | 조건에 맞는 전체 링크 수(태그로 걸렀으면 거른 수) |
| totalPages | 전체 쪽 수 |
| hasNext | 다음 쪽이 있는지 |

## 테이블 변경 (H2, JPA 스키마 자동 생성. ADR-0001)

```text
link
├── id              BIGINT        PK, IDENTITY                       (변경 없음)
├── normalized_url  VARCHAR(4096) NOT NULL, UNIQUE                   (변경 없음)
├── memo            VARCHAR(1000) NULL                               (변경 없음)
├── created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL             (변경 없음)
└── is_read         BOOLEAN       DEFAULT FALSE NOT NULL             ← 추가

link_tag (변경 없음)
```

- 기존 행은 기본값으로 false가 된다. 기본값 없이 NOT NULL로 더하면 기존 데이터가 있는 로컬 DB에서 실패한다(실측, [research R1](research.md#r1-기존-로컬-db에-읽음-컬럼을-더하는-방법)).
- 인덱스는 더하지 않는다. 1인·수천 건 규모(PRD §6)라 정렬·태그 비교 성능 목표가 없다.
- 컬럼 이름 `is_read`: 다른 DB의 예약어 `read`와 충돌을 피한다.
