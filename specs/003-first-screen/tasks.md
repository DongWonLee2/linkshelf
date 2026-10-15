# Tasks: 첫 화면 (링크 저장·목록·태그 필터·읽음)

**Input**: `specs/003-first-screen/` — spec.md(clarify 완료), plan.md, research.md, quickstart.md, [Feature Map](../../docs/feature-map.md)
**기준 SHA**: `f1609fa`

**Tests**: 필수(constitution II). B2가 Feature Map 기준 E2E를 먼저 쓰고 로컬에서 Red를 확인·커밋한다. E2E 이름은 [plan 표](plan.md#e2e-테스트-이름--r-ui-sc-002)를 따른다.

## Format: `[ID] [P?] [Story] Description`

- **[P]** 병렬 가능. **[Story]** US1 목록, US2 저장, US3 필터, US4 읽음
- 경로: `frontend/src/`(A), `frontend/e2e/`(B2), `.github/workflows/`·`frontend/playwright.config.ts`(B1)

## Issue

| Issue | 제목 | 포함 task | 담당 | 독립 검증 |
|---|---|---|---|---|
| [#21](https://github.com/DongWonLee2/linkshelf/issues/21) | ci: frontend-e2e job과 Playwright 설정 보강 | T001~T007 | 오케스트레이터(작아서 직접) | 기존 smoke가 CI `frontend-e2e`에서 PASS |
| [#22](https://github.com/DongWonLee2/linkshelf/issues/22) | feat: 첫 화면 구현(목록·저장·필터·읽음) | T008~T019 | implementer A(worktree) | B2의 기능 E2E가 로컬에서 PASS, `typecheck`·`build` PASS |
| [#23](https://github.com/DongWonLee2/linkshelf/issues/23) | test: 첫 화면 E2E와 Visual QA | T020~T031 | implementer B2(worktree) | Red 커밋 → A 병합 후 rebase → CI PASS(visual은 기준 이미지 절차) |

- 순서: B1 병합 → A ∥ B2 동시 시작 → A PR 병합 → B2 rebase 후 PR → 기준 이미지 커밋 → 병합.
- 공유 파일(`docs/ai/handoff.md`, 가이드 `plans/*`, `docs/feature-map.md`)은 오케스트레이터만 고친다. testid를 바꿔야 하면 implementer는 멈추고 보고한다.

---

## Phase 1: B1 — CI와 설정 (먼저)

- [ ] T001 `.github/workflows/ci.yml`에 `frontend-e2e` job: `runs-on: ubuntu-24.04`, `timeout-minutes: 20`, setup-java temurin 21(`cache: gradle`), setup-node 22(`cache: npm`, `cache-dependency-path: frontend/package-lock.json`)
- [ ] T002 같은 job: `npm ci`(working-directory `frontend`) → `npx playwright install --with-deps chromium` → `./gradlew classes`(working-directory `backend`) → `npx playwright test`
- [ ] T003 같은 job: 실패해도(`if: ${{ !cancelled() }}`) `frontend/playwright-report/`, `frontend/test-results/`, `frontend/e2e/**/*-snapshots/**`를 아티팩트로 올림(`retention-days: 7`)
- [ ] T004 `workflow_dispatch` 입력 `update_snapshots`(boolean): 켜지면 `npx playwright test --update-snapshots=changed`
- [ ] T005 `frontend/playwright.config.ts`: `globalTimeout: 600_000`, `use.timezoneId: 'Asia/Seoul'`, `use.locale: 'ko-KR'`, viewport 고정(1280×800)
- [ ] T006 검증: PR CI에서 `frontend-e2e` PASS(smoke 1개), `backend-test` PASS. 실행 시간 기록
- [ ] T007 pr-reviewer → 분류 → 사용자 병합. 병합 후 main에서 첫 PASS 확인 → 필수 검사 추가 승인 질문(advisor 초안)

## Phase 2: A — 화면 (implementer, `frontend/src/`만)

- [ ] T008 [US1] `frontend/src/api.ts`: `listLinks({page, tag})`, `saveLink({url, memo, tags})`, `markRead(id, read)`. 400 `errors[]`, 409, 404, 네트워크 실패를 구분해 돌려준다. 빈·공백 tag는 보내지 않는다(R-UI-07)
- [ ] T009 [US1] `frontend/src/LinkList.tsx`: `link-list`, `link-item`(`data-link-id`), `link-item-url`(새 탭, `rel="noopener noreferrer"`), `link-item-memo`(없으면 생략), `link-item-tag`, `link-item-created-at`(`YYYY-MM-DD HH:mm`, 지역 시간), `link-list-empty`, `link-list-prev`/`next`(첫·마지막 쪽 disabled), `link-list-page-info`(빈 목록이면 숨김), `list-error`
- [ ] T010 [US2] `frontend/src/LinkForm.tsx`: `link-form`, `link-url-input`, `link-memo-input`, `link-tags-input`(쉼표 분리, 앞뒤 공백 제거, 빈 조각 버림), `link-save-button`(요청 중 disabled), `link-form-error`(`data-field`), `link-form-duplicate`, `link-form-saved`(다음 입력·저장까지 유지). 실패 시 입력값 유지, 성공 시 폼 비움
- [ ] T011 [US3] `frontend/src/TagFilter.tsx`: `tag-filter-input`, `tag-filter-apply`(빈·공백이면 해제), `tag-filter-clear`, `tag-filter-active`, `tag-filter-error`(400이면 이전 목록·필터 유지)
- [ ] T012 [US4] `LinkList.tsx` 체크박스 `link-item-read-toggle`: 먼저 바꾸고 `markRead`, 성공 시 서버 값으로, 실패 시 되돌리고 `link-item-error`
- [ ] T013 `frontend/src/App.tsx`: 상태(쪽, 필터), 저장 성공 시 필터 해제 + 첫 쪽 다시 불러오기(R-UI-05), 필터 적용·해제 시 첫 쪽
- [ ] T014 `frontend/src/styles.css`: 최소 스타일, 긴 URL·메모 줄바꿈(가로 스크롤 없음)
- [ ] T015 `npm run typecheck`, `npm run build` PASS
- [ ] T016 B2의 Red 커밋이 있으면 그 브랜치를 받아 로컬에서 기능 E2E를 돌려 PASS 확인(B2 파일은 고치지 않음). 없으면 smoke + 수동 확인
- [ ] T017 고장 내기(SC-003): 정렬 뒤집기, 오류 표시 제거, 실패 시 되돌림 제거 → 해당 E2E FAIL 확인 후 편집으로 되돌림(`git checkout` 금지)
- [ ] T018 PR(`Closes #N`, R-UI ↔ 화면 요소 표, 검증 표) → CI(`frontend-e2e`는 smoke만 돎) PASS
- [ ] T019 pr-reviewer → 분류 → 수용 수정 → 사용자 병합

## Phase 3: B2 — E2E와 Visual QA (implementer, `frontend/e2e/`만)

- [ ] T020 [P] `frontend/e2e/list.spec.ts`: R-UI-01, 02, 03(빈 목록은 없는 태그로, 목록 실패는 `page.route`로 GET만 실패)
- [ ] T021 [P] `frontend/e2e/save.spec.ts`: R-UI-04, 05(필터 해제 포함), 06(`data-field` 별 오류, 중복)
- [ ] T022 [P] `frontend/e2e/filter.spec.ts`: R-UI-07, 08
- [ ] T023 [P] `frontend/e2e/read.spec.ts`: R-UI-09, 10(PUT만 `page.route`로 실패)
- [ ] T024 `frontend/e2e/flow.spec.ts`: `E2E_SC_001_full_flow`, `E2E_R_UI_12_backend_uses_in_memory_db`
- [ ] T025 `frontend/e2e/visual.spec.ts`: `VISUAL_R_UI_13_*` 3개. `page.route('/api/links*')` 고정 JSON(spec 001·002 계약 예시에서), `test.skip(process.platform !== 'linux')`, 캡처 전 `page.mouse.move(-1, -1)`. `node-env.d.ts`에 `platform` 선언 추가
- [ ] T026 데이터 격리: 모든 기능 E2E는 고유 URL(`https://e2e.test/<테스트 이름>/<Date.now()>`), DB가 비었다고 가정하지 않음
- [ ] T027 Red 확인(화면 없음 → 기능 E2E FAIL, visual SKIP on macOS) → **Red 커밋, push**(worktree 임시)
- [ ] T028 A 병합 후 main에 rebase → 로컬 기능 E2E PASS
- [ ] T029 PR 생성. 본문에 "visual 3개는 기준 이미지 없음으로 예상 FAIL" 명시 → 첫 CI
- [ ] T030 기준 이미지: `gh run download` → `*-chromium-linux.png`만인지 확인 → 커밋 → CI PASS
- [ ] T031 pr-reviewer → 분류 → 사용자 병합(이미지 diff 확인)

## Phase 4: 마무리 (오케스트레이터)

- [ ] T032 worktree 하네스 검증 기록(조언 17:50 4번): implementer가 별도 worktree에서 돌았는지, 끝난 뒤 정리 시점, 메인 폴더 `git status`
- [ ] T033 `frontend-e2e` 필수 검사 승인(T007에서 안 했으면)
- [ ] T034 spec 003 추적 표(R-UI ↔ E2E, SC 근거), handoff, status, `docs/ai/result.md` MVP-3 결과(시간, 질문 수, 병렬 효과)

## Dependencies

- B1 → (A ∥ B2) → A 병합 → B2 rebase·PR → 병합 → 마무리
- A와 B2는 Feature Map testid가 계약이다. 어긋나면 B2 rebase 뒤 CI에서 드러난다.
