# Tasks: 안 읽은 것만 보기 필터

**Input**: `specs/005-unread-filter/` (spec, plan, research, data-model, quickstart, contracts) · 기준 SHA `b15718f`

**Issue / PR**: Issue #49(2026-10-22 승인·생성), PR 하나, 브랜치 `feat/49-unread-filter`

**Tests**: 필수(constitution II). R-ID마다 Red 커밋(테스트만, FAIL 확인) → Green 커밋. 테스트 이름은 `R_LINK_NN_…` / `E2E_R_UI_NN_…`. 경계는 ADR-0003(plan "테스트 경계").

**공통 함정**: 테스트 DB와 E2E 백엔드는 다른 테스트의 링크를 함께 담는다. "안 읽은 것만"의 개수·순서 단언은 **고유 태그로 거른 결과**에 한다(R-UI-12). 경쟁 E2E는 `waitForResponse` 같은 대기로 경쟁을 피하지 않는다(research R5).

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

- [x] T001 승인 게이트: 사용자에게 Issue 초안(제목·본문·받아들임 기준·라벨)과 browse-links 개정안(`specs/005-unread-filter/contracts/unread-filter.md`)을 보이고 승인받는다. 승인 전에는 아래 작업을 하지 않는다. 승인 말을 `plan.md` 승인 기록과 `contracts/unread-filter.md` 상태 줄에 사람의 말로 적는다
- [x] T002 브랜치 `feat/49-unread-filter` 생성, 메인 체크아웃 로컬 `docs/prd.md`에 PRD-06 줄 추가(커밋 안 함). 기준 확인: `cd backend && ./gradlew test` 전체 PASS 개수, `cd frontend && npx playwright test` PASS·SKIP 개수와 exit를 기록

## Phase 1b: Prefactor (동작 변화 없음, 별도 커밋)

- [x] T003 `frontend/src/App.tsx`: 목록 요청 조건(태그·정렬)을 "마지막으로 고른 조건" 하나(`requested` ref, `{ tag, order }`)로 모은다. 요청을 만드는 모든 경로(첫 불러오기, 쪽 넘김, 필터 적용·해제, 정렬 변경, 저장 뒤)는 바뀌지 않는 조건을 이 묶음에서 읽는다. 실패 때 규칙은 지금과 같다: 태그는 화면에 적용된 필터로 돌아가고(`App.tsx:42`, R-UI-08), 정렬은 고른 값을 유지한다. 출처: 비교 흐름 B 티켓 01(사용자 판정 2026-10-22). 증거: 이 커밋만으로 `cd frontend && npx playwright test` 전체 PASS(T002 기준과 같은 PASS·SKIP 개수)와 `cd backend && ./gradlew test` PASS를 `.audit/`에 남긴다. #46 경로(필터 400 진행 중 정렬 변경)의 마지막 요청 쿼리와 화면 표시(`list-error`/`tag-filter-error`)가 prefactor 전후로 같은지 `page.route` 지연으로 일회성 확인(커밋 안 함). **다르면 멈추고 사용자에게 보고한다**(고칠지·테스트를 더할지는 사용자 결정)

## Phase 2: Foundational

- [x] T004 계약 반영: `specs/002-browse-links/contracts/browse-links.md`에 개정 표기, 쿼리 표 `unread` 행(`true` 또는 `false`, 정확히 같을 때만 … `yes`·`on`·`1` … 빈 값은 400), 200 설명(거른 결과만 셈), 400 `field` 목록에 `unread`·메시지 `true 또는 false여야 합니다`, 호환성 줄. `contracts/unread-filter.md` 상태를 "승인됨, 반영됨"으로. 계약 커밋
- [x] T005 testid 계약을 먼저 고친다: `.claude/skills/verify-linkshelf/features/README.md` 표에 `PRD-06 안 읽음 필터 | "안 읽은 것만" 체크박스(기본 꺼짐, 새로 고치면 꺼짐) | link-unread-only-toggle | GET /api/links?unread= | E2E_R_UI_22_toggle_reloads_first_page_keeping_tag_and_order` 줄 추가, 기준 줄에 PRD-06과 spec 005 추가. 기능 목록의 `unread-filter` 링크는 T026에서 더한다

**Checkpoint**: 계약(T004)과 testid 표(T005)가 커밋되면 US1(백엔드)과 US2·US3(화면)을 병렬로 진행할 수 있다. 고칠 파일이 겹치지 않는다.

## Phase 3: User Story 1 - 안 읽은 링크만 받기 (P1) 🎯 MVP

**Goal**: `GET /api/links?unread=true|false`(R-LINK-22~24)

**Independent Test**: quickstart §1·§2

### Tests (Red 먼저) ⚠️

- [x] T006 [P] [US1] `backend/src/test/java/dev/linkshelf/link/UnreadFilterRepositoryTest.java` (`@DataJpaTest`): `R_LINK_22_unread_only_excludes_read_links`, `R_LINK_22_without_unread_matches_existing_paths`(조건 없음 = 기존 `findPage` 결과), `R_LINK_23_unread_with_tag_filters_both_and_counts_filtered_only`, `R_LINK_23_unread_with_oldest_order`, `R_LINK_23_unread_pages_without_overlap_or_gap`, `R_LINK_23_unread_with_huge_page_is_empty`. research R2 가정(`:tag is null` 비교)이 H2에서 동작하는지 이 테스트로 고정
- [x] T007 [P] [US1] `backend/src/test/java/dev/linkshelf/link/UnreadFilterControllerTest.java` (`@WebMvcTest`): `R_LINK_22_passes_unread_to_service`(생략 → false, `false`, `true`), `R_LINK_24_rejects_other_boolean_spellings_400`(`yes`, `on`, `1`, `TRUE`, `" true"` — 사용자 보충 "yes/on/1도 true로 바꾼다"를 고정), `R_LINK_24_rejects_empty_unread_400`(`unread=`), `R_LINK_24_rejects_repeated_unread_400`(`unread=true&unread=false`), `R_LINK_24_reports_unread_with_other_errors_together`(`unread=1&order=bad&page=-1` → `errors`에 세 field, 순서 무관), `R_LINK_24_error_message_matches_contract`(`"true 또는 false여야 합니다"`, `detail` `Invalid request parameters.`)
- [x] T008 [P] [US1] `backend/src/test/java/dev/linkshelf/link/UnreadFilterIntegrationTest.java` (`@SpringBootTest`, 2개): `R_LINK_22_omitted_unread_response_equals_false_response`(본문 문자열 동일), `R_LINK_23_save_mark_read_then_unread_only_lists_rest_end_to_end`(고유 태그로 거름, `PUT /read` 뒤 `unread=true&tag=…`)
- [x] T009 [US1] Red 커밋: T006~T007만 커밋하고 `./gradlew test` FAIL(컴파일 실패 포함) 출력을 `.audit/`에 남긴다

### Implementation

- [x] T010 [US1] `backend/src/main/java/dev/linkshelf/link/LinkRepository.java`: `findByTag`/`countByTag`를 `findFiltered(@Param("tag") String tag, @Param("unreadOnly") boolean unreadOnly, Pageable)`/`countFiltered(…)`(JPQL `where (:tag is null or exists (select 1 from l.tags t where lower(t) = :tag)) and (:unreadOnly = false or l.read = false)`)로 바꾸고 `findPageByTag`를 `findPageFiltered(String tag, boolean unreadOnly, Sort, int page, int size)`로. 큰 쪽 번호 처리(`sortedPage`)는 그대로. H2가 `:tag is null`을 받지 못하면 research R2 대안 A(태그 있음/없음 두 쿼리). 기존 `TagFilterRepositoryTest`·`SortLinksRepositoryTest`가 부르는 메서드 이름이 바뀌면 호출만 고치고 기대값은 바꾸지 않는다(이유를 PR에)
- [x] T011 [US1] `backend/src/main/java/dev/linkshelf/link/LinkService.java`: `list(page, size, tag, LinkOrder order, boolean unreadOnly)`. 태그 없음·안 읽음 끔이면 기존 `findPage`, 아니면 `findPageFiltered(tag 정규화 또는 null, unreadOnly, …)`. 기존 컨트롤러 테스트의 `list(…)` 호출 인자 추종
- [x] T012 [US1] `backend/src/main/java/dev/linkshelf/link/LinkController.java`: `@RequestParam(required = false) @Pattern(regexp = "true|false", message = "true 또는 false여야 합니다") String unread`(`Boolean` 바인딩·`defaultValue` 쓰지 않음, research R1), `"true".equals(unread)`를 서비스에
- [x] T013 [US1] Green 커밋: `cd backend && ./gradlew test` 전체 PASS
- [ ] T014 [US1] 고장 내기(quickstart §5 1·2행): 저장소 조건을 `l.read = true`로, 컨트롤러를 `Boolean unread`로 → 해당 테스트 FAIL 확인 후 되돌려 PASS. 커밋하지 않음, 출력은 `.audit/`

## Phase 4: User Story 2 - 화면에서 안 읽은 것만 보기 (P2)

**Goal**: `link-unread-only-toggle`(R-UI-21~23, 25)

**Independent Test**: quickstart §3·§4

### Tests (Red 먼저) ⚠️

- [x] T015 [P] [US2] `frontend/e2e/unread.spec.ts`: `E2E_R_UI_21_toggle_defaults_off_and_resets_on_reload`, `E2E_R_UI_22_toggle_reloads_first_page_keeping_tag_and_order`(고유 태그 링크 21개 이상, 일부 읽음 → 2쪽·오래된순·태그에서 켬 → 1쪽, 읽음 없음, `tag-filter-active`·`link-sort-select` 유지, 요청 쿼리 `unread=true`·`tag`·`order` 단언), `E2E_R_UI_22_paging_tag_and_order_keep_unread`, `E2E_R_UI_22_toggle_does_not_move_focus_or_scroll`, `E2E_R_UI_23_saving_turns_unread_off`(저장 뒤 체크 해제·첫 항목이 새 링크·요청에 `unread` 없음 또는 `false`), `E2E_R_UI_25_marking_read_keeps_item_until_reload`, `E2E_R_UI_25_page_shift_after_marking_read`(research R6: 안 읽은 링크 21개, 1쪽 하나 읽음 → 다음 쪽이 빈 목록 안내·쪽 넘김 숨김(clarify A) → 껐다 켜면 그 항목이 1쪽에 보임), `E2E_R_UI_22_list_error_keeps_chosen_toggle`(목록 500 → `list-error`, 체크 유지, 다시 누르면 반대 값으로 요청), `E2E_R_UI_22_empty_result_shows_empty_notice`(모두 읽음인 고유 태그 → `link-list-empty`). testid·역할로만 찾는다(R-UI-12). 필요하면 `frontend/e2e/support.ts`에 헬퍼(링크 N개 저장, 읽음 지정)를 더한다
- [x] T016 [US2] Red 커밋: T014만 커밋, `npx playwright test e2e/unread.spec.ts` FAIL 출력 기록

### Implementation

- [ ] T017 [P] [US2] `frontend/src/api.ts`: `listLinks({ page, tag, order, unread })`, `unread`가 참이면 `unread=true`만 보낸다(거짓이면 보내지 않아 기존 요청과 같음)
- [ ] T018 [P] [US2] `frontend/src/UnreadToggle.tsx`: `<label><input type="checkbox" data-testid="link-unread-only-toggle">안 읽은 것만</label>`, 요청 중에도 비활성으로 두지 않는다(R-UI-21, `SortSelect`와 같은 이유)
- [ ] T019 [US2] `frontend/src/App.tsx`: `unread` 상태(기본 `false`), 토글 → `load(0, tag, order, next)`(스크롤·포커스 이동 없음), 쪽 넘김·필터 적용/해제·정렬 변경은 현재 `unread` 유지, `onSaved`에서 `false`(R-UI-23). 토글은 `.list-controls`의 `SortSelect` 옆. 이 단계에서는 US3의 ref 규칙을 아직 넣지 않아도 된다
- [ ] T020 [US2] Green 커밋: `npx playwright test` 전체 PASS(visual 기준 이미지 차이는 T020에서)
- [ ] T021 [US2] Visual 기준 이미지 갱신: 브랜치 push 뒤 CI `workflow_dispatch` `update_snapshots=true` → 아티팩트의 `frontend/e2e/visual.spec.ts-snapshots/` 커밋(research R7). 체크박스 추가 말고 다른 차이가 없는지 눈으로 확인
- [ ] T022 [US2] 고장 내기(quickstart §5 4행): 저장 뒤 `unread`를 끄지 않기 → `E2E_R_UI_23_…` FAIL 확인 후 되돌림. 커밋하지 않음

## Phase 5: User Story 3 - 겹친 조작에도 고른 조건대로 보기 (P3)

**Goal**: R-UI-24(경쟁), 범위 밖: 태그 400 진행 중 토글(#46)

**Independent Test**: `E2E_R_UI_24_` 3개가 지연으로 실제로 겹친 상태에서 PASS, 고장 내기에서 3개 모두 FAIL

### Tests (Red 먼저) ⚠️

- [ ] T023 [US3] `frontend/e2e/unread.spec.ts`에 추가(research R5, `page.route`로 해당 요청만 800ms 지연, 대기 없이 다음 조작): `E2E_R_UI_24_toggle_right_after_tag_apply_or_clear_keeps_new_tag`(태그 적용 지연 중 켬 → 마지막 요청에 새 `tag`와 `unread=true`, 화면 `tag-filter-active`와 결과 일치. 해제도 같은 테스트에서), `E2E_R_UI_24_order_change_right_after_toggle_keeps_unread`(켬 지연 중 정렬 변경 → 마지막 요청에 `unread=true`와 새 `order`, 체크 유지), `E2E_R_UI_24_toggle_right_after_save_keeps_filters_cleared`(켠 상태에서 저장 → 다시 불러오기 지연 중 다시 켬 → 마지막 요청은 `tag` 없음·`newest`·`unread=true`). 태그 400 진행 중 토글은 쓰지 않는다(#46). 각 테스트 위 주석에 "대기를 넣어 통과시키지 않는다(registry 2026-10-22)"
- [ ] T024 [US3] Red 커밋: T022만 커밋, FAIL 출력 기록(T019 구현에서 이미 통과하는 테스트가 있으면 그 사실과 이유를 적고, 고장 내기 T025로 테스트가 경쟁을 잡는지 확인)

### Implementation

- [ ] T025 [US3] `frontend/src/App.tsx`: T003의 요청 조건 묶음(`requested`)에 `unread`를 더하고 `load(page, tag, order, unread, fromFilter)`가 요청 직전에 묶음을 갱신. 요청을 만드는 모든 경로(토글, 태그 적용·해제, 정렬, 쪽 넘김)는 바뀌지 않는 조건을 묶음에서 읽는다(research R4). 실패해도 묶음의 정렬·안 읽음 값은 되돌리지 않는다(R-UI-24). 태그 400 경로의 `requestedTag.current = shownTag.current`는 그대로(#46). Green 커밋: `npx playwright test` 전체 PASS
- [ ] T026 [US3] 고장 내기(quickstart §5 3행, 사용자 보충): 새 요청이 `requested*` 대신 직전에 성공한 값(`shown*`, 렌더 시점 state)을 읽게 바꿔 `E2E_R_UI_24_` 3개 모두 FAIL 확인 후 되돌림. 하나라도 PASS면 그 테스트의 지연·단언을 고친다(대기 추가 금지). 커밋하지 않음

## Phase 6: Polish & Cross-Cutting

- [ ] T027 [P] `.claude/skills/verify-linkshelf/features/unread-filter.md`(하위 기능 `unread-only`, `unread-keeps-tag-and-order`, `unread-off-on-save`, 증거 파일 이름) + `.claude/skills/verify-linkshelf/drive/unread-filter.verify.ts` + `features/README.md` 기능 목록에 링크. CI 타입 검사 PASS
- [ ] T028 verify-linkshelf로 실제 앱 한 바퀴(`drive.sh unread-filter`) PASS, 증거 경로 기록
- [ ] T029 [P] spec 시퀀스 그림 1장: `specs/005-unread-filter/diagrams/` (diagram-design, 화면 토글 → `GET /api/links?unread&tag&order` → 거르기 쿼리, 경쟁 시 `requested*` 읽기, 근거 커밋 표기, SVG export 뒤 `@import` 규칙 제거·XML 파싱 확인). 아키텍처 그림은 바뀌지 않아 다시 그리지 않음(PR에 이유)
- [ ] T030 spec 005에 추적 표(R-ID ↔ Issue ↔ 테스트 이름) 추가, Status를 Implemented로
- [ ] T031 cleanup → blast-radius(공개 API 계약, 근거를 실행으로 증명) → PR(`Closes #49`, R-ID↔테스트 표, 검증 표, 고장 내기 4종, verify 증거, CI run, blast-radius 요약, 기준 SHA, 기존 테스트 호출 수정 이유) → CI → pr-reviewer → 지적 반영

## Dependencies & Execution Order

- T001(승인) → T002 → T003(prefactor, 단독 커밋·기존 E2E 전부 PASS) → T004 → T005 → {US1: T006~T014} ∥ {US2: T015~T019} → T020 → T021(push 필요) → T022 → {US3: T023~T026} → T027~T030 → T031
- US2·US3의 E2E는 실제 백엔드로 돈다. US2 Green(T020)은 US1 Green(T013)이 같은 브랜치에 합쳐진 뒤에 확인한다. Red(T015·T016)와 화면 코드(T017~T019)는 그 전에 병렬로 진행할 수 있다.
- US3은 같은 `App.tsx`를 고치므로 US2(T019) 뒤에 한다.

## Parallel Example

```text
# Checkpoint(T005) 뒤:
implementer A (backend/): T006 ∥ T007 ∥ T008 → T009 → T010 → T011 → T012 → T013 → T014
implementer B (frontend/, features/, drive/): T015 → T016 → T017 ∥ T018 → T019
# 합친 뒤: T020 → T021 → T022 → T023~T026 → T027 ∥ T029 → T028 → T030 → T031
```

## Implementation Strategy

- MVP: US1(API만)으로 `unread=true` 요청/응답을 확인할 수 있다. 화면 사용자 가치는 US2에서 생긴다. PR은 US1~US3을 합친 하나다(spec 004와 같음).
- US3을 US2와 나눈 이유: 경쟁 테스트를 Red로 먼저 보고, 고장 내기로 "직전에 성공한 값" 회귀를 잡는지 따로 확인하기 위해서다.
