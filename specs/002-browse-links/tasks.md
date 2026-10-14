# Tasks: 링크 목록 조회·태그 필터·읽음 표시

**Input**: `specs/002-browse-links/` — spec.md, plan.md(승인됨 2026-10-14, B1~B5), research.md, data-model.md, contracts/browse-links.md, quickstart.md
**기준 SHA**: `c2c2bc3` · **spec 브랜치**: `chore/spec-002-browse-links`

**Tests**: 필수다. constitution II(TDD)를 따른다.
- 스토리마다 실패하는 테스트를 먼저 쓰고, **Red 확인 → Red 커밋** 후 구현한다.
- Red는 실행 명령과 실패 요약(컴파일 실패 또는 assertion 실패)을 PR에 남긴다.
- 테스트 이름은 `R_LINK_NN_…`으로 시작한다(ADR-0003).

**범위**: R-LINK-09~18. 안 읽은 것만 보기, 여러 태그 필터, 검색, 수정·삭제, 프론트엔드는 다루지 않는다.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: 병렬 가능(다른 파일, 미완료 task에 의존하지 않음)
- **[Story]**: US1 목록(P1), US2 태그 필터(P2), US3 읽음 표시(P3)
- 경로 기준: `backend/src/main/java/dev/linkshelf/`, `backend/src/test/java/dev/linkshelf/`
- 테스트 실행: `cd backend && ./gradlew test --tests '<FQCN>'`, 전체는 `cd backend && ./gradlew test`

## Issue

스토리 하나 = Issue 하나 = PR 하나. 순서대로 진행한다(병렬 없음: 세 스토리가 `LinkController`·`LinkService`·`LinkResponse`를 함께 고친다, advisor 판단). 사용자 승인 "추천안으로 ㄱ"(2026-10-14)으로 생성.

| Issue | 제목 | 포함 task | R-ID | 독립 검증 |
|---|---|---|---|---|
| [#12](https://github.com/DongWonLee2/linkshelf/issues/12) | 링크 목록 조회 API (쪽 나누기) | T001~T016 | R-LINK-09, 10, 11 | 최신순·동률 id순, 쪽 경계 겹침·빠짐 없음, 잘못된 page·size 400 |
| [#13](https://github.com/DongWonLee2/linkshelf/issues/13) | 태그 하나로 목록 거르기 | T017~T027 | R-LINK-12, 13, 14 | 대소문자·공백 무시 전체 일치, 부분 불일치, 공백·31자 tag 400 |
| [#14](https://github.com/DongWonLee2/linkshelf/issues/14) | 읽음/안 읽음 지정과 기존 데이터 기본값 | T028~T045 | R-LINK-15, 16, 17, 18 | 반복 지정 같은 결과, 404·400, 저장 응답 `read:false`, 기존 DB 복사본 기동 세 층 |

---

## Phase 1: Setup

**Purpose**: 별도 준비 없음. 의존성·설정 변경이 없다(plan Technical Context). 각 스토리는 `main` 최신에서 `feat/<issue>-<slug>` 브랜치로 시작한다.

---

## Phase 2: Foundational

**Purpose**: 없음. 공통 코드(쿼리 검증 오류 처리)는 처음 필요한 US1에 넣는다.

---

## Phase 3: User Story 1 — 저장한 링크 목록 보기 (P1) 🎯 MVP

**Goal**: `GET /api/links?page&size` → `{items, page, size, totalElements, totalPages, hasNext}` (contracts/browse-links.md)

**Independent Test**: 링크 3개 저장 → 목록이 최신순, 저장 내용 그대로. `size=1`로 모든 쪽을 합치면 전부.

### Tests (먼저 작성) ⚠️

- [ ] T001 [P] [US1] `backend/src/test/java/dev/linkshelf/link/ListLinksControllerTest.java`(`@WebMvcTest(LinkController.class)`, 서비스 `@MockitoBean`): `R_LINK_10_defaults_page_0_size_20`(파라미터 없을 때 서비스에 0·20 전달, `tag` 생략 → 200), `R_LINK_10_returns_page_object_shape`(6개 필드), `R_LINK_11_size_bounds`(1·100 → 200, 0·101 → 400 `errors[0].field=size`), `R_LINK_11_negative_page_400`, `R_LINK_11_reports_page_and_size_together`(`page=-1&size=0` → errors 2개), `R_LINK_11_non_numeric_page_reports_only_that_field`(`page=abc&size=0` → errors 1개 `page`, 승인 B5), `R_LINK_11_detail_is_invalid_request_parameters`(`detail`=`Invalid request parameters.`)
- [ ] T002 [P] [US1] `backend/src/test/java/dev/linkshelf/link/ListLinksRepositoryTest.java`(`@DataJpaTest`): `R_LINK_09_orders_by_created_at_desc_then_id_desc`(같은 `createdAt` 2개 포함), `R_LINK_10_pages_without_overlap_or_gap`(5개, size 2 → 3쪽 합집합 = 전체, 교집합 없음), `R_LINK_10_counts_total`
- [ ] T003 [P] [US1] `backend/src/test/java/dev/linkshelf/link/ListLinksIntegrationTest.java`(`@SpringBootTest` + MockMvc, `@BeforeEach deleteAll`): `R_LINK_09_lists_saved_links_newest_first_with_korean_intact`(저장 3개 → C,B,A, 메모·태그 한글 그대로), `R_LINK_10_all_pages_together_return_every_link`(25개, 기본 크기 → 2쪽, SC-001), `R_LINK_09_empty_store_returns_empty_items`(`items:[]`, `totalPages:0`), `R_LINK_10_page_after_last_is_empty`
- [ ] T004 [US1] Red 확인: `./gradlew test --tests '*ListLinks*'` 실패(컴파일 실패 포함) 요약 기록 → **Red 커밋**

### Implementation

- [ ] T005 [P] [US1] `backend/src/main/java/dev/linkshelf/link/LinkPageResponse.java` record `(List<LinkResponse> items, int page, int size, long totalElements, int totalPages, boolean hasNext)` + `static from(Page<LinkResponse>)`
- [ ] T006 [US1] `backend/src/main/java/dev/linkshelf/link/LinkRepository.java`: 정렬은 `Sort.by(desc("createdAt"), desc("id"))`를 서비스에서 `PageRequest`로 전달(기본 `findAll(Pageable)` 사용)
- [ ] T007 [US1] `backend/src/main/java/dev/linkshelf/link/LinkService.java`: `@Transactional(readOnly = true) LinkPageResponse list(int page, int size)` — 트랜잭션 안에서 `LinkResponse.from` 변환(OSIV 꺼짐, plan Constraints)
- [ ] T008 [US1] `backend/src/main/java/dev/linkshelf/link/Link.java`: 태그 컬렉션에 `@BatchSize(size = 100)` (research R3)
- [ ] T009 [US1] `backend/src/main/java/dev/linkshelf/link/LinkController.java`: `@GetMapping` `list(@RequestParam(defaultValue = "0") @Min(0) int page, @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size)`. 클래스에 `@Validated`를 붙이지 않는다(research R4)
- [ ] T010 [US1] `backend/src/main/java/dev/linkshelf/common/ApiExceptionHandler.java`: `handleHandlerMethodValidationException` 오버라이드 → 400, `detail` `Invalid request parameters.`, `errors[{field: 파라미터 이름, message}]`; `handleTypeMismatch` 오버라이드 → 같은 형식, 해당 항목 하나(승인 B5). 기존 저장 API 400 형식은 바꾸지 않는다
- [ ] T011 [US1] 오류 메시지(한국어): page `0 이상이어야 합니다`, size `1 이상 100 이하여야 합니다`, 숫자 아님 `숫자여야 합니다`
- [ ] T012 [US1] Green: `./gradlew test` 전체 PASS(기존 테스트 포함) 기록 → 커밋

### 검증

- [ ] T013 [US1] 고장 내기(quickstart §5): 정렬에서 `id` 내림차순 제거 → T002 FAIL, `@Max(100)` 제거 → T001 FAIL 확인 후 되돌림(되돌릴 때 `git checkout`을 쓰지 말고 편집으로 되돌린다: #5에서 커밋 전 변경을 지운 실수)
- [ ] T014 [US1] quickstart §2의 US1 curl 확인(bootRun) 결과 기록
- [ ] T015 [US1] PR 생성(`Closes #N`, R-ID↔테스트 표, 검증 표, 기준 SHA) → CI `backend-test` PASS
- [ ] T016 [US1] pr-reviewer 리뷰 → 분류 코멘트(런북 8-4) → 수용분 수정

**Checkpoint**: 사용자 병합 후 US2 시작.

---

## Phase 4: User Story 2 — 태그 하나로 거르기 (P2)

**Goal**: `GET /api/links?tag=` 추가. 앞뒤 공백 제거 + 대소문자 무시 + 태그 전체 일치(R-LINK-13)

**Independent Test**: `Java`/`java`,`스프링`/태그 없음 링크로 `tag=%20JAVA%20` → 두 링크만 최신순.

### Tests (먼저 작성) ⚠️

- [ ] T017 [P] [US2] `backend/src/test/java/dev/linkshelf/link/TagFilterControllerTest.java`(`@WebMvcTest`): `R_LINK_14_blank_tag_400`(`tag=`, `tag=%20%20` → errors `tag`), `R_LINK_14_tag_over_30_code_points_400`(31자, 😀 포함), `R_LINK_14_tag_of_30_code_points_ok`, `R_LINK_12_passes_tag_to_service`
- [ ] T018 [P] [US2] `backend/src/test/java/dev/linkshelf/link/TagFilterRepositoryTest.java`(`@DataJpaTest`): `R_LINK_13_matches_case_insensitively`(`Java`·`java` 둘 다), `R_LINK_13_requires_whole_tag`(`Jav`·`JavaScript` 미일치), `R_LINK_13_korean_tag`(`스프링`), `R_LINK_12_link_with_many_tags_appears_once`, `R_LINK_12_total_counts_filtered_only`
- [ ] T019 [P] [US2] `backend/src/test/java/dev/linkshelf/link/TagFilterIntegrationTest.java`(`@SpringBootTest`): `R_LINK_13_padded_uppercase_tag_filters_end_to_end`(SC-002), `R_LINK_13_unknown_tag_returns_empty_items`, `R_LINK_12_filtered_pages_keep_newest_first`
- [ ] T020 [US2] Red 확인 → **Red 커밋**

### Implementation

- [ ] T021 [US2] `backend/src/main/java/dev/linkshelf/link/LinkRepository.java`: `@Query("select l from Link l where exists (select 1 from l.tags t where lower(t) = :tag)") Page<Link> findByTag(String tag, Pageable)` + 같은 조건의 countQuery (research R2)
- [ ] T022 [US2] `backend/src/main/java/dev/linkshelf/link/LinkService.java`: `list(int page, int size, String tag)` — tag가 null이면 US1 경로, 아니면 `tag.strip().toLowerCase(Locale.ROOT)`로 `findByTag`
- [ ] T023 [US2] `backend/src/main/java/dev/linkshelf/link/LinkController.java`: `@RequestParam(required = false) @Pattern(regexp = ".*\\S.*") @MaxCodePoints(30) String tag`. `@NotBlank`는 쓰지 않는다(null 거부 → tag 생략 400, research R4)
- [ ] T024 [US2] 오류 메시지: tag 공백 `공백만 있을 수 없습니다`, 길이 `30자 이하여야 합니다`
- [ ] T025 [US2] Green: 전체 PASS → 커밋
- [ ] T026 [US2] 고장 내기: 서비스의 소문자화 제거 → T018/T019 대소문자 테스트 FAIL 확인 후 편집으로 되돌림
- [ ] T027 [US2] PR → CI → pr-reviewer → 분류 코멘트

**Checkpoint**: 사용자 병합 후 US3 시작.

---

## Phase 5: User Story 3 — 읽음/안 읽음 표시 (P3)

**Goal**: `link.is_read BOOLEAN DEFAULT FALSE NOT NULL`, `PUT /api/links/{id}/read` `{"read": bool}`, 모든 링크 표현에 `read`

**Independent Test**: 저장 → `read:false` → 읽음 지정 2번 → 목록에서 `read:true`, 다른 필드 불변.

### 준비

- [ ] T028 [US3] **시작 전** 원본 `backend/data/linkshelf.mv.db`를 저장소 밖 백업 폴더에 복사(quickstart §4-0, 되돌리기용). 원본은 건드리지 않는다

### Tests (먼저 작성) ⚠️

- [ ] T029 [P] [US3] `backend/src/test/java/dev/linkshelf/link/MarkReadControllerTest.java`(`@WebMvcTest`): `R_LINK_17_missing_read_400`, `R_LINK_17_null_read_400`, `R_LINK_17_string_true_400`, `R_LINK_17_number_1_400`(research R5: Jackson 3 강제 변환 여부를 이 테스트로 확인), `R_LINK_17_non_numeric_id_400`, `R_LINK_17_unknown_id_404_shape`(`detail`, `id`), `R_LINK_17_bad_body_before_unknown_id`(400 우선)
- [ ] T030 [P] [US3] `backend/src/test/java/dev/linkshelf/link/MarkReadRepositoryTest.java`(`@DataJpaTest`): `R_LINK_16_new_link_is_unread`, `R_LINK_15_read_state_persists`
- [ ] T031 [P] [US3] `backend/src/test/java/dev/linkshelf/link/MarkReadIntegrationTest.java`(`@SpringBootTest`): `R_LINK_15_marking_twice_gives_same_result_and_keeps_other_fields`(SC-003), `R_LINK_15_can_mark_unread_again`, `R_LINK_16_save_response_has_read_false`, `R_LINK_16_list_items_have_read`, `R_LINK_17_unknown_id_changes_nothing`
- [ ] T032 [US3] `R_LINK_18`: 기존 `SaveLinkIntegrationTest`·`LinkControllerTest` 단언은 수정하지 않고 통과해야 한다. 저장 응답 `read`는 T031에서만 단언한다
- [ ] T033 [US3] Red 확인 → **Red 커밋**

### Implementation

- [ ] T034 [US3] `backend/src/main/java/dev/linkshelf/link/Link.java`: `@Column(name = "is_read", nullable = false) @ColumnDefault("false") private boolean read;` + `isRead()`, `markRead(boolean)` — data-model "필수 참/거짓, 기본값 false"
- [ ] T035 [US3] `backend/src/main/java/dev/linkshelf/link/LinkResponse.java`: record 구성요소 `boolean read` 추가(이름은 `read`, contracts 문서 동기화 절)
- [ ] T036 [P] [US3] `backend/src/main/java/dev/linkshelf/link/MarkReadRequest.java` record `(@NotNull Boolean read)`. T029 결과 문자열·숫자를 받아들이면 이 필드에만 엄격 해석 적용(전역 Jackson 설정은 바꾸지 않음, research R5)
- [ ] T037 [P] [US3] `backend/src/main/java/dev/linkshelf/link/LinkNotFoundException.java`(id 보관)
- [ ] T038 [US3] `backend/src/main/java/dev/linkshelf/link/LinkService.java`: `@Transactional LinkResponse markRead(long id, boolean read)` — 없으면 `LinkNotFoundException`
- [ ] T039 [US3] `backend/src/main/java/dev/linkshelf/link/LinkController.java`: `@PutMapping("/{id}/read")` `markRead(@PathVariable long id, @Valid @RequestBody MarkReadRequest)`
- [ ] T040 [US3] `backend/src/main/java/dev/linkshelf/common/ApiExceptionHandler.java`: 404 `detail` `링크를 찾을 수 없습니다`, 확장 필드 `id`
- [ ] T041 [US3] `specs/001-save-link/contracts/save-link.md`: 201 예시에 `"read": false` 추가 + 002 계약 참조 개정 표기
- [ ] T042 [US3] Green: 전체 PASS → 커밋

### 검증

- [ ] T043 [US3] quickstart §4 기존 데이터 복사본 기동: 층 1 기동 로그(ALTER 문장, 예외 없음), 층 2 H2 Shell로 `is_read` DEFAULT FALSE NOT NULL·기존 행 false, 층 3 `GET` 기존 링크 `read:false`. 대조: `@ColumnDefault` 뺀 엔티티로 새 복사본 기동 시 실패가 드러나는지. 결과를 PR 검증 표에 층별로 기록(SC-004)
- [ ] T044 [US3] 고장 내기: 서비스에서 `markRead` 저장 경로 제거(값만 응답) → T031 FAIL 확인 후 편집으로 되돌림
- [ ] T045 [US3] PR → CI → pr-reviewer → 분류 코멘트

---

## Phase 6: Polish

- [ ] T046 각 PR 병합 후 `docs/ai/handoff.md`·`plans/mvp1-status.md` 갱신, 브랜치 정리(플레이북 규칙 20)
- [ ] T047 US3 병합 후 R-LINK-09~18 ↔ 테스트 이름 표를 spec 002 또는 마지막 PR에 남긴다(SC-006)

## Dependencies & Execution Order

- US1 → US2 → US3 순서. 각 스토리는 이전 스토리 PR이 **병합된 main**에서 시작한다.
- 스토리 안에서는 Tests → Red 커밋 → Implementation → Green → 검증 → PR.
- US3의 T028(DB 백업)은 US3의 어떤 코드 실행보다 먼저 한다.

### Parallel Opportunities

- 스토리 안의 테스트 파일 3개(Controller/Repository/Integration)는 [P]로 동시에 쓸 수 있다.
- US3의 `MarkReadRequest`·`LinkNotFoundException`은 [P].
- 스토리 사이 병렬은 없다(같은 컨트롤러·서비스·응답 DTO).

## Implementation Strategy

- MVP는 US1(목록)이다. US1만 병합돼도 저장한 링크를 다시 볼 수 있다.
- 각 스토리 PR은 독립적으로 검증되고 병합된다(constitution V).
