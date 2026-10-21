# Tasks: 링크 목록 정렬 옵션

**Input**: `specs/004-sort-links/` (spec, plan, research, data-model, quickstart, contracts) · 계약 커밋 `65859e0`

**Issue / PR**: #43 하나, PR 하나, 브랜치 `feat/43-sort-links`

**Tests**: 필수(constitution II). R-ID마다 Red 커밋(테스트만, FAIL 확인) → Green 커밋. 테스트 이름은 `R_LINK_NN_…` / `E2E_R_UI_NN_…`.

**공통 함정**: 테스트 DB와 E2E 백엔드는 다른 테스트의 링크를 함께 담는다. 오래된순의 첫 항목은 "가장 오래된 전체 링크"라서 개수·순서 단언은 **고유 태그로 거른 결과**에 한다(spec 003 R-UI-12와 같은 원칙).

## Format: `[ID] [P?] [Story] Description`

## Phase 1: Setup

- [x] T001 기준 확인: backend `./gradlew test` 122 PASS @ `65859e0`, frontend `npx playwright test` 29 PASS · 3 SKIP(visual, 로컬은 Linux 기준 이미지 없음) exit 0 @ `c85da98`

## Phase 2: Foundational

- [x] T002 testid 계약을 먼저 고친다: `.claude/skills/verify-linkshelf/features/README.md` 표 PRD-02 목록 영역에 `PRD-05 정렬 | 정렬 선택 상자(최신순 기본·오래된순) | link-sort-select | GET /api/links?order= | E2E_R_UI_19_changing_order_reloads_first_page_keeping_filter` 줄 추가, 기준 줄에 PRD-05와 spec 004 추가. 기능 목록의 `sort-links` 링크는 파일을 만드는 T021에서 더한다

**Checkpoint**: 계약(`65859e0`)과 testid 표(T002)가 커밋되면 US1(백엔드)과 US2(화면)를 병렬로 진행할 수 있다. 고칠 파일이 겹치지 않는다.

## Phase 3: User Story 1 - 오래된순으로 목록 보기 (P1) 🎯 MVP

**Goal**: `GET /api/links?order=newest|oldest`(R-LINK-19~21)

**Independent Test**: quickstart §1·§2

### Tests (Red 먼저) ⚠️

- [x] T003 [P] [US1] `backend/src/test/java/dev/linkshelf/link/SortLinksRepositoryTest.java` (`@DataJpaTest`): `R_LINK_19_oldest_orders_by_created_at_asc_then_id_asc`(같은 저장 시각 동률 포함), `R_LINK_19_newest_matches_existing_order`, `R_LINK_20_oldest_pages_without_overlap_or_gap`, `R_LINK_20_oldest_with_tag_filters_then_sorts_and_counts_filtered_only`
- [x] T004 [P] [US1] `backend/src/test/java/dev/linkshelf/link/SortLinksControllerTest.java` (`@WebMvcTest`): `R_LINK_19_passes_order_to_service`(생략 → NEWEST, `newest`, `oldest`), `R_LINK_21_rejects_unknown_order_400`(`bad`, `NEWEST`, `" newest"`, `newest,oldest` 두 번 보냄), `R_LINK_21_rejects_empty_order_400`(`order=`, research R1 가정 고정), `R_LINK_21_reports_order_with_page_and_size_together`(`order=bad&page=-1&size=0` → `errors`에 세 field, 순서 무관), `R_LINK_21_error_message_matches_contract`(`"newest 또는 oldest여야 합니다"`, `detail` `Invalid request parameters.`)
- [x] T005 [P] [US1] `backend/src/test/java/dev/linkshelf/link/SortLinksIntegrationTest.java` (`@SpringBootTest`): `R_LINK_19_omitted_order_response_equals_newest_response`(본문 문자열 동일), `R_LINK_19_oldest_lists_saved_order_end_to_end`(고유 태그로 거름), `R_LINK_20_oldest_with_huge_page_is_empty_not_server_error`
- [x] T006 [US1] Red 커밋: T003~T005만 커밋하고 `./gradlew test` FAIL(컴파일 실패 포함) 출력을 `.audit/`에 남긴다

### Implementation

- [x] T007 [US1] `backend/src/main/java/dev/linkshelf/link/LinkOrder.java` 새 enum `NEWEST`, `OLDEST`
- [x] T008 [US1] `backend/src/main/java/dev/linkshelf/link/LinkRepository.java`: `OLDEST_FIRST = Sort.by(asc("createdAt"), asc("id"))`, `findNewestFirst`/`findNewestFirstByTag`를 `Sort`를 받는 조회로 바꾸되 큰 쪽 번호 처리(`offset > Integer.MAX_VALUE` → 빈 쪽)는 그대로. 기존 `ListLinksRepositoryTest`·`TagFilterRepositoryTest`가 부르는 메서드 이름이 바뀌면 호출만 고치고 기대값은 바꾸지 않는다(바꾼 이유를 PR에 적음)
- [x] T009 [US1] `backend/src/main/java/dev/linkshelf/link/LinkService.java`: `list(page, size, tag, LinkOrder)`
- [x] T010 [US1] `backend/src/main/java/dev/linkshelf/link/LinkController.java`: `@RequestParam(required = false) @Pattern(regexp = "newest|oldest", message = "newest 또는 oldest여야 합니다") String order` (`defaultValue` 쓰지 않음, research R1), null → `NEWEST`
- [x] T011 [US1] Green 커밋: `cd backend && ./gradlew test` 전체 PASS
- [x] T012 [US1] 고장 내기(quickstart §5 1·2행): `OLDEST_FIRST` 방향, 동률 id 방향을 뒤집어 FAIL 확인 후 되돌려 PASS. 커밋하지 않음

## Phase 4: User Story 2 - 화면에서 정렬 고르기 (P2)

**Goal**: `link-sort-select`(R-UI-18~20)

**Independent Test**: quickstart §3·§4

### Tests (Red 먼저) ⚠️

- [x] T013 [P] [US2] `frontend/e2e/sort.spec.ts`: `E2E_R_UI_18_sort_select_defaults_to_newest_and_resets_on_reload`, `E2E_R_UI_19_changing_order_reloads_first_page_keeping_filter`(고유 태그 링크 21개 이상 → 2쪽에서 오래된순 → 1쪽·첫 항목이 가장 먼저 저장한 것·`tag-filter-active` 유지), `E2E_R_UI_19_paging_and_filter_keep_order`, `E2E_R_UI_19_order_change_does_not_move_focus_or_scroll`, `E2E_R_UI_20_saving_resets_order_to_newest`(저장 뒤 선택 상자 값 최신순·첫 항목이 새 링크·다음 쪽도 최신순). testid·역할로만 찾는다(R-UI-12). 필요하면 `frontend/e2e/support.ts`에 헬퍼를 더한다
- [x] T014 [US2] Red 커밋: T013만 커밋, `npx playwright test e2e/sort.spec.ts` FAIL 출력 기록

### Implementation

- [x] T015 [P] [US2] `frontend/src/api.ts`: `listLinks({ page, tag, order })`, `order`는 `'newest' | 'oldest'`, 항상 보낸다
- [x] T016 [P] [US2] `frontend/src/SortSelect.tsx`: `<label>정렬<select data-testid="link-sort-select">` 옵션 "최신순"(`newest`)·"오래된순"(`oldest`), 요청 중에도 비활성으로 두지 않는다(Chromium은 비활성이 된 요소에서 포커스를 빼서 R-UI-19를 어긴다. 마지막 요청만 화면에 반영하는 `latest` 가드가 있어 안전. 구현 중 변경, implementer 실측)
- [x] T017 [US2] `frontend/src/App.tsx`: `order` 상태(기본 `newest`), 정렬 변경 → `load(0, tag, order)`(스크롤·포커스 이동 없음), 쪽 넘김·필터 적용/해제는 현재 `order` 유지, `onSaved`에서 `newest`로 되돌림(R-UI-20). 선택 상자는 `.browse` 영역의 필터 옆
- [x] T018 [US2] Green 커밋: `npx playwright test` 전체 PASS(visual 제외 기준 이미지 차이는 T019에서)
- [ ] T019 [US2] Visual 기준 이미지 갱신: 브랜치 push 뒤 CI `workflow_dispatch` `update_snapshots=true` → 아티팩트의 `frontend/e2e/visual.spec.ts-snapshots/` 커밋(research R5). 바뀐 이미지가 선택 상자 추가 말고 다른 차이가 없는지 눈으로 확인
- [x] T020 [US2] 고장 내기(quickstart §5 3·4행): 정렬 변경 시 `order` 안 보내기, 저장 뒤 되돌리지 않기 → 해당 E2E FAIL 확인 후 되돌림. 커밋하지 않음

## Phase 5: Polish & Cross-Cutting

- [x] T021 [P] `.claude/skills/verify-linkshelf/features/sort-links.md`(하위 기능 `sort-oldest`, `sort-keeps-filter`, `sort-reset-on-save`, 증거 파일 이름) + `.claude/skills/verify-linkshelf/drive/sort-links.verify.ts` + `features/README.md` 기능 목록에 링크. CI 타입 검사(PR #42) PASS
- [ ] T022 verify-linkshelf로 실제 앱 한 바퀴(`drive.sh sort-links`) PASS, 증거 경로 기록
- [x] T023 [P] spec 시퀀스 그림 1장: `specs/004-sort-links/diagrams/` (diagram-design, 화면 → `GET /api/links?order` → 정렬 쿼리, 근거 커밋 표기, SVG export 뒤 `@import` 줄 제거). 아키텍처 그림은 바뀌지 않아 다시 그리지 않음(PR에 이유)
- [ ] T024 spec 004에 추적 표(R-ID ↔ PR ↔ 테스트 이름) 추가, Status를 Implemented로
- [ ] T025 cleanup → blast-radius(공개 API 계약, 근거를 실행으로 증명) → PR(`Closes #43`, R-ID↔테스트 표, 검증 표, 고장 내기, verify 증거, CI run, blast-radius 요약, 기준 SHA) → CI → pr-reviewer → 지적 반영

## Dependencies & Execution Order

- T001 → T002 → {US1: T003~T012} ∥ {US2: T013~T018} → T019(push 필요) → T020 → T021~T024 → T025
- US2의 E2E는 실제 백엔드로 돈다. US2 Green(T018)은 US1 Green(T011)이 같은 브랜치에 합쳐진 뒤에 확인한다. Red(T013·T014)와 화면 코드(T015~T017)는 그 전에 병렬로 진행할 수 있다.

## Parallel Example

```text
implementer A (worktree, backend/): T003 T004 T005 → T006 → T007~T011 → T012
implementer B (worktree, frontend/ + .claude/skills/verify-linkshelf/): T013 → T014 → T015 T016 → T017 → (A 합친 뒤) T018
오케스트레이터: T001, T002, 합치기, T019~T025
```

## Implementation Strategy

MVP는 US1(API만으로도 오래된순 조회 가능). 다만 PR은 하나이므로 US1·US2를 모두 끝낸 뒤 PR을 만든다.
