# Implementation Plan: 링크 목록 조회·태그 필터·읽음 표시

**Branch**: spec `chore/spec-002-browse-links` → 구현은 스토리별 `feat/<issue>-…` 3개 | **Date**: 2026-10-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/002-browse-links/spec.md` · 기준 SHA `c2c2bc3`

**상태**: 승인됨 (2026-10-14). 사용자 답변 "추천으로 가" (B1~B5 승인, 이유 미기재). 아래 [승인 요청](#승인-요청-완료)을 본다.

## Summary

- **US1 목록**: `GET /api/links?page&size`가 링크를 `createdAt desc, id desc`로 쪽 나눠 돌려준다. 응답은 `{items, page, size, totalElements, totalPages, hasNext}`. 잘못된 `page`·`size`는 400(저장 API와 같은 `errors` 형식).
- **US2 태그 필터**: 같은 GET에 `tag`를 더한다. 요청 태그를 `strip` + `toLowerCase(Locale.ROOT)` 한 뒤 `exists (… lower(t) = :tag)`로 거른다. 저장 규칙(`TagNormalizer`)과 같은 기준이다.
- **US3 읽음**: `link.is_read BOOLEAN DEFAULT FALSE NOT NULL` 추가. `PUT /api/links/{id}/read` `{"read": bool}`로 상태를 지정한다(반복해도 같은 결과). 모든 링크 표현에 `read`가 더해진다.

## Technical Context

**Language/Version**: Java 21 (ADR-0001)

**Primary Dependencies**: Spring Boot 4.1.1 — webmvc, data-jpa, validation. **새 의존성 없음** (`@ColumnDefault`, `@BatchSize`는 이미 있는 Hibernate 7.4.5 어노테이션)

**Storage**: H2 2.4.240. 개발 파일 DB(`ddl-auto=update`), 테스트 메모리 DB(`create-drop`). 테이블 변경은 컬럼 1개 추가

**Testing**: JUnit 5, MockMvc, `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest` (ADR-0003)

**Target Platform**: 로컬 JVM (macOS), CI GitHub Actions ubuntu

**Project Type**: web-service (백엔드 API만)

**Performance Goals**: 쪽 하나 1초 이내(SC-005, 수천 건). 별도 성능 테스트 없음

**Constraints**: OSIV 꺼짐(#5) → 응답 DTO는 서비스 트랜잭션 안에서 만든다. 외부 요청 금지, UTF-8

**Scale/Scope**: 수천 건 이하. 새 엔드포인트 2개, 기존 응답 필드 1개 추가

## Constitution Check

*GATE: Phase 0 전 / Phase 1 후 재확인*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 승인 없는 계약 변경 금지 | PASS | 새 공개 API 2개, 저장 응답 `read` 추가, 테이블 컬럼 1개 추가, R-LINK-11 예외(B5). 2026-10-14 사용자 답변 "추천으로 가" (B1~B5 승인, 이유 미기재) |
| II. 테스트 먼저 | PASS(계획) | 스토리마다 Red 커밋 후 구현. 고장 내기 검증은 quickstart §5 |
| III. 네 가지 결과 | PASS(계획) | quickstart 보고 방식 |
| IV. 2회 실패 중단 | 해당 시 적용 | — |
| V. 작은 PR | PASS | 스토리마다 PR 하나(US1 → US2 → US3) |
| VI. ID 추적 | PASS(계획) | R-LINK-09~18 → tasks → Issue → `R_LINK_NN_…` 테스트 |
| VII. 우회 금지 | PASS | — |

**Phase 1 후 재확인**: 설계에서 새 위반은 없다. I은 2026-10-14 사용자 승인으로 PASS.

## 핵심 설계 결정

| # | 결정 | 근거 |
|---|---|---|
| 1 | 읽음 컬럼은 `DEFAULT FALSE NOT NULL` | 기본값 없는 NOT NULL은 기존 데이터에서 실패(실측). [research R1](research.md) |
| 2 | 태그 비교는 앱에서 소문자화 + DB `lower()` + `exists` | 저장 규칙과 같은 기준, 중복 행 없음. [R2](research.md) |
| 3 | 태그 컬렉션 `@BatchSize(100)` | OSIV 꺼진 상태에서 N+1 방지, fetch join + 쪽 나누기 경고 회피. [R3](research.md) |
| 4 | 쿼리 파라미터 검증 오류를 기존 400 `errors` 형식으로 | 입력 오류 형식 하나로 통일. 숫자가 아닌 값은 그 항목만 담는 한계 있음. [R4](research.md) |
| 5 | `PUT /api/links/{id}/read` 상태 지정 | 반복해도 같은 결과(clarify Q4), 링크 수정(범위 밖)과 분리. [R6](research.md) |
| 6 | `read` 엄격 해석은 Red 테스트로 먼저 확인 | Jackson 3 기본 강제 변환 미확인. 전역 설정은 바꾸지 않는다. [R5](research.md) |
| 7 | 스토리 순서대로, 병렬 없음 | 세 스토리가 `LinkController`·`LinkService`·`LinkResponse`를 함께 고친다(advisor 판단) |

## 테스트 계획

### 웹 슬라이스 (`@WebMvcTest`, 서비스 `@MockitoBean`)

- US1: `tag` 생략 → 200(거르지 않음), `page`·`size` 기본값 전달, 경계(`size` 1·100 허용, 0·101 거부, `page` -1 거부), 숫자가 아닌 값 400과 `errors[field]`, 응답 JSON 모양(6개 필드).
- US2: `tag` 공백만 → 400, 31 코드 포인트 → 400, 30 → 허용.
- US3: `read` 없음·`null`·`"true"`·`1` → 400, 숫자가 아닌 id → 400, 없는 id → 404 모양.

### JPA 슬라이스 (`@DataJpaTest`, H2 메모리)

- US1: 같은 `createdAt` 링크의 id 내림차순, 쪽 경계에서 겹침·빠짐 없음.
- US2: `Java`/`java`/`JavaScript`/`스프링` 데이터로 전체 일치·대소문자 무시·부분 불일치, 태그 여러 개 달린 링크가 한 번만 나옴, 거른 `totalElements`.
- US3: 새 링크 `read=false`, 지정 후 다시 읽어 유지.

### 통합 (`@SpringBootTest` + MockMvc, 스토리당 최소)

- US1: 저장 3개 → 목록 최신순·한글 그대로(R-LINK-09, 07 유지). 기본 크기보다 많을 때 모든 쪽을 합치면 전부(SC-001).
- US2: 저장 → `tag=%20JAVA%20`로 거른 결과(SC-002).
- US3: 저장 → 읽음 지정 2번 → 목록에서 읽음, 다른 필드 불변(SC-003). 저장 응답 `read:false`(R-LINK-16).

### 실제 기동 (US3 PR)

- quickstart §4: 기존 데이터 복사본에서 기동, 기존 링크 `read:false`(SC-004).

## Project Structure

### Documentation (this feature)

```text
specs/002-browse-links/
├── spec.md
├── plan.md            # 이 파일
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/browse-links.md
├── checklists/requirements.md
└── tasks.md           # /speckit-tasks에서 만든다
```

### Source Code (바뀌는 파일)

```text
backend/src/main/java/dev/linkshelf/
├── link/
│   ├── Link.java              # US2 @BatchSize, US3 read 필드
│   ├── LinkRepository.java    # US1 정렬 페이지 조회, US2 태그 exists 조회
│   ├── LinkService.java       # US1 list, US3 markRead
│   ├── LinkController.java    # US1·US2 GET, US3 PUT
│   ├── LinkResponse.java      # US3 read 필드
│   ├── LinkPageResponse.java  # US1 새 파일
│   ├── MarkReadRequest.java   # US3 새 파일
│   └── LinkNotFoundException.java  # US3 새 파일
└── common/ApiExceptionHandler.java # US1 쿼리 검증 오류, US3 404
backend/src/test/java/dev/linkshelf/link/  # 스토리별 테스트 추가
```

## 승인 요청 (완료)

constitution I에 따라 아래를 한 번에 승인받는다. 상세: [contracts/browse-links.md](contracts/browse-links.md), [data-model.md](data-model.md).

| # | 변경 | 종류 |
|---|---|---|
| B1 | `GET /api/links?page&size&tag` → 200 쪽 객체 / 400 | 새 공개 API |
| B2 | `PUT /api/links/{id}/read` `{"read": bool}` → 200 링크 / 400 / 404 | 새 공개 API |
| B3 | 모든 링크 표현(저장 201 포함)에 `read` 추가 | 기존 API 응답 필드 추가 |
| B4 | `link.is_read BOOLEAN DEFAULT FALSE NOT NULL` | DB 컬럼 추가 |
| B5 | `page=abc&size=0`처럼 숫자가 아닌 값과 다른 오류가 함께 오면 숫자가 아닌 항목 하나만 알려 준다(R-LINK-11 "모두 알려 준다"의 예외) | spec 요구사항 예외 |

- 되돌리기: 코드를 이전으로 돌려도 `is_read`는 기본값 덕에 INSERT가 깨지지 않는다. 컬럼까지 없애려면 US3 시작 전에 만든 로컬 DB 백업(quickstart §4-0)으로 바꾼다.
- advisor 검토(2026-10-14)로 고친 것: tag 제약(`@NotBlank` → null 허용 공백 검사), SC-004 증거 세 층, 계약 경계 동작(400 우선, 415, detail 문구), 001 계약 동기화.

## Complexity Tracking

위반 없음.
