# Feature Specification: 링크 목록 정렬 옵션

**Feature Branch**: `feat/43-sort-links` (Issue #43)

**Created**: 2026-10-21

**Status**: Implemented (Issue #43, PR 병합 대기)

**Input**: User description: "linkshelf 목록에 정렬 옵션 추가해 줘" (출처: [PRD-05](../../docs/prd.md), 2026-10-21 grilling에서 사용자가 추천안 선택)

**Base SHA**: `0006c38` (main, PR #42 병합 후)

**관계**: spec 002 Clarifications는 "정렬 기준 바꾸기"를 범위 밖으로 정했다. 이 spec이 그 범위를 새로 연다. browse-links 계약(`specs/002-browse-links/contracts/browse-links.md`)을 바꾸는 일은 plan 단계에서 승인받는다(constitution I).

## Clarifications

### Session 2026-10-21

- Q: 정렬 기준·API 모양·잘못된 값·조합·화면을 어떻게 할까요? → A: 저장 시각 최신순(기본)·오래된순 두 가지, 정렬 값 하나로 고르고 생략하면 최신순, 잘못된 값은 입력 오류, 태그 필터·쪽 나누기와 함께 동작, 목록 위 선택 상자 하나·새로 고치면 기본값 (사용자 선택: grilling 추천안, 이유 미기재)
- Q: 오래된순으로 보는 중에 링크를 저장하면? → A: 정렬을 최신순으로 되돌려 새 링크를 맨 위에 보인다. 필터 해제(R-UI-05)와 같은 원칙 (사용자 선택: advisor 추천 A, 이유 미기재)
- Q: 정렬 값을 빈 값으로 보내면? → A: 입력 오류로 거부한다(빈 태그를 거부하는 규칙과 같음) (사용자 선택: advisor 추천, 이유 미기재)

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 오래된순으로 목록 보기 (Priority: P1)

사용자가 링크 목록을 오래전에 저장한 것부터 본다. 아무것도 고르지 않으면 지금처럼 최신순이다. 오래된순에서도 쪽 나누기는 그대로 동작한다.

**Why this priority**: 정렬 방향을 고를 수 있어야 이 기능이 의미가 있다. 화면(US2)도 이 동작에 기댄다.

**Independent Test**: 저장 시각이 다른 링크 3개를 저장한 뒤 오래된순으로 목록을 요청해 저장한 순서대로 돌아오는지, 정렬을 지정하지 않은 요청은 최신순 그대로인지 확인한다.

**Acceptance Scenarios**:

1. **Given** 링크 A, B, C를 이 순서로 저장함, **When** 정렬을 지정하지 않고 목록 요청, **Then** C, B, A 순서로 돌아오고 응답이 이 기능 전과 같다.
2. **Given** 위와 같음, **When** 최신순으로 목록 요청, **Then** 1과 같은 결과다.
3. **Given** 위와 같음, **When** 오래된순으로 목록 요청, **Then** A, B, C 순서로 돌아온다.
4. **Given** 한 쪽 크기보다 많은 링크가 있음, **When** 오래된순으로 모든 쪽을 차례로 요청, **Then** 모든 링크가 빠짐없이, 겹침 없이, 오래된순으로 나뉘어 돌아온다.
5. **Given** 링크 A(태그 `java`), B(태그 없음), C(태그 `java`)를 이 순서로 저장함, **When** 태그 `java`로 거르고 오래된순으로 요청, **Then** A, C만 이 순서로 돌아온다.
6. **Given** 아무 상태, **When** 허용하지 않는 정렬 값으로 요청, **Then** 입력 오류로 거부되고 오류는 틀린 항목이 정렬임을 알려 준다.

---

### User Story 2 - 화면에서 정렬 고르기 (Priority: P2)

사용자가 목록 위의 정렬 선택 상자에서 "최신순" 또는 "오래된순"을 고른다. 고르면 목록이 첫 쪽부터 그 순서로 다시 보인다.

**Why this priority**: 사용자는 화면으로 기능을 쓴다. API(US1)가 먼저 있어야 한다.

**Independent Test**: 링크 여러 개가 있는 화면에서 정렬을 오래된순으로 바꿔, 첫 항목이 가장 먼저 저장한 링크이고 쪽 정보가 첫 쪽인지 확인한다.

**Acceptance Scenarios**:

1. **Given** 화면을 처음 열었음, **When** 정렬 선택 상자를 확인, **Then** "최신순"이 골라져 있고 목록은 최신순이다.
2. **Given** 링크가 여러 쪽에 걸쳐 있고 사용자가 2쪽에 있음, **When** 정렬을 오래된순으로 바꿈, **Then** 목록이 첫 쪽부터 오래된순으로 보인다.
3. **Given** 오래된순으로 보고 있음, **When** 다음 쪽으로 넘김, **Then** 다음 쪽도 오래된순이다.
4. **Given** 태그 필터를 적용한 상태, **When** 정렬을 바꿈, **Then** 필터는 그대로이고 거른 결과가 첫 쪽부터 새 순서로 보인다.
5. **Given** 오래된순으로 보고 있음, **When** 태그 필터를 적용하거나 해제함, **Then** 정렬은 오래된순 그대로다.
6. **Given** 오래된순으로 보고 있음, **When** 화면을 새로 고침, **Then** 정렬은 최신순으로 돌아간다.
7. **Given** 오래된순으로 보고 있음, **When** 링크를 새로 저장함, **Then** 정렬 선택 상자가 최신순으로 바뀌고, 목록이 첫 쪽부터 최신순으로 보여 새 링크가 맨 위에 있다.

---

### Edge Cases

- 저장 시각이 같은 링크 여러 개: 최신순이면 식별자가 큰 것부터, 오래된순이면 식별자가 작은 것부터 보인다. 순서가 요청마다 바뀌지 않는다.
- 정렬 값의 대소문자·앞뒤 공백: 정해진 값과 정확히 같을 때만 받는다. 다른 값은 고치지 않고 거부한다(R-LINK-11과 같은 원칙).
- 정렬 값과 쪽 번호·쪽 크기가 함께 틀림: 틀린 항목을 모두 알려 준다. 숫자가 아닌 쪽 번호·크기가 있으면 R-LINK-11 예외(그 항목만 알림)를 따른다.
- 정렬 값이 비어 있음(값 없이 이름만 보냄): 생략과 다르게 입력 오류로 거부한다(clarify Q3).
- 정렬을 바꾸는 중 목록 불러오기가 실패: 실패 안내(`list-error`)를 보여 준다(R-UI-03). 선택 상자는 사용자가 고른 값을 유지한다. 같은 값을 다시 골라도 선택 상자는 바뀐 것이 없어 요청하지 않으므로, 다시 불러오기는 실패 안내 안의 버튼으로 한다(R-UI-26, Issue #45).
- 빈 목록에서 정렬을 바꿈: 빈 목록 안내가 그대로 보인다.

## Requirements *(mandatory)*

백엔드는 spec 001·002의 R-LINK-01~18에 이어 `R-LINK-NN (출처)`, 화면은 spec 003의 R-UI-01~17에 이어 `R-UI-NN (출처)`로 적는다. 화면 요소 이름은 `.claude/skills/verify-linkshelf/features/README.md` 표의 `data-testid`와 같다. 새 testid는 같은 PR에서 표에 먼저 넣는다.

### Functional Requirements

- **R-LINK-19** (PRD-05): 사용자는 목록의 정렬을 최신순 또는 오래된순으로 고를 수 있다. 최신순은 R-LINK-09 그대로(저장 시각이 늦은 것부터, 같으면 식별자가 큰 것부터)이고, 오래된순은 저장 시각이 이른 것부터, 같으면 식별자가 작은 것부터다. 정렬을 생략하면 최신순이고 응답은 이 기능 전과 같다.
- **R-LINK-20** (PRD-05): 정렬은 태그 거르기(R-LINK-12)와 쪽 나누기(R-LINK-10)와 함께 동작한다. 거른 결과를 고른 순서로 정렬한 뒤 나눈다. 전체 개수와 전체 쪽 수는 정렬과 관계없다.
- **R-LINK-21** (PRD-05): 허용하지 않는 정렬 값(빈 값 포함)은 입력 오류로 거부하고, 기존 오류 형식으로 틀린 항목이 정렬임을 알려 준다. 쪽 번호·크기 오류와 함께 있으면 R-LINK-11대로 틀린 항목을 모두 알려 준다. 값을 고쳐서 처리하지 않는다.
- **R-UI-18** (PRD-05): 목록 위에 정렬 선택 상자(`link-sort-select`)를 두고 "최신순"(기본)과 "오래된순"을 고를 수 있다. 화면을 열거나 새로 고치면 최신순이다. 정렬 상태는 주소에 남기지 않는다.
- **R-UI-19** (PRD-05): 정렬을 바꾸면 현재 태그 필터를 유지한 채 첫 쪽부터 새 순서로 다시 불러온다. 쪽 넘김과 태그 필터 적용·해제는 현재 정렬을 유지한다. 정렬로 첫 쪽에 돌아갈 때는 R-UI-16·17의 스크롤·포커스 이동을 하지 않는다.
- **R-UI-20** (PRD-05): 링크 저장에 성공하면(R-UI-05) 정렬을 최신순으로 되돌린다. 선택 상자 표시와 다시 불러오는 순서가 함께 최신순이고, 새 링크가 첫 쪽 맨 위에 보인다. 이후 쪽 넘김도 최신순이다.
- **R-UI-26** (PRD-05, Issue #45): 목록 불러오기 실패 안내(`list-error`) 안에 다시 불러오기 버튼(`list-error-retry`)을 둔다. 누르면 실패한 요청의 쪽을 사용자가 마지막으로 고른 정렬·안 읽음 값과 화면에 적용된 태그 필터로 다시 불러온다(R-UI-24). 정렬 선택 상자와 안 읽음 체크박스는 되돌리지 않는다. 다시 불러오는 중에는 버튼을 누를 수 없다.

### Key Entities *(include if feature involves data)*

- **정렬 방향**: 최신순 또는 오래된순. 저장하지 않는 요청 조건이다. 링크 데이터와 DB 스키마는 바뀌지 않는다.
- **화면 상태**: spec 003의 화면 상태(현재 쪽, 필터 태그, 폼)에 현재 정렬을 더한다. 새로 고치면 최신순으로 돌아간다.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 사용자가 화면에서 한 번 골라 가장 먼저 저장한 링크를 첫 항목으로 볼 수 있다(E2E 하나가 같은 흐름을 통과).
- **SC-002**: 정렬을 지정하지 않은 기존 요청과 기존 자동 테스트(백엔드·E2E)가 모두 이 기능 전과 같은 결과로 통과한다.
- **SC-003**: 정렬 방향을 일부러 뒤집으면 해당 R-ID의 테스트가 실패한다(고장 내기).
- **SC-004**: 각 R-ID가 최소 하나의 자동 테스트와 연결되고, verify-linkshelf 정렬 여정 한 바퀴가 실제 앱에서 PASS한다.

## Assumptions

- 사용자는 1명, 로그인 없음, 로컬 실행(PRD §6). 링크 수는 수천 개 이하라 정렬 성능 목표를 따로 두지 않는다.
- 범위 밖: URL·메모·읽음 여부 등 다른 기준으로 정렬, 여러 기준 조합, 정렬 상태를 주소나 저장소에 남기기, 사용자별 기본 정렬.
- 구체 파라미터 이름과 값(grilling 추천 `order=newest|oldest`), 오류 메시지 문구는 plan에서 정하고 계약 변경으로 승인받는다.
- 정렬 선택 상자가 들어가면 Visual QA 기준 이미지(R-UI-13)가 바뀐다. 기준 이미지 갱신은 같은 PR에서 자동 검사 환경으로 만든다.
- 결정 기록: 정렬 결정은 `adr/tech-decision-checklist.md`("시간대·정렬·페이지네이션 → Spec")대로 이 spec에 둔다. ADR은 만들지 않는다.

## 추적 (구현 완료, 2026-10-21)

| R-ID | Issue | 테스트 |
|---|---|---|
| R-LINK-19 | #43 | `SortLinksRepositoryTest.R_LINK_19_oldest_orders_by_created_at_asc_then_id_asc`, `SortLinksRepositoryTest.R_LINK_19_newest_matches_existing_order`, `SortLinksControllerTest.R_LINK_19_passes_order_to_service`, `SortLinksIntegrationTest.R_LINK_19_omitted_order_response_equals_newest_response`, `SortLinksIntegrationTest.R_LINK_19_oldest_lists_saved_order_end_to_end` |
| R-LINK-20 | #43 | `SortLinksRepositoryTest.R_LINK_20_oldest_pages_without_overlap_or_gap`, `SortLinksRepositoryTest.R_LINK_20_oldest_with_tag_filters_then_sorts_and_counts_filtered_only`, `SortLinksIntegrationTest.R_LINK_20_oldest_with_huge_page_is_empty_not_server_error` |
| R-LINK-21 | #43 | `SortLinksControllerTest.R_LINK_21_rejects_unknown_order_400`, `SortLinksControllerTest.R_LINK_21_rejects_empty_order_400`, `SortLinksControllerTest.R_LINK_21_reports_order_with_page_and_size_together`, `SortLinksControllerTest.R_LINK_21_error_message_matches_contract` |
| R-UI-18 | #43 | `E2E_R_UI_18_sort_select_defaults_to_newest_and_resets_on_reload` |
| R-UI-19 | #43 | `E2E_R_UI_19_changing_order_reloads_first_page_keeping_filter`, `E2E_R_UI_19_paging_and_filter_keep_order`, `E2E_R_UI_19_order_change_does_not_move_focus_or_scroll`, `E2E_R_UI_19_order_change_right_after_filter_keeps_filter`, `E2E_R_UI_19_order_change_right_after_save_keeps_filter_cleared`, `E2E_R_UI_19_list_error_keeps_chosen_order`, `E2E_R_UI_19_empty_list_stays_empty_after_order_change`, `E2E_R_UI_19_order_change_after_rejected_filter_keeps_shown_filter` |
| R-UI-20 | #43 | `E2E_R_UI_20_saving_resets_order_to_newest` |
| R-UI-26 | #45 | `E2E_R_UI_26_retry_after_order_change_failure_shows_chosen_order` |
| R-UI-13(기준 이미지) | #43 | `VISUAL_R_UI_13_*` 3장 갱신(CI run 39582485480) |

실제 앱 증거: verify-linkshelf `sort-links` 여정(`.claude/skills/verify-linkshelf/drive/sort-links.verify.ts`).
