# Research: 링크 목록 조회·태그 필터·읽음 표시

기준 SHA `c2c2bc3` · 2026-10-14

## R1. 기존 로컬 DB에 읽음 컬럼을 더하는 방법

- **Decision**: `is_read BOOLEAN DEFAULT FALSE NOT NULL`. 엔티티에서는 Hibernate `@ColumnDefault("false")`와 `nullable = false`를 쓴다. 기존 `ddl-auto=update`(ADR-0001)가 컬럼을 더한다.
- **Rationale**: 로컬 파일 DB 복사본(행 2개)으로 H2 2.4.240에서 직접 시험했다(원본 미변경).
  | 문장 | 결과 |
  |---|---|
  | `alter table link add column is_read boolean not null` | **실패**: `NULL not allowed for column "IS_READ"` |
  | `alter table link add column is_read boolean default false not null` | 성공, 기존 2행 모두 false |
  | `alter table link add column is_read boolean` (null 허용) | 성공, 기존 행 null |
  - 기본값 없이 NOT NULL로 두면 기존 데이터가 있는 로컬 DB에서 앱이 뜨지 않는다. 테스트는 매번 빈 메모리 DB(`create-drop`)라 이 실패를 잡지 못한다 → SC-004는 실제 기동으로 확인한다.
  - Hibernate가 `update`에서 `@ColumnDefault`를 ALTER 문에 넣는지는 미확인이다. US3 구현 때 로컬 DB 복사본을 대상으로 기동해 확인한다(quickstart §4).
- **Alternatives considered**:
  - null 허용 + 앱에서 null을 안 읽음으로 취급: 3가지 상태(null/true/false)가 생겨 규칙이 늘어난다.
  - 로컬 DB 파일을 지우고 새로 만들기: 데이터 삭제라 사용자 승인이 필요하고 되돌릴 수 없다.
  - Flyway 같은 마이그레이션 도구: 새 의존성. ADR-0001이 MVP는 JPA 자동 생성으로 정했다.
- 컬럼 이름: H2에서 `read`도 쓸 수 있었지만(시험함), 다른 DB의 예약어 충돌을 피하려고 `is_read`로 한다.

## R2. 태그 일치 비교를 어디서 하나

- **Decision**: 요청 태그를 앱에서 `strip()` + `toLowerCase(Locale.ROOT)`로 바꾸고, DB에서는 `lower(t) = :tag`로 비교한다. 저장된 태그는 이미 앞뒤 공백이 제거돼 있다(R-LINK-05). 중복 행이 생기지 않게 `exists` 하위 질의를 쓴다.
- **Rationale**: 저장 쪽 `TagNormalizer`가 `strip()` + `toLowerCase(Locale.ROOT)`로 같은 태그를 판정한다. 같은 기준을 써야 R-LINK-13이 저장 규칙과 맞는다. H2 `LOWER`와 Java `toLowerCase(Locale.ROOT)`는 ASCII·한글에서 같다. 특수 문자(터키어 İ 등)의 차이는 1인 사용 범위에서 받아들인다.
- **Alternatives considered**: 태그를 소문자 열로 따로 저장 → 스키마 변경이 커진다. 조인 후 `distinct` → 쪽 나누기 개수 계산이 복잡해진다.

## R3. 목록에서 태그를 불러오는 방식 (OSIV 꺼짐)

- **Decision**: 서비스 트랜잭션 안에서 응답 DTO로 바꾼다(#5에서 OSIV를 껐음). 태그 컬렉션에 Hibernate `@BatchSize(size = 100)`를 붙여 쪽 하나(최대 100개)의 태그를 한 번에 불러온다.
- **Rationale**: 컬렉션 fetch join과 쪽 나누기를 함께 쓰면 Hibernate가 메모리에서 쪽을 나눈다(경고 HHH90003004). 배치 로딩은 쿼리를 쪽당 2~3개로 줄이고 계약을 바꾸지 않는다.
- **Alternatives considered**: 기본 지연 로딩(N+1, 쪽당 최대 101개 쿼리) — 수천 건 규모라 동작은 하지만 피할 수 있다. `@EntityGraph` — 위 경고.

## R4. 쪽 나누기 값 검증과 오류 모양

- **Decision**: `page`, `size`, `tag`를 컨트롤러 `@RequestParam`으로 받고 `@Min`·`@Max`·`@MaxCodePoints`·`@NotBlank`를 붙인다. Spring의 메서드 검증(`HandlerMethodValidationException`)을 `ApiExceptionHandler`에서 기존 400 형식(`errors: [{field, message}]`)으로 바꾼다. 숫자가 아닌 값(`MethodArgumentTypeMismatchException`)도 같은 형식으로 바꾼다.
- **Rationale**: 저장 API의 입력 오류 형식(R-LINK-06)과 같아야 사용자가 하나의 규칙으로 처리한다(clarify Q2).
- **한계**: 타입 변환 실패는 검증보다 먼저 일어나므로, `page=abc&size=0`처럼 두 가지가 함께 틀리면 타입 오류 항목만 알려 준다. spec R-LINK-11 "모두 알려 준다"와 어긋나는 유일한 경우라 plan에 적고 테스트로 고정한다.

## R5. 읽음 값의 엄격한 해석

- **Decision**: 요청 본문 `{"read": true|false}`. 값이 없거나 `null`이면 400(`@NotNull`). 문자열 `"true"`나 숫자 `1`처럼 참/거짓이 아닌 값도 400이어야 한다(R-LINK-17).
- **미확인**: Spring Boot 4.1.1의 Jackson 3 기본 설정이 `"true"`·`1`을 boolean으로 바꿔 받아들이는지. US3 Red 테스트로 먼저 확인하고, 받아들이면 이 필드에만 엄격한 해석을 적용한다(전역 설정은 저장 API에 영향을 주므로 바꾸지 않는다).

## R6. 엔드포인트 모양

- **Decision**: 목록과 태그 필터는 `GET /api/links?page&size&tag`. 읽음 지정은 `PUT /api/links/{id}/read`, 본문 `{"read": boolean}`.
- **Rationale**: 같은 요청을 반복해도 결과가 같아야 한다(clarify Q4) → 뒤집기(PATCH toggle)가 아닌 상태 지정. 읽음만 바꾸는 하위 자원으로 두면 링크 전체 수정(범위 밖)과 섞이지 않는다.
- **Alternatives considered**: `PATCH /api/links/{id}` 본문 `{"read": true}` — 나중에 수정 기능이 생기면 같은 경로를 써야 해서 지금 범위를 정하기 어렵다. `POST /api/links/{id}/read` + `DELETE` — 읽음/안 읽음이 다른 메서드로 나뉜다.
