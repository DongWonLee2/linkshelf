# Feature Specification: 첫 화면 (링크 저장·목록·태그 필터·읽음)

**Feature Branch**: 스토리별 Issue·PR. spec 브랜치 `chore/spec-003-first-screen`

**Created**: 2026-10-15

**Status**: Draft

**Input**: User description: "한 화면에서 링크를 저장하고, 최신순 목록을 쪽 단위로 보고, 태그 하나로 거르고, 읽음을 표시한다." (출처: [PRD-01~04](../../docs/prd.md), 조언 00:40 MVP-3)

**Base SHA**: `f1609fa` (main, PR #20 병합 후)

## Clarifications

### Session 2026-10-15

질문 5개를 한 번에 묻고 한 번에 답을 받았다(조언 00:40 "clarify 한 번").

- Q: 태그 필터가 걸린 상태에서 저장하면 필터를 유지할까요? → A: 해제하고 전체 목록 첫 쪽을 보여 준다 (사용자 답 "추천", 이유 미기재)
- Q: 저장 시각 표시 형식은? → A: 사용자 지역 시간, `YYYY-MM-DD HH:mm`. E2E는 시간대를 고정한다 (사용자 답 "추천", 이유 미기재)
- Q: 저장 성공 표시는 언제 사라지나요? → A: 다음 입력을 시작하거나 다시 저장할 때까지 남는다(시간으로 사라지지 않음) (사용자 답 "추천", 이유 미기재)
- Q: 읽음 체크박스는 서버 응답 전에 바뀌나요? → A: 먼저 바꾸고, 실패하면 되돌리고 안내한다 (사용자 답 "추천", 이유 미기재)
- Q: Visual QA 대상 상태는? → A: 3가지 — 링크가 있는 첫 화면, 빈 목록, 저장 입력 오류 (사용자 답 "추천", 이유 미기재)

**화면 ↔ E2E 계약**: [docs/feature-map.md](../../docs/feature-map.md)의 `data-testid` 목록. 이 spec의 요구사항은 그 표의 요소를 기준으로 쓴다.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 저장한 링크 목록 보고 쪽 넘기기 (Priority: P1)

사용자가 화면을 열면 저장한 링크가 최신순으로 보인다. 각 항목에서 URL(눌러서 열 수 있음), 메모, 태그를 본다. 링크가 많으면 이전·다음 쪽으로 넘기고 지금 몇 쪽인지 본다. 링크가 없으면 비어 있다는 안내를 본다.

**Why this priority**: 저장한 것을 다시 보는 것이 제품의 핵심이다. 다른 스토리(저장·필터·읽음)의 결과도 목록에서 확인한다.

**Independent Test**: 링크 여러 개가 저장된 상태에서 화면을 열어 최신순으로 보이는지, 다음 쪽으로 넘어가는지 확인한다.

**Acceptance Scenarios**:

1. **Given** 링크 A, B, C를 이 순서로 저장함, **When** 화면을 엶, **Then** C, B, A 순서로 URL·메모·태그가 저장한 그대로 보인다(한글 포함).
2. **Given** 한 쪽보다 많은 링크가 있음, **When** 다음 쪽을 누름, **Then** 다음 링크들이 보이고 쪽 정보가 바뀐다. 첫 쪽에서는 이전 쪽을, 마지막 쪽에서는 다음 쪽을 누를 수 없다.
3. **Given** 보이는 링크가 없음, **When** 화면을 엶, **Then** 빈 목록 안내가 보이고 쪽 정보와 쪽 넘김은 보이지 않는다.
4. **Given** 목록을 불러오지 못함(서버가 꺼짐 등), **When** 화면을 엶, **Then** 불러오기 실패 안내가 보인다.

---

### User Story 2 - 링크 저장 (Priority: P1)

사용자가 URL과, 원하면 메모와 태그(쉼표로 구분)를 입력해 저장한다. 성공하면 저장됐다는 표시가 나오고 폼이 비워지며, 새 링크가 목록 맨 위에 보인다. 입력이 틀렸으면 어느 항목이 왜 틀렸는지 항목 옆에 보이고, 이미 저장된 URL이면 중복이라는 안내가 보인다.

**Why this priority**: 화면에서 링크를 모으지 못하면 화면의 가치가 반쯤 사라진다. 목록(US1)과 함께 최소 기능이다.

**Independent Test**: URL과 태그를 입력해 저장하고, 성공 표시와 함께 목록 맨 위에 새 링크가 보이는지 확인한다.

**Acceptance Scenarios**:

1. **Given** 화면이 열림, **When** URL·한글 메모·태그 `Java, 스프링`을 입력해 저장, **Then** 저장 성공 표시가 나오고 폼이 비워지며 목록 첫 항목이 그 링크다(태그 2개).
6. **Given** 태그 필터 `스프링`이 적용됨, **When** 태그 없는 링크를 저장, **Then** 필터가 해제되고 새 링크가 목록 맨 위에 보인다.
2. **Given** 화면이 열림, **When** `not a url`로 저장, **Then** URL 항목 옆에 오류가 보이고 아무것도 저장되지 않으며 입력한 값은 남아 있다.
3. **Given** URL 오류와 메모 길이 오류가 함께 있음, **When** 저장, **Then** 두 항목 모두 오류가 보인다.
4. **Given** `https://example.com/a`가 저장돼 있음, **When** `HTTPS://Example.COM:443/a`로 저장, **Then** 이미 저장된 링크라는 안내가 보이고 목록은 바뀌지 않는다.
5. **Given** 저장 요청 중, **When** 저장 버튼을 다시 누름, **Then** 요청이 두 번 가지 않는다.

---

### User Story 3 - 태그 하나로 거르기 (Priority: P2)

사용자가 태그를 입력하고 적용하면 그 태그가 붙은 링크만 보이고, 지금 어떤 태그로 걸렀는지 보인다. 해제하면 전체 목록으로 돌아간다. 입력이 비었거나 공백뿐이면 적용해도 해제와 같다.

**Why this priority**: 링크가 쌓인 뒤에 필요하다. 목록(US1)이 먼저 있어야 한다.

**Independent Test**: 태그가 다른 링크들이 있는 상태에서 태그를 대문자·공백을 섞어 입력해 적용하고, 그 태그 링크만 보이는지 확인한다.

**Acceptance Scenarios**:

1. **Given** 태그 `Java`, `java`, `스프링`이 붙은 링크들이 있음, **When** ` JAVA `로 적용, **Then** `Java`·`java` 링크만 최신순으로 보이고 현재 필터 표시가 보인다.
2. **Given** 필터가 적용됨, **When** 해제, **Then** 전체 목록이 첫 쪽부터 보이고 필터 표시가 사라진다.
3. **Given** 필터 입력이 비었거나 공백뿐, **When** 적용, **Then** 해제와 같다(오류 없음).
4. **Given** 31자 태그, **When** 적용, **Then** 필터 입력 오류가 보이고 목록은 바뀌지 않는다.
5. **Given** 어느 링크에도 없는 태그, **When** 적용, **Then** 빈 목록 안내가 보인다.
6. **Given** 필터가 적용된 상태에서 쪽을 넘김, **When** 다음 쪽, **Then** 같은 필터 안에서 넘어간다.

---

### User Story 4 - 읽음 표시 (Priority: P2)

사용자가 링크 옆 체크박스로 읽음/안 읽음을 바꾼다. 바꾼 상태는 화면을 새로 열어도 남는다. 바꾸기에 실패하면 체크 상태가 원래대로 돌아가고 실패 안내가 그 항목에 보인다.

**Why this priority**: 읽을거리 관리에 유용하지만, 목록이 있어야 의미가 있다.

**Independent Test**: 링크 하나를 읽음으로 체크하고 화면을 새로 연 뒤에도 체크돼 있는지 확인한다.

**Acceptance Scenarios**:

1. **Given** 안 읽은 링크, **When** 체크, **Then** 체크된 상태가 되고 화면을 새로 열어도 체크돼 있다.
2. **Given** 읽은 링크, **When** 체크 해제, **Then** 해제된 상태가 새로 열어도 남는다.
3. **Given** 바꾸기 요청이 실패함(링크가 다른 곳에서 사라짐, 서버 오류), **When** 체크, **Then** 체크 상태가 원래대로 돌아가고 그 항목에 실패 안내가 보인다.

---

### Edge Cases

- 저장 직후 목록: 태그 필터를 해제하고 전체 목록 첫 쪽으로 돌아가 새 링크가 맨 위에 보인다(clarify Q1).
- 태그 입력: 쉼표로 나누고 각 조각의 앞뒤 공백을 지운다. 빈 조각(`Java,,스프링`, 끝의 쉼표)은 버린다. 대소문자 중복 합치기·길이 검사는 서버 규칙(spec 001)을 따르고 화면은 서버 오류를 그대로 보여 준다.
- 메모가 공백뿐이면 메모 없이 저장된다(서버 규칙).
- 긴 URL·메모: 화면 폭을 넘으면 줄바꿈되거나 잘려 보이되 가로 스크롤이 생기지 않는다.
- URL 링크는 새 탭으로 열고, 열린 페이지가 이 화면을 조작할 수 없게 한다.
- 저장 시각은 사용자의 지역 시간으로 `YYYY-MM-DD HH:mm` 형식이다(clarify Q2).
- 빠르게 체크를 두 번 바꿈: 마지막으로 바꾼 상태가 남는다.
- 다른 쪽에 있는 동안 링크가 늘어도 자동으로 다시 불러오지 않는다(새로 고침·쪽 이동 때 반영).

## Requirements *(mandatory)*

각 요구사항은 `R-UI-NN (출처)` 형식이다. 화면 요소 이름은 Feature Map의 `data-testid`와 같다.

### Functional Requirements

- **R-UI-01** (PRD-02): 화면을 열면 링크 목록(`link-list`)을 최신순으로 보여 준다. 각 항목(`link-item`)은 URL(`link-item-url`, 누르면 새 탭), 메모(`link-item-memo`, 없으면 표시 안 함), 태그(`link-item-tag` 여러 개), 저장 시각(`link-item-created-at`, 사용자 지역 시간 `YYYY-MM-DD HH:mm`)을 보여 준다.
- **R-UI-02** (PRD-02): 쪽 넘김(`link-list-prev`/`link-list-next`)과 쪽 정보(`link-list-page-info`, "현재 / 전체")를 보여 준다. 첫 쪽에서 이전, 마지막 쪽에서 다음은 누를 수 없다. 한 쪽의 개수는 서버 기본값(20)이다.
- **R-UI-03** (PRD-02): 보일 링크가 없으면 빈 목록 안내(`link-list-empty`)를 보여 주고 쪽 정보·쪽 넘김은 숨긴다. 목록을 불러오지 못하면 실패 안내(`list-error`)를 보여 준다.
- **R-UI-04** (PRD-01): 저장 폼(`link-form`)은 URL(`link-url-input`), 메모(`link-memo-input`), 태그(`link-tags-input`, 쉼표로 구분), 저장 버튼(`link-save-button`)을 가진다. 저장 요청 중에는 저장 버튼을 누를 수 없다.
- **R-UI-05** (PRD-01): 저장에 성공하면 성공 표시(`link-form-saved`)를 보여 주고, 폼을 비우고, 태그 필터를 해제하고, 목록을 첫 쪽부터 다시 불러온다. 성공 표시는 다음 입력을 시작하거나 다시 저장할 때까지 남는다.
- **R-UI-06** (PRD-01): 서버가 입력 오류를 돌려주면 틀린 항목마다 오류(`link-form-error`, `data-field`=`url`·`memo`·`tags`)를 서버가 준 메시지로 보여 주고 입력값은 남긴다. 중복이면 중복 안내(`link-form-duplicate`)를 보여 준다. 다음 저장을 시도하면 이전 오류·안내는 지운다.
- **R-UI-07** (PRD-03): 필터 입력(`tag-filter-input`)과 적용(`tag-filter-apply`)으로 태그 하나로 거른다. 적용하면 첫 쪽부터 다시 불러오고, 현재 필터(`tag-filter-active`)와 해제(`tag-filter-clear`)를 보여 준다. 입력이 비었거나 공백뿐이면 적용은 해제와 같다(서버에 빈 태그를 보내지 않는다).
- **R-UI-08** (PRD-03): 서버가 필터 태그를 거부하면(예: 31자) 필터 입력 오류(`tag-filter-error`)를 보여 주고 이전 목록과 필터를 유지한다. 쪽 넘김은 현재 필터 안에서 한다.
- **R-UI-09** (PRD-04): 각 항목의 읽음 체크박스(`link-item-read-toggle`)는 링크의 읽음 상태와 같다. 바꾸면 체크 상태를 먼저 바꾸고 서버에 그 상태를 지정하며, 성공하면 서버가 돌려준 상태로 맞춘다.
- **R-UI-10** (PRD-04): 읽음 지정이 실패하면 체크 상태를 원래대로 되돌리고 그 항목에 실패 안내(`link-item-error`)를 보여 준다.
- **R-UI-11**: 화면은 백엔드 API(spec 001·002 계약)를 바꾸지 않고 그대로 쓴다.
- **R-UI-12**: E2E 테스트는 Feature Map의 testid와 역할로만 요소를 찾고, 서로의 데이터에 기대지 않는다(각자 고유한 URL을 쓴다). 로컬 파일 데이터는 쓰지 않는다.
- **R-UI-13**: 화면 상태 3가지(링크가 있는 첫 화면, 빈 목록, 저장 입력 오류)의 스크린샷을 기준 이미지와 비교해 의도하지 않은 화면 변화를 찾는다(Visual QA). 기준 이미지는 자동 검사 환경과 같은 환경에서 만든다.
- **R-UI-14** (PRD-01, Issue #32 F1): 저장에 성공해 성공 표시(`link-form-saved`)가 보이는 동안 아무것도 입력하지 않고 다시 제출(Enter·저장 버튼)하면 저장 요청을 보내지 않고, 성공 표시를 그대로 두며 입력 오류도 보이지 않는다. 그 밖의 빈 제출(처음 화면, 입력을 시작한 뒤 지운 경우)은 R-UI-06대로 서버 오류를 보여 준다.
- **R-UI-15** (PRD-04, Issue #32 F2): 각 항목의 읽음 체크박스의 접근성 이름은 `<그 항목의 URL> 읽음`(예: "https://example.com/a 읽음")이라 스크린리더로 항목끼리 구분할 수 있다.
- **R-UI-16** (PRD-02, Issue #32 F3): 이전 쪽·다음 쪽으로 넘겨 새 쪽이 보이면, 목록 영역의 맨 위가 화면 맨 위에 오도록 스크롤해 새 쪽의 첫 항목이 화면 안에 다 보이게 한다. 불러오기에 실패하면 스크롤하지 않는다. 저장·필터로 첫 쪽으로 돌아갈 때는 스크롤하지 않는다.
- **R-UI-17** (PRD-02, Issue #34): 이전 쪽·다음 쪽으로 넘겨 새 쪽이 보이면 키보드 포커스를 새 쪽 첫 항목의 링크(`link-item-url`)로 옮겨, 키보드·스크린리더 사용자가 새 쪽 처음부터 이어 갈 수 있게 한다. 불러오기에 실패하면 포커스를 옮기지 않는다.

### Key Entities *(include if feature involves data)*

- **링크(화면 표시)**: spec 001·002의 링크 그대로(식별자, URL, 메모, 태그, 저장 시각, 읽음). 화면은 따로 저장하지 않는다.
- **화면 상태**: 현재 쪽, 현재 필터 태그, 폼 입력값과 오류. 새로 고침하면 첫 쪽·필터 없음으로 돌아간다.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: PRD-01~04의 정상 흐름(저장 → 목록 맨 위 → 필터 → 읽음 → 새로 고침 후 유지)을 사용자가 화면만으로 1분 안에 끝낼 수 있다(E2E 하나가 같은 흐름을 통과).
- **SC-002**: Feature Map의 E2E 테스트가 모두 자동 검사(CI)에서 통과하고, 각 R-UI 요구사항이 최소 하나의 E2E 테스트와 연결된다.
- **SC-003**: 일부러 고장 낸 화면(예: 정렬 뒤집기, 오류 표시 제거, 실패 시 되돌림 제거)을 E2E가 잡는다.
- **SC-004**: 기준 이미지와 다른 화면 변화가 생기면 자동 검사가 실패하고, 차이 이미지를 사람이 볼 수 있다.
- **SC-005**: 로컬과 자동 검사 모두에서 E2E 실행이 로컬 파일 데이터를 바꾸지 않는다.

## Assumptions

- 사용자는 1명, 로그인 없음, 로컬 실행(PRD §6). 화면은 데스크톱 브라우저 폭 기준이다.
- 범위 밖: 로그인, 링크 수정·삭제, 검색, 여러 태그 필터, 안 읽은 것만 보기, 반응형 세부 디자인, 배포, 다국어.
- 구현 단위: 화면(US1~US4 화면 코드)과 E2E·CI·Visual QA를 나눈다. 둘은 Feature Map의 testid를 계약으로 고칠 파일이 겹치지 않으므로 병렬로 진행한다(조언 00:40 4번). 세부 Issue 구성은 tasks에서 정한다.
- 디자인: 기본 HTML 요소와 최소 스타일. 디자인 시스템은 쓰지 않는다.

## 추적 (구현 완료, 2026-10-15)

화면은 PR #26(`frontend/src/`), E2E·Visual QA는 PR #27(`frontend/e2e/`), 라벨 줄바꿈 수정은 PR #29. CI는 PR #25의 `frontend-e2e` job이며 2026-10-15부터 main 필수 검사다.

| R-ID | 화면 PR | E2E (`frontend/e2e/`) |
|---|---|---|
| R-UI-01 | #26 | `E2E_R_UI_01_lists_newest_first_with_fields` |
| R-UI-02 | #26 | `E2E_R_UI_02_pages_through_links` |
| R-UI-03 | #26 | `E2E_R_UI_03_shows_empty_state`, `E2E_R_UI_03_shows_list_error_when_api_fails`, `E2E_R_UI_03_shows_list_error_when_api_returns_400` |
| R-UI-04 | #26 | `E2E_R_UI_04_save_button_disabled_while_saving` |
| R-UI-05 | #26 | `E2E_R_UI_05_saves_link_and_shows_it_first`, `E2E_R_UI_05_save_clears_tag_filter`, `E2E_R_UI_05_save_returns_to_first_page` |
| R-UI-06 | #26 | `E2E_R_UI_06_shows_field_errors`, `E2E_R_UI_06_shows_tags_error`, `E2E_R_UI_06_shows_duplicate_notice` |
| R-UI-07 | #26 | `E2E_R_UI_07_filters_by_tag_ignoring_case`, `E2E_R_UI_07_blank_filter_clears` |
| R-UI-08 | #26 | `E2E_R_UI_08_shows_filter_error_and_keeps_list`, `E2E_R_UI_08_pages_within_filter` |
| R-UI-09 | #26 | `E2E_R_UI_09_marks_read_and_keeps_after_reload`, `E2E_R_UI_09_marks_unread_again` |
| R-UI-10 | #26 | `E2E_R_UI_10_reverts_checkbox_when_marking_fails` |
| R-UI-11 | #26·#27·#29 | 세 PR 모두 `backend/` diff 없음 + 위 기능 E2E가 실제 API로 PASS |
| R-UI-12 | — | `E2E_R_UI_12_backend_uses_in_memory_db` (파일 DB 미사용 부분만. 메모리 DB 설정을 빼면 FAIL 확인, PR #27). testid·역할로 찾기와 고유 URL은 리뷰로 확인 |
| R-UI-14 | #33 | `E2E_R_UI_14_resubmitting_empty_form_after_save_keeps_saved_notice`, `E2E_R_UI_14_empty_submit_on_fresh_form_still_shows_url_error`, `E2E_R_UI_14_typing_after_save_reenables_empty_submit` |
| R-UI-15 | #33 | `E2E_R_UI_15_read_checkbox_name_includes_url` (역할·이름으로 찾음) |
| R-UI-16 | #33 | `E2E_R_UI_16_paging_scrolls_new_first_item_into_view` |
| R-UI-17 | #35 | `E2E_R_UI_17_paging_moves_focus_to_new_first_link` (역할·이름으로 찾음) |
| R-UI-13 | #26·#29 | `VISUAL_R_UI_13_first_screen`, `VISUAL_R_UI_13_empty_list`, `VISUAL_R_UI_13_form_errors`. 회귀 방지(Issue #28, spec 문장 없음): `E2E_R_UI_13_tag_filter_label_stays_on_one_line` |

| 성공 기준 | 근거 |
|---|---|
| SC-001 | `E2E_SC_001_full_flow` |
| SC-002 | 위 표. PR #33 최종 CI(아래 PR 본문의 run): `frontend-e2e` 31개 중 31 PASS. 그 전 PR #29 run 38666997040: 26개 중 26 PASS |
| SC-003 | PR #26 고장 내기 3건(정렬 뒤집기 10 FAIL, `list-error` 제거 1 FAIL, 읽음 되돌림 제거 1 FAIL), PR #27 새 단언 4개 각각 고장으로 FAIL 확인 |
| SC-004 | PR #29 첫 CI run 38666210871: 화면 변화로 visual 3개 FAIL, 차이 이미지(`*-diff.png`)가 `playwright-report` 아티팩트에 남음 |
| SC-005 | `E2E_R_UI_12_backend_uses_in_memory_db`(로컬·CI), 고장 시험은 implementer worktree 안에서만 실행(사용자 로컬 DB 무접촉) |
