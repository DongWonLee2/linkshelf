# Implementation Plan: 링크 저장

**Branch**: `feat/3-save-link` (Issue #3) | **Date**: 2026-10-14 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/001-save-link/spec.md` · 기준 SHA `8c26558`

**상태**: 승인됨 (2026-10-14). API 계약·테이블 구조 승인, 한글 도메인은 거부. 아래 [승인 요청](#승인-요청-완료)을 본다.

## Summary

`POST /api/links`로 URL·메모·태그를 받아 저장한다. 처리 순서는 다음과 같다.
1. 커스텀 Bean Validation 제약이 모든 입력 오류를 한 번에 모아 400 ProblemDetail로 돌려준다. 길이는 코드 포인트로 센다.
2. URL은 표준 동치 범위로 정규화한다.
3. 같은 정규화 URL이 있는지 먼저 확인한다. 동시 요청은 DB 고유 제약 `uk_link_normalized_url`이 막고, 그 위반을 409로 바꾼다.
4. 태그는 공백을 지우고 대소문자를 무시해 합친다. 첫 표기를 남긴다.

시퀀스: [diagrams/save-link-sequence.html](diagrams/save-link-sequence.html)

## Technical Context

**Language/Version**: Java 21 (ADR-0001)

**Primary Dependencies**: Spring Boot 4.1.1 — webmvc, data-jpa, validation (이미 `build.gradle`에 있음). **새 의존성 없음**

**Storage**: H2 2.4.240. 개발은 파일 DB, 테스트는 메모리 DB. JPA 스키마 자동 생성 (ADR-0001)

**Testing**: JUnit 5, MockMvc, `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest` (ADR-0003)

**Target Platform**: 로컬 JVM (macOS), CI는 GitHub Actions ubuntu

**Project Type**: web-service (백엔드 API만)

**Performance Goals**: 없음 (PRD §6)

**Constraints**: 외부 네트워크 요청 금지(URL 접속 확인 안 함), UTF-8, 1인 사용

**Scale/Scope**: 수천 건 이하, 엔드포인트 1개

## Constitution Check

*GATE: Phase 0 전 / Phase 1 후 재확인*

| 원칙 | 판정 | 근거 |
|---|---|---|
| I. 승인 없는 계약 변경 금지 | PASS | 새 공개 API(`POST /api/links`)와 새 테이블 2개를 사용자가 승인함(2026-10-14, "A2·A3 승인"). 의존성 추가 없음 |
| II. 테스트 먼저 | PASS(계획) | 아래 테스트 계획을 먼저 작성하고 Red를 확인한다. 오류 주입은 quickstart §3 |
| III. 네 가지 결과 | PASS(계획) | quickstart의 보고 방식 |
| IV. 2회 실패 중단 | 해당 시 적용 | — |
| V. 작은 PR | PASS | PR 하나에 R-LINK-01~08만 담는다. 목록·필터·읽음(PRD-02~04)은 제외 |
| VI. ID 추적 | PASS(계획) | 테스트 이름을 `R_LINK_NN_…`으로 시작한다(ADR-0003) |
| VII. 우회 금지 | PASS | — |

**Phase 1 후 재확인**: 설계에서 위반이 새로 생기지 않았다. I은 2026-10-14 사용자 승인으로 PASS.

## 핵심 설계 결정

| # | 사용자 요청 | 결정 | 근거 |
|---|---|---|---|
| 1 | 메모 500자를 무엇으로 셀지와 DB 컬럼 크기를 같은 기준으로 맞추고, 한글·이모지 경계 테스트 | 앱은 코드 포인트로 센다(Q4). DB는 UTF-16 기준 한도 × 2: memo 1000, tag 60, url 4096 | H2가 UTF-16으로 센다는 실측. [research R1](research.md) |
| 2 | 스킴이 http/https로 제한되는지 확인 | spec R-LINK-02에 **이미 있음**(확인 사례 `ftp://` 거부 포함). 추가 질문 불필요 | spec |
| 3 | 정규화 URL 컬럼 DB 고유 제약 + 위반을 409로 | 이름 있는 제약, `saveAndFlush`, SQLState 23505 + 제약 이름으로 판정해 409 | [research R3](research.md) |
| 4 | 태그 공백·빈 태그·대소문자 중복 규칙 | `strip()`(전각 공백 포함), 빈 태그 400, 대소문자 무시로 합치고 첫 표기·순서 유지(Q5). 개수 10은 합친 뒤 기준 | spec R-LINK-05, [research R6](research.md) |
| 5 | 시퀀스 다이어그램 | `diagrams/save-link-sequence.html` + `.svg` | — |

## 테스트 계획

테스트 이름은 R-ID로 시작한다. 경계는 ADR-0003을 따른다. **(경계)** 표시는 사용자가 요청한 경계 테스트다.

### 단위 (순수 로직)

| 테스트 | R-ID | 내용 |
|---|---|---|
| `TextsTest.R_LINK_08_counts_code_points` | 08 | `가`=1, `😀`=1, `👍🏽`=2 |
| `UrlNormalizerTest.R_LINK_04_lowercases_scheme_and_host` | 04 | `HTTPS://Example.COM/a` → `https://example.com/a` |
| `UrlNormalizerTest.R_LINK_04_removes_only_default_port` | 04 | http:80, https:443 제거. http:443, https:8443 유지 |
| `UrlNormalizerTest.R_LINK_04_keeps_path_query_fragment_and_trailing_slash` | 04 | `/A/`, `?b=1`, `#x`, `%20`, `/경로/😀`를 그대로 둔다 |
| `LinkUrlValidatorTest.R_LINK_02_rejects_invalid_urls` | 02 | 매개변수 테스트: null, `""`, `"  "`, `not a url`, `http://`, `ftp://example.com`, `mailto:a@b.c`, `https:///nohost`, `https://한국.kr/a`(A1) |
| `LinkUrlValidatorTest.R_LINK_02_url_length_boundary` **(경계)** | 02, 08 | 2,048 코드 포인트 통과, 2,049 거부 |
| `LinkUrlValidatorTest.R_LINK_02_accepts_punycode_host` | 02 | `https://xn--3e0b707e.kr/a` 통과 (A1) |
| `TagNormalizerTest.R_LINK_05_strips_and_merges_ignoring_case_keeping_first` | 05 | `["Java"," java ","JAVA","스프링"]` → `["Java","스프링"]` |
| `TagNormalizerTest.R_LINK_05_strips_fullwidth_space` | 05 | `"　태그　"` → `"태그"` |

### 웹 슬라이스 (`@WebMvcTest`, 서비스는 `@MockitoBean`)

| 테스트 | R-ID | 내용 |
|---|---|---|
| `R_LINK_01_returns_201_with_saved_link` | 01 | 응답 필드: id, url, memo, tags, createdAt |
| `R_LINK_02_rejects_invalid_url_with_400` | 02, 06 | `errors[].field == "url"`, 서비스 호출 0회 |
| `R_LINK_03_accepts_500_code_point_memo_mixing_hangul_and_emoji` **(경계)** | 03, 08 | `"가"×249 + "😀"×251` = 500 코드 포인트(UTF-16 751) → 통과 |
| `R_LINK_03_rejects_501_code_point_memo_mixing_hangul_and_emoji` **(경계)** | 03, 08 | 위 문자열 + `"가"` = 501 → 400, `field == "memo"` |
| `R_LINK_08_memo_counts_skin_tone_emoji_as_two` **(경계)** | 08 | `"가"×498 + "👍🏽"` = 500 통과, `"가"×499 + "👍🏽"` = 501 거부 |
| `R_LINK_05_tag_count_boundary_after_merge` **(경계)** | 05 | 서로 다른 10개 통과, 11개 거부. 원본 11개 중 대소문자 중복 1개면 통과 |
| `R_LINK_05_tag_length_boundary` **(경계)** | 05, 08 | `"😀"×30` 통과, 31 거부 |
| `R_LINK_05_rejects_blank_or_null_tag` | 05 | `""`, `"   "`, `"　"`, `null` 원소 → 400 `field == "tags"` |
| `R_LINK_06_reports_all_invalid_fields_at_once` | 06 | url·memo·tags가 모두 틀리면 errors 3개 |
| `R_LINK_06_returns_409_problem_on_duplicate` | 04, 06 | 서비스가 `DuplicateLinkException`을 던지면 409, `application/problem+json`, `url` 필드 |
| `R_LINK_06_returns_400_problem_on_malformed_json` | 06 | 깨진 JSON → 400 ProblemDetail |

### JPA 슬라이스 (`@DataJpaTest`, H2 메모리)

| 테스트 | R-ID | 내용 |
|---|---|---|
| `R_LINK_04_unique_constraint_rejects_same_normalized_url` | 04 | 같은 URL을 두 번 `saveAndFlush` → `DataIntegrityViolationException`, 원인에 SQLState 23505와 `UK_LINK_NORMALIZED_URL` |
| `R_LINK_08_columns_fit_max_code_points_of_astral_chars` **(경계)** | 08 | memo `"😀"×500`(UTF-16 1000), 태그 `"😀"×30`(60), URL 2,048 코드 포인트(경로에 😀 포함) → 저장·재조회 일치 |
| `R_LINK_07_round_trips_hangul_memo_and_tags` | 07 | 한글 메모·태그 저장 후 재조회 일치, 태그 순서 유지 |

### 통합 (`@SpringBootTest` + MockMvc, 기능당 최소)

| 테스트 | R-ID | 내용 |
|---|---|---|
| `R_LINK_01_saves_link_end_to_end` | 01, 05, 07 | 한글·이모지 메모와 대소문자 중복 태그 → 201, DB 1건 |
| `R_LINK_04_rejects_equivalent_url_with_409_and_keeps_count` | 04, 06 | `https://example.com/a` 저장 후 `HTTPS://Example.COM:443/a` → 409, 건수 1. `/a/` → 201 |
| `R_LINK_04_translates_unique_violation_to_409_when_precheck_misses` | 04, 06 | `@MockitoSpyBean`으로 `existsByNormalizedUrl`을 `false`로 고정해 경쟁 상황을 재현 → 409, 건수 1 |
| `R_LINK_06_rejected_requests_store_nothing` | 06 | 400 사례 후 건수 0 |

## Project Structure

### Documentation (this feature)

```text
specs/001-save-link/
├── spec.md · plan.md · research.md · data-model.md · quickstart.md
├── contracts/save-link.md
├── diagrams/save-link-sequence.html · .svg
├── checklists/requirements.md
└── tasks.md            # /speckit-tasks 에서 생성
```

### Source Code

```text
backend/src/main/java/dev/linkshelf/
├── common/
│   └── ApiExceptionHandler.java      # ProblemDetail 400(errors)/409
└── link/
    ├── LinkController.java           # POST /api/links
    ├── LinkService.java              # 정규화 → 사전 확인 → saveAndFlush → 제약 위반 변환
    ├── Link.java                     # @Entity, @ElementCollection tags
    ├── LinkRepository.java           # existsByNormalizedUrl
    ├── SaveLinkRequest.java · LinkResponse.java   # record DTO
    ├── DuplicateLinkException.java
    ├── UrlNormalizer.java · TagNormalizer.java · Texts.java
    └── validation/
        ├── LinkUrl.java · LinkUrlValidator.java
        ├── MaxCodePoints.java · MaxCodePointsValidator.java
        └── LinkTags.java · LinkTagsValidator.java

backend/src/test/java/dev/linkshelf/link/
├── TextsTest · UrlNormalizerTest · TagNormalizerTest · LinkUrlValidatorTest
├── LinkControllerTest            # @WebMvcTest
├── LinkRepositoryTest            # @DataJpaTest
└── SaveLinkIntegrationTest       # @SpringBootTest
```

**Structure Decision**: 기존 `backend/` 단일 앱에 기능 패키지 `dev.linkshelf.link`를 추가한다(ADR-0001). 공통 오류 처리만 `dev.linkshelf.common`에 둔다.

## 승인 요청 (완료)

| # | 항목 | 초안 | 위치 | 결정 (2026-10-14, 사용자 답변) |
|---|---|---|---|---|
| A1 | **한글 도메인(IDN) URL** | `https://한국.kr/a`는 `java.net.URI`가 host를 못 읽어 **400으로 거부**된다. punycode(`xn--3e0b707e.kr`)로 보내야 함. 대안: `java.net.IDN.toASCII`로 변환해 허용(정규화 규칙 추가, spec R-LINK-04 변경) | research R2 | (a) 거부 — "A1은 (a)로" (이유 미기재) |
| A2 | API 계약 | `POST /api/links`, 201/400/409, ProblemDetail `errors`·`url` 확장, `Location` 헤더 없음 | contracts/save-link.md | 승인 — "A2·A3 승인" (이유 미기재) |
| A3 | 테이블 구조 | `link`(normalized_url 4096 UNIQUE, memo 1000, created_at), `link_tag`(name 60, position) | data-model.md | 승인 — "A2·A3 승인" (이유 미기재) |

## Complexity Tracking

위반 없음.
