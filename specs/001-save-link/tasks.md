# Tasks: 링크 저장

**Input**: `specs/001-save-link/` — spec.md, plan.md(승인됨 2026-10-14), research.md, data-model.md, contracts/save-link.md, quickstart.md
**기준 SHA**: `8c26558` · **브랜치**: `feat/<issue>-save-link` (T002에서 확정)

**Tests**: 필수다. constitution II(TDD)를 따른다.
- 모든 구현 task 앞에 실패하는 테스트 task와 **Red 확인** task를 둔다.
- Red는 실행 명령과 실패 출력 요약(컴파일 실패 또는 assertion 실패)을 남긴다.
- 테스트 이름은 [plan.md 테스트 계획](plan.md#테스트-계획)과 똑같이 쓴다.

**범위**: R-LINK-01~08만 다룬다. 목록 조회·태그 필터·읽음 표시(PRD-02~04), 수정·삭제, 프론트엔드는 다루지 않는다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 가능(다른 파일, 미완료 task에 의존하지 않음)
- **[Story]**: US1 저장(P1), US2 입력 거부(P1), US3 중복 거부(P2) — spec.md User Story 번호
- 경로 기준: `backend/src/main/java/dev/linkshelf/` = `main/`, `backend/src/test/java/dev/linkshelf/` = `test/`(설명에는 전체 경로를 적는다)
- 테스트 실행: `cd backend && ./gradlew test --tests '<FQCN>'`, 전체는 `cd backend && ./gradlew test`

## Issue (런북 5-4)

GitHub Issue 하나 = 독립적으로 검증 가능한 묶음 하나. 번호는 생성 후 기록한다.

| Issue | 제목 | 포함 task | R-ID | 독립 검증 |
|---|---|---|---|---|
| #___ | 링크 저장 API (저장·입력 검증·중복 409) | T004~T041 | R-LINK-01~08 | 유효 요청 201, 규칙 위반 400(경계값 포함), 동치 URL·경쟁 경로 409, 거부 시 건수 유지 |

- 사용자 결정(2026-10-14): "하나로 만들어". 후보 A(저장·검증)와 B(중복 409)를 Issue 하나로 합쳤다. PR #2가 `Closes #N`으로 닫는다.

---

## Phase 1: Setup

**Purpose**: Issue·브랜치 준비. 기능 코드는 없다.

- [ ] T001 Issue의 제목·본문 미리보기를 보여준다.
  - 본문에 `Task-ID`(`Task-ID: T004-T041`), R-ID, 커밋에 고정한 spec 링크를 넣는다.
  - 중복 확인: `gh issue list --search "Task-ID: T004-T041 in:body" --state all`.
  - **사용자 승인 후** `gh issue create`를 실행하고 `gh issue view`로 재확인한다.
  - 번호를 이 파일 "Issue" 표에 기록한다(specs/001-save-link/tasks.md).
- [ ] T002 `main`(`8c26558`)에서 임시 로컬 브랜치 `feat/save-link`를 만들고, Issue 생성 후 `git branch -m feat/<Issue 번호>-save-link`로 이름을 바꾼다(사용자 승인 2026-10-14).
  - Issue 생성 전에 `specs/001-save-link/` 전체를 커밋하고, Issue의 spec 링크를 그 SHA로 고정한다(코드 변경 없음).
- [ ] T003 기준 테스트를 실행해 기존 실패와 새 회귀를 구분한다.
  - `cd backend && ./gradlew test --rerun` → 2 tests PASS를 기록한다(backend/build/test-results/test/*.xml).

---

## Phase 2: Foundational (모든 스토리 선행)

**Purpose**: 길이 세는 규칙(R-LINK-08). URL·메모·태그 검증과 정규화가 모두 이것을 쓴다.

- [ ] T004 [P] 테스트 작성: `TextsTest.R_LINK_08_counts_code_points` — `가`=1, `😀`=1, `👍🏽`=2. backend/src/test/java/dev/linkshelf/link/TextsTest.java
- [ ] T005 Red 확인: `cd backend && ./gradlew test --tests 'dev.linkshelf.link.TextsTest'` FAIL(컴파일 실패 또는 assertion)을 기록한다.
- [ ] T006 구현: `Texts.codePoints(String)`(`String.codePointCount`). 공백 제거용 `Texts.stripToNull(String)`(`strip()` 후 비면 null)도 둔다. backend/src/main/java/dev/linkshelf/link/Texts.java → T004 PASS.

**Checkpoint**: 코드 포인트 계산 PASS.

---

## Phase 3: User Story 1 — 링크 저장 (P1) 🎯 MVP

**Goal**: 유효한 URL(+메모·태그)을 저장하고 201로 돌려준다. URL은 정규화 형태로, 태그는 합친 형태로 저장한다.

**Independent Test**: 빈 DB에 `{"url":"HTTPS://Example.COM:443/a","memo":"한글 😀","tags":["Java"," java ","스프링"]}` → 201, `url`=`https://example.com/a`, `tags`=`["Java","스프링"]`, DB 1건.

### Tests (먼저 작성) ⚠️

- [ ] T007 [P] [US1] 테스트 작성(R-LINK-04 정규화): backend/src/test/java/dev/linkshelf/link/UrlNormalizerTest.java
  - `UrlNormalizerTest.R_LINK_04_lowercases_scheme_and_host` — `HTTPS://Example.COM/a` → `https://example.com/a`
  - `UrlNormalizerTest.R_LINK_04_removes_only_default_port` — http:80, https:443은 제거, http:443과 https:8443은 유지
  - `UrlNormalizerTest.R_LINK_04_keeps_path_query_fragment_and_trailing_slash` — `/A/`, `?b=1`, `#x`, `%20`, `/경로/😀`를 그대로 둔다. `%20`이 `%2520`이 되면 실패(research R2)
- [ ] T008 [P] [US1] 테스트 작성(R-LINK-05): backend/src/test/java/dev/linkshelf/link/TagNormalizerTest.java
  - `TagNormalizerTest.R_LINK_05_strips_and_merges_ignoring_case_keeping_first` — `["Java"," java ","JAVA","스프링"]` → `["Java","스프링"]`
  - `TagNormalizerTest.R_LINK_05_strips_fullwidth_space` — `"　태그　"` → `"태그"`
- [ ] T009 [P] [US1] 테스트 작성(@DataJpaTest): backend/src/test/java/dev/linkshelf/link/LinkRepositoryTest.java
  - `LinkRepositoryTest.R_LINK_07_round_trips_hangul_memo_and_tags` (R-LINK-07) — 한글 메모·태그를 저장 후 다시 조회해 일치하는지, 태그 순서가 유지되는지
  - `LinkRepositoryTest.R_LINK_08_columns_fit_max_code_points_of_astral_chars` (R-LINK-08) — memo `"😀"×500`(UTF-16 1000), 태그 `"😀"×30`(60), URL 2,048 코드 포인트(경로에 😀 포함)를 저장·재조회
- [ ] T010 [P] [US1] 테스트 작성(@WebMvcTest, `LinkService`는 `@MockitoBean`): `LinkControllerTest.R_LINK_01_returns_201_with_saved_link` (R-LINK-01)
  - 응답 필드는 `id`, `url`, `memo`, `tags`, `createdAt`(ISO-8601 `Z`)
  - `Location` 헤더가 없는지 확인한다(contracts/save-link.md)
  - backend/src/test/java/dev/linkshelf/link/LinkControllerTest.java
- [ ] T011 [P] [US1] 테스트 작성(@SpringBootTest + MockMvc): `SaveLinkIntegrationTest.R_LINK_01_saves_link_end_to_end` (R-LINK-01, 05, 07)
  - 한글·이모지 메모와 대소문자 중복 태그를 보내면 201, 정규화 URL, 합친 태그, `linkRepository.count()==1`
  - backend/src/test/java/dev/linkshelf/link/SaveLinkIntegrationTest.java
- [ ] T012 [US1] Red 확인: `cd backend && ./gradlew test --tests 'dev.linkshelf.link.*'` → T007~T011이 FAIL인 것을 기록한다(T004는 PASS 유지).

### Implementation

- [ ] T013 [P] [US1] `UrlNormalizer.normalize(String)`: backend/src/main/java/dev/linkshelf/link/UrlNormalizer.java
  - `strip()` → `new URI(..)`
  - 다음을 이어 붙인다: `scheme.toLowerCase(ROOT) + "://" + [rawUserInfo@] + host.toLowerCase(ROOT) + [":"+port, 기본 포트가 아닐 때] + rawPath + ["?"+rawQuery] + ["#"+rawFragment]`
  - 여러 인자 `URI` 생성자와 `URI.normalize()`는 쓰지 않는다(research R2)
  - 입력이 유효하다는 전제(검증은 US2)
  - → T007 PASS
- [ ] T014 [P] [US1] `TagNormalizer.normalize(List<String>)`: backend/src/main/java/dev/linkshelf/link/TagNormalizer.java
  - null 목록은 `[]`
  - 각 원소에 `strip()`
  - `toLowerCase(Locale.ROOT)` 키로 `LinkedHashMap.putIfAbsent`(첫 표기·순서 유지)
  - → T008 PASS
- [ ] T015 [US1] `Link` 엔티티: backend/src/main/java/dev/linkshelf/link/Link.java
  - 테이블 `link`: `id BIGINT PK IDENTITY`, `normalized_url VARCHAR(4096) NOT NULL`, `memo VARCHAR(1000) NULL`, `created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL`(`Instant`)
  - 태그: `@ElementCollection` + `@CollectionTable(name="link_tag", joinColumns=link_id)` + `@OrderColumn(name="position")` + `@Column(name="name", length=60, nullable=false)` → "PK (link_id, position)"
  - **고유 제약 `uk_link_normalized_url`은 아직 넣지 않는다(T042에서 추가)**
- [ ] T016 [US1] `LinkRepository extends JpaRepository<Link, Long>`: backend/src/main/java/dev/linkshelf/link/LinkRepository.java → T009 PASS
- [ ] T017 [P] [US1] record DTO: backend/src/main/java/dev/linkshelf/link/SaveLinkRequest.java, backend/src/main/java/dev/linkshelf/link/LinkResponse.java
  - `SaveLinkRequest(String url, String memo, List<String> tags)`
  - `LinkResponse(Long id, String url, String memo, List<String> tags, Instant createdAt)` + `from(Link)`
- [ ] T018 [US1] `LinkService.save(SaveLinkRequest)`: backend/src/main/java/dev/linkshelf/link/LinkService.java
  - `@Transactional`
  - URL 정규화, `Texts.stripToNull(memo)`, 태그 정규화, `createdAt = Instant.now()`
  - `linkRepository.saveAndFlush`
  - `LinkResponse`를 돌려준다
- [ ] T019 [US1] `LinkController`: `POST /api/links` → `ResponseEntity.status(201).body(..)`(Location 없음). 이 단계에서는 `@Valid`를 붙이지 않는다(US2). backend/src/main/java/dev/linkshelf/link/LinkController.java → T010, T011 PASS
- [ ] T020 [US1] Green 확인: `cd backend && ./gradlew test` 전체 PASS를 기록한다.

**Checkpoint**: US1 단독 동작(201).

---

## Phase 4: User Story 2 — 잘못된 입력 거부 (P1)

**Goal**: URL·메모·태그 규칙 위반을 400 ProblemDetail(`errors[{field,message}]`)로 한 번에 알려 준다. 아무것도 저장하지 않는다.

**Independent Test**: `{"url":"ftp://x","memo":"가"×501,"tags":["   "]}` → 400, errors 3개(url, memo, tags), DB 0건.

### Tests (먼저 작성) ⚠️

- [ ] T021 [P] [US2] 테스트 작성(R-LINK-02, 08): backend/src/test/java/dev/linkshelf/link/LinkUrlValidatorTest.java
  - `LinkUrlValidatorTest.R_LINK_02_rejects_invalid_urls` — 매개변수 테스트: null, `""`, `"  "`, `not a url`, `http://`, `ftp://example.com`, `mailto:a@b.c`, `https:///nohost`, `https://한국.kr/a`(A1)
  - `LinkUrlValidatorTest.R_LINK_02_url_length_boundary` — 2,048 코드 포인트는 통과, 2,049는 거부
  - `LinkUrlValidatorTest.R_LINK_02_accepts_punycode_host` — `https://xn--3e0b707e.kr/a`는 통과(A1)
- [ ] T022 [P] [US2] 테스트 추가(@WebMvcTest): backend/src/test/java/dev/linkshelf/link/LinkControllerTest.java
  - `R_LINK_02_rejects_invalid_url_with_400` (R-LINK-02, 06) — `errors[].field=="url"`, `application/problem+json`, `verify(linkService, never()).save(any())`
  - `R_LINK_03_accepts_500_code_point_memo_mixing_hangul_and_emoji` (R-LINK-03, 08) — `"가"×249 + "😀"×251`(500 코드 포인트, UTF-16 751)은 201
  - `R_LINK_03_rejects_501_code_point_memo_mixing_hangul_and_emoji` (R-LINK-03, 08) — 위 문자열 + `"가"`는 400, `field=="memo"`
  - `R_LINK_08_memo_counts_skin_tone_emoji_as_two` (R-LINK-08) — `"가"×498 + "👍🏽"`는 201, `"가"×499 + "👍🏽"`는 400
  - `R_LINK_05_tag_count_boundary_after_merge` (R-LINK-05) — 서로 다른 10개 201, 11개 400. 원본 11개 중 대소문자 중복 1개면 201
  - `R_LINK_05_tag_length_boundary` (R-LINK-05, 08) — `"😀"×30`은 201, `×31`은 400
  - `R_LINK_05_rejects_blank_or_null_tag` (R-LINK-05) — `""`, `"   "`, `"　"`, `null` 원소는 400, `field=="tags"`
  - `R_LINK_06_reports_all_invalid_fields_at_once` (R-LINK-06) — url·memo·tags가 모두 틀리면 errors 3개
  - `R_LINK_06_returns_400_problem_on_malformed_json` (R-LINK-06) — 깨진 JSON은 400 ProblemDetail
- [ ] T023 [P] [US2] 테스트 추가: `SaveLinkIntegrationTest.R_LINK_06_rejected_requests_store_nothing` (R-LINK-06) — 400 사례 뒤 `count()==0`. backend/src/test/java/dev/linkshelf/link/SaveLinkIntegrationTest.java
- [ ] T024 [US2] Red 확인: `cd backend && ./gradlew test --tests 'dev.linkshelf.link.*'` → T021~T023 FAIL을 기록한다. 경계 테스트의 통과(201) 사례도 FAIL 또는 컴파일 실패여야 한다. 이미 PASS인 사례는 이유를 적는다.

### Implementation

- [ ] T025 [P] [US2] `@MaxCodePoints(int value)` + `MaxCodePointsValidator`: backend/src/main/java/dev/linkshelf/link/validation/MaxCodePoints.java, backend/src/main/java/dev/linkshelf/link/validation/MaxCodePointsValidator.java
  - null·공백뿐이면 통과
  - `Texts.codePoints(strip()) <= value`
  - 메시지 "500자 이하여야 합니다" 형식(`{value}`)
- [ ] T026 [P] [US2] `@LinkUrl` + `LinkUrlValidator`: backend/src/main/java/dev/linkshelf/link/validation/LinkUrl.java, backend/src/main/java/dev/linkshelf/link/validation/LinkUrlValidator.java
  - 필수, `strip()` 후 ≤ 2,048 코드 포인트
  - `new URI` 파싱 성공, 스킴 `http`/`https`(대소문자 무시), `getHost()!=null`
  - 한글 도메인은 host가 null이라 거부된다
  - → T021 PASS
- [ ] T027 [P] [US2] `@LinkTags(maxCount=10, maxLength=30)` + `LinkTagsValidator`: backend/src/main/java/dev/linkshelf/link/validation/LinkTags.java, backend/src/main/java/dev/linkshelf/link/validation/LinkTagsValidator.java
  - null 목록은 통과
  - 원소가 null이거나 `strip()` 후 비면 거부
  - 원소는 ≤ 30 코드 포인트
  - `TagNormalizer.normalize` 결과 개수 ≤ 10(합친 뒤 기준, research R4)
  - 오류 field는 `tags`
- [ ] T028 [US2] `SaveLinkRequest`에 `@LinkUrl url`, `@MaxCodePoints(500) memo`, `@LinkTags(maxCount=10, maxLength=30) tags`를 붙인다. backend/src/main/java/dev/linkshelf/link/SaveLinkRequest.java
- [ ] T029 [US2] `LinkController`에 `@Valid @RequestBody`를 붙인다. backend/src/main/java/dev/linkshelf/link/LinkController.java
- [ ] T030 [US2] `ApiExceptionHandler extends ResponseEntityExceptionHandler`(`@RestControllerAdvice`): backend/src/main/java/dev/linkshelf/common/ApiExceptionHandler.java
  - `handleMethodArgumentNotValid`를 재정의한다
  - ProblemDetail 400에 `errors: [{field, message}]` 확장 필드를 붙인다(field 오류 전부)
  - 깨진 JSON은 상속 기본 동작으로 400 ProblemDetail이 되는지 확인한다(contracts/save-link.md 400 예시)
  - → T022, T023 PASS
- [ ] T031 [US2] Green 확인: `cd backend && ./gradlew test` 전체 PASS를 기록한다.

**Checkpoint**: US1 + US2 완료(저장·입력 검증).

---

## Phase 5: User Story 3 — 중복 URL 거부 (P2)

**Goal**: 같은 정규화 URL은 409로 거부한다. 사전 확인과 DB 고유 제약 `uk_link_normalized_url`을 함께 쓰고, 제약 위반도 409로 바꾼다(research R3).

**Independent Test**: `https://example.com/a` 저장 후 `HTTPS://Example.COM:443/a`는 409, `count()==1`. `https://example.com/a/`는 201.

### Tests (먼저 작성) ⚠️

- [ ] T032 [P] [US3] 테스트 추가: `LinkRepositoryTest.R_LINK_04_unique_constraint_rejects_same_normalized_url` (R-LINK-04)
  - 같은 URL을 두 번 `saveAndFlush`하면 `DataIntegrityViolationException`
  - 원인 체인에 SQLState `23505`가 있고 메시지에 `UK_LINK_NORMALIZED_URL`(대소문자 무시)이 있다
  - backend/src/test/java/dev/linkshelf/link/LinkRepositoryTest.java
- [ ] T033 [P] [US3] 테스트 추가: `LinkControllerTest.R_LINK_06_returns_409_problem_on_duplicate` (R-LINK-04, 06)
  - `linkService.save`가 `DuplicateLinkException`을 던지면 409, `application/problem+json`, `detail`, 확장 필드 `url`
  - backend/src/test/java/dev/linkshelf/link/LinkControllerTest.java
- [ ] T034 [P] [US3] 테스트 추가: backend/src/test/java/dev/linkshelf/link/SaveLinkIntegrationTest.java
  - `SaveLinkIntegrationTest.R_LINK_04_rejects_equivalent_url_with_409_and_keeps_count` (R-LINK-04, 06) — `/a` 저장, `HTTPS://Example.COM:443/a`는 409, 건수 1, `/a/`는 201
  - `SaveLinkIntegrationTest.R_LINK_04_translates_unique_violation_to_409_when_precheck_misses` (R-LINK-04, 06) — `@MockitoSpyBean LinkRepository`로 `existsByNormalizedUrl`을 `false`로 고정한다. 같은 URL을 두 번 보내면 두 번째가 409, 건수 1
- [ ] T035 [US3] Red 확인: `cd backend && ./gradlew test --tests 'dev.linkshelf.link.*'` → T032~T034 FAIL을 기록한다.

### Implementation

- [ ] T036 [US3] `Link`에 `@Table(name="link", uniqueConstraints=@UniqueConstraint(name="uk_link_normalized_url", columnNames="normalized_url"))`를 추가한다("CONSTRAINT uk_link_normalized_url UNIQUE"). backend/src/main/java/dev/linkshelf/link/Link.java → T032 PASS
- [ ] T037 [P] [US3] `DuplicateLinkException(String normalizedUrl)`: backend/src/main/java/dev/linkshelf/link/DuplicateLinkException.java
- [ ] T038 [US3] `LinkRepository.existsByNormalizedUrl(String)`를 추가한다. backend/src/main/java/dev/linkshelf/link/LinkRepository.java
- [ ] T039 [US3] `LinkService.save`를 수정한다: backend/src/main/java/dev/linkshelf/link/LinkService.java
  - 정규화 뒤 `existsByNormalizedUrl`이 true면 `DuplicateLinkException`
  - `saveAndFlush`에서 `DataIntegrityViolationException`이 나면, 원인 체인의 `SQLException`이 SQLState `23505`이고 메시지에 `UK_LINK_NORMALIZED_URL`(대소문자 무시)이 있을 때만 `DuplicateLinkException`으로 바꾼다
  - 그 밖의 경우는 그대로 다시 던진다
- [ ] T040 [US3] `ApiExceptionHandler`에 `@ExceptionHandler(DuplicateLinkException)` → ProblemDetail 409(`detail`="이미 저장된 URL입니다", 확장 필드 `url`)를 추가한다. backend/src/main/java/dev/linkshelf/common/ApiExceptionHandler.java → T033, T034 PASS
- [ ] T041 [US3] Green 확인: `cd backend && ./gradlew test` 전체 PASS를 기록한다.

**Checkpoint**: US3 완료. 전 스토리 동작.

---

## Phase 6: Polish & 검증 (런북 5-6 ~ 5-9)

- [ ] T042 전체 검증: `cd backend && ./gradlew test --rerun`
  - 결과 xml의 tests/failures/errors/skipped 합계를 기록한다
  - SC-004 확인: R-LINK-01~08 각각에 연결된 테스트 이름을 grep으로 확인한다(`grep -rhoE 'R_LINK_0[1-8]_[a-z0-9_]+' backend/src/test`)
- [ ] T043 `/speckit-converge`로 spec·plan 대비 누락을 확인한다. 누락은 R-LINK-01~08 범위 안에서만 tasks에 추가해 처리한다.
- [ ] T044 오류 주입(런북 5-8, A4):
  - `LinkControllerTest.R_LINK_06_returns_409_problem_on_duplicate`의 기대 상태를 409 → 400으로 바꾼다
  - `./gradlew test` FAIL 출력을 저장한다
  - `git checkout -- backend/src/test/java/dev/linkshelf/link/LinkControllerTest.java`로 원복하고 PASS를 다시 확인한다
  - 커밋하지 않는다. 로그는 PR 본문에 남긴다
- [ ] T045 [P] quickstart §2 수동 확인 4건을 실행할지 사용자에게 묻는다. 실행하지 않으면 PR 검증 표에 NOT_RUN과 이유를 적는다. specs/001-save-link/quickstart.md
- [ ] T046 PR #2 준비(`feat/<issue>-save-link` → `main`):
  - 본문: `Closes #N`, R-ID ↔ 테스트 이름 표, 검증 표(PASS/FAIL/SKIP/NOT_RUN), 기준 SHA `8c26558`, 남은 위험, 계측 1줄(런북 §5)
  - 남은 위험: IDN 거부, `ß`/`SS` 미접기, 태그 대소문자 중복은 DB가 아니라 앱에서만 막음
  - **사용자 확인 후** push와 `gh pr create`를 실행한다
- [ ] T047 docs/ai/handoff.md를 갱신한다: 단계, 브랜치, 기준 SHA, 마지막 명령·결과, 다음 행동 1개.

---

## Dependencies & Execution Order

- Phase 1(T001~T003) → Phase 2(T004~T006) → US1(T007~T020) → US2(T021~T031) → US3(T032~T041) → Phase 6(T042~T047)
- US2는 US1의 컨트롤러·DTO에 검증을 붙이므로 US1 뒤에 온다. US3는 US1의 저장 경로가 필요하다. US2와 US3는 서로 독립이라 순서를 바꿔도 된다. 다만 둘 다 `LinkControllerTest`·`SaveLinkIntegrationTest`·`ApiExceptionHandler`를 수정하므로 동시에 진행하지 않는다.
- 각 스토리 안에서는 테스트 작성 → Red 확인 → 구현 → Green 확인 순서를 지킨다.

### Parallel Opportunities

- US1 테스트 T007~T011은 서로 다른 파일이라 동시에 작성할 수 있다. 구현 T013, T014, T017도 병렬로 할 수 있다.
- US2: T021~T023 병렬, 검증기 T025~T027 병렬.
- US3: T032~T034 병렬.
- 이 세션은 단일 에이전트로 순서대로 실행한다(런북: 멀티 에이전트 제외). [P]는 독립성을 표시할 뿐이다.

## Implementation Strategy

1. **MVP = US1**: 유효 요청 201. 여기서 멈춰도 저장 기능은 시연할 수 있다.
2. US2를 더하면 데이터 오염을 막는 최소 기능이 된다.
3. US3를 더하면 Issue 수용 기준이 모두 충족된다.
4. Phase 6: 검증, converge, 오류 주입, PR #2 하나.
