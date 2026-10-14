# Feature Specification: 링크 목록 조회·태그 필터·읽음 표시

**Feature Branch**: 스토리별 Issue·PR (US1 → US2 → US3 순서). spec 브랜치 `chore/spec-002-browse-links`

**Created**: 2026-10-14

**Status**: Implemented (PR #16·#17·#18)

**Input**: User description: "저장된 링크를 최신순으로 보고(PRD-02), 태그 하나로 거르고(PRD-03), 링크별로 읽음/안 읽음을 표시한다(PRD-04)." (출처: [PRD-02~04](../../docs/prd.md))

**Base SHA**: `c2c2bc3` (main, PR #11 병합 후)

## Clarifications

### Session 2026-10-14

- Q: 목록이 길 때 나눠 받는 방식과 응답 모양을 어떻게 할까요? → A: 쪽 번호 방식, 기본 20개, 최대 100개, 응답은 목록·쪽 번호·쪽 크기·전체 개수·전체 쪽 수·다음 쪽 여부를 담은 객체 (사용자 답 "a", 이유 미기재)
- Q: 쪽 번호나 쪽 크기가 잘못 들어오면 어떻게 처리할까요? → A: 음수, 숫자가 아닌 값, 0 이하 또는 100 초과 쪽 크기는 모두 입력 오류로 거부하고 틀린 항목을 알려 준다 (사용자 답 "a", 이유 미기재)
- Q: 태그 하나로 거를 때 어떤 경우를 같은 태그로 볼까요? → A: 앞뒤 공백을 빼고 대소문자를 무시했을 때 태그 전체가 같으면 일치. 부분 일치는 아님 (사용자 위임: "다 추천안으로 해. 어차피 테스트니까")
- Q: 읽음 표시는 읽음 여부만 둘까요, 읽은 시각도 남길까요? 뒤집기와 상태 지정 중 어느 방식일까요? → A: 읽음 여부(참/거짓)만, 읽은 시각 없음. 원하는 상태를 지정하는 방식(같은 요청을 반복해도 결과가 같음) (사용자 위임: "다 추천안으로 해. 어차피 테스트니까")
- Q: 안 읽은 것만 보기와 여러 태그로 거르기는 이번 범위 밖인가요? → A: 범위 밖으로 확정 (사용자 위임: "다 추천안으로 해. 어차피 테스트니까")

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 저장한 링크 목록 보기 (Priority: P1)

사용자가 지금까지 저장한 링크를 가장 최근에 저장한 것부터 본다. 각 링크는 저장할 때 돌려받은 내용(식별자, URL, 메모, 태그, 저장 시각)을 그대로 보여 준다. 링크가 많으면 한 번에 일부만 받고 다음 부분을 이어서 받는다.

**Why this priority**: 저장만 되고 다시 볼 수 없으면 제품의 가치가 없다. 태그 필터(US2)와 읽음 표시(US3)도 목록이 있어야 확인할 수 있다.

**Independent Test**: 저장 시각이 다른 링크 3개를 저장한 뒤 목록을 요청해, 3개가 최신순으로 저장한 내용 그대로 돌아오는지 확인한다.

**Acceptance Scenarios**:

1. **Given** 저장된 링크가 없음, **When** 목록 요청, **Then** 성공하고 빈 목록이 돌아온다.
2. **Given** 링크 A, B, C를 이 순서로 저장함, **When** 목록 요청, **Then** C, B, A 순서로 돌아오고 각 항목의 URL·메모·태그·저장 시각이 저장 때와 같다.
3. **Given** 한글 메모와 한글 태그가 있는 링크를 저장함, **When** 목록 요청, **Then** 메모와 태그가 깨지지 않고 저장 때와 같다.
4. **Given** 한 번에 받는 개수보다 많은 링크가 있음, **When** 첫 부분과 다음 부분을 차례로 요청, **Then** 모든 링크가 빠짐없이, 겹침 없이, 최신순으로 나뉘어 돌아오고 전체 개수를 알 수 있다.

---

### User Story 2 - 태그 하나로 거르기 (Priority: P2)

사용자가 태그 하나를 골라 그 태그가 붙은 링크만 본다. 저장할 때 대소문자를 다르게 썼어도(링크 A에는 `Java`, 링크 B에는 `java`) 같은 태그로 본다.

**Why this priority**: 링크가 쌓이면 주제별로 다시 찾는 일이 필요하다. 목록(US1)이 먼저 있어야 한다.

**Independent Test**: 태그가 다른 링크 여러 개를 저장한 뒤 태그 하나로 목록을 요청해, 그 태그가 붙은 링크만 최신순으로 돌아오는지 확인한다.

**Acceptance Scenarios**:

1. **Given** 링크 A(태그 `Java`), B(태그 `java`, `스프링`), C(태그 없음)를 저장함, **When** 태그 `java`로 거르기, **Then** B, A만 최신순으로 돌아온다.
2. **Given** 위와 같음, **When** 태그 ` JAVA `(앞뒤 공백, 대문자)로 거르기, **Then** 결과가 1과 같다.
3. **Given** 위와 같음, **When** 태그 `스프링`으로 거르기, **Then** B만 돌아온다.
4. **Given** 위와 같음, **When** 태그 `Jav`(일부만 일치)로 거르기, **Then** 빈 목록이 돌아온다(태그 전체가 같아야 일치).
5. **Given** 위와 같음, **When** 어느 링크에도 없는 태그로 거르기, **Then** 성공하고 빈 목록이 돌아온다.
6. **Given** 아무 상태, **When** 공백만 있는 태그로 거르기, **Then** 입력 오류로 거부된다.

---

### User Story 3 - 읽음/안 읽음 표시 (Priority: P3)

사용자가 링크를 읽었으면 읽음으로, 다시 읽을 생각이면 안 읽음으로 표시한다. 목록과 저장 결과에서 각 링크가 읽음인지 볼 수 있다.

**Why this priority**: 읽을거리를 관리하는 데 유용하지만, 목록과 필터가 없으면 확인할 방법이 없다. 데이터에 새 항목이 생기는 변경이라 마지막에 한다.

**Independent Test**: 링크 하나를 저장해 안 읽음인지 확인하고, 읽음으로 표시한 뒤 목록에서 읽음으로 보이는지 확인한다.

**Acceptance Scenarios**:

1. **Given** 링크를 새로 저장함, **When** 저장 결과와 목록을 확인, **Then** 그 링크는 안 읽음이다.
2. **Given** 안 읽음인 링크 A, **When** A를 읽음으로 표시, **Then** 성공하고 결과와 목록에서 A가 읽음이다.
3. **Given** 읽음인 링크 A, **When** A를 다시 읽음으로 표시, **Then** 성공하고 A는 여전히 읽음이다(같은 요청을 반복해도 결과가 같다).
4. **Given** 읽음인 링크 A, **When** A를 안 읽음으로 표시, **Then** 성공하고 A가 안 읽음이다.
5. **Given** 존재하지 않는 식별자, **When** 읽음으로 표시, **Then** 찾을 수 없다는 오류로 거부되고 아무것도 바뀌지 않는다.
6. **Given** 이 기능 전에 저장된 링크가 있음, **When** 기능을 적용한 뒤 목록을 확인, **Then** 기존 링크는 모두 안 읽음이고 다른 내용은 바뀌지 않는다.

---

### Edge Cases

- 저장 시각이 같은 링크 여러 개: 나중에 저장된(식별자가 큰) 링크를 먼저 보여 준다. 순서가 요청마다 바뀌지 않는다.
- 쪽 크기를 101 이상이나 0 이하로, 또는 쪽 번호를 음수·숫자가 아닌 값으로 요청: 입력 오류로 거부한다(R-LINK-11). 경계값: 쪽 크기 1·100은 허용, 0·101은 거부.
- 마지막 부분 다음을 요청: 성공하고 빈 목록이 돌아온다.
- 태그 거르기와 나눠 받기를 함께 쓰는 경우: 거른 결과를 기준으로 나누고 전체 개수도 거른 결과의 개수다.
- 태그 거르기의 대소문자: 저장 규칙(R-LINK-05)과 같게 앞뒤 공백을 제거하고 대소문자를 무시해 태그 전체가 같은지 본다. 한글은 대소문자 구분이 없으므로 그대로 비교한다.
- 읽음 표시 요청에 읽음 여부 값이 빠졌거나 참/거짓이 아닌 경우: 입력 오류로 거부한다.
- 읽음 표시는 링크의 다른 내용(URL·메모·태그·저장 시각)을 바꾸지 않는다.

## Requirements *(mandatory)*

각 요구사항은 `R-LINK-NN (출처 PRD 번호)` 형식으로 적는다. spec 001의 R-LINK-01~08에 이어 번호를 붙인다.

### Functional Requirements

- **R-LINK-09** (PRD-02): 사용자는 저장된 링크 목록을 볼 수 있다. 목록은 저장 시각이 늦은 것부터, 저장 시각이 같으면 식별자가 큰 것부터 정렬한다. 각 항목은 저장 결과(R-LINK-01)와 같은 내용을 담는다.
- **R-LINK-10** (PRD-02): 목록은 쪽 번호로 나눠 받는다. 쪽 번호는 0부터 시작하고, 쪽 크기를 정하지 않으면 20개, 최대 100개다. 응답은 해당 쪽의 링크 목록과 함께 쪽 번호, 쪽 크기, 전체 개수, 전체 쪽 수, 다음 쪽이 있는지를 담는다. 쪽 번호와 크기를 생략하면 첫 쪽 20개를 돌려준다.
- **R-LINK-11** (PRD-02): 쪽 번호가 음수이거나 숫자가 아니면, 또는 쪽 크기가 숫자가 아니거나 1~100 범위 밖이면 입력 오류로 거부하고 틀린 항목을 모두 알려 준다. 값을 고쳐서 처리하지 않는다. 예외: 숫자가 아닌 값이 있으면 그 항목만 알려 준다(숫자로 바꿀 수 없어 다른 검사를 할 수 없음. plan 승인 B5).
- **R-LINK-12** (PRD-03): 사용자는 태그 하나로 목록을 거를 수 있다. 거른 결과는 그 태그가 붙은 링크만 담고, 정렬·나눠 받기는 R-LINK-09·10을 따른다.
- **R-LINK-13** (PRD-03): 태그 일치는 요청한 태그와 링크의 태그 각각의 앞뒤 공백을 제거하고 대소문자를 무시했을 때 전체가 같은 경우다. 일부만 같으면 일치하지 않는다. 일치하는 링크가 없으면 빈 목록으로 성공한다.
- **R-LINK-14** (PRD-03): 거를 태그가 공백만 있거나 30자(코드 포인트, R-LINK-08)를 넘으면 입력 오류로 거부한다.
- **R-LINK-15** (PRD-04): 사용자는 링크 하나를 읽음 또는 안 읽음으로 지정할 수 있다. 같은 지정을 반복해도 결과가 같다. 성공하면 바뀐 링크 내용을 돌려준다. 다른 내용은 바뀌지 않는다.
- **R-LINK-16** (PRD-04): 새로 저장한 링크와 이 기능 전에 저장된 링크는 안 읽음이다. 저장 결과와 목록의 각 항목은 읽음 여부를 담는다.
- **R-LINK-17** (PRD-04): 존재하지 않는 링크를 지정하면 찾을 수 없다는 오류로 거부한다. 읽음 여부 값이 없거나 참/거짓이 아니면 입력 오류로 거부한다. 거부된 요청은 아무것도 바꾸지 않는다.
- **R-LINK-18** (PRD-02~04, spec 001): 기존 저장 기능(R-LINK-01~08)의 동작은 바뀌지 않는다. 저장 결과에 읽음 여부가 더해지는 것만 예외다.

### Key Entities *(include if feature involves data)*

- **링크(Link)**: spec 001의 링크에 읽음 여부(참/거짓)가 더해진다. 기본값은 안 읽음(거짓)이다. 읽은 시각은 남기지 않는다.
- **링크 목록의 한 부분(Page)**: 정렬된 링크 일부와, 전체 개수·다음 부분 존재 여부 같은 나눠 받기 정보.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 링크 수가 기본 개수보다 많을 때 모든 부분을 차례로 받으면 저장된 링크 전부가 빠짐없이, 겹침 없이, 최신순으로 돌아온다(자동 테스트로 검증).
- **SC-002**: 대소문자·앞뒤 공백만 다른 태그로 거른 결과가 서로 같고, 일부만 같은 태그는 결과에 나오지 않는다(자동 테스트로 검증).
- **SC-003**: 읽음 지정을 두 번 반복한 결과가 한 번 한 결과와 같고, 지정 전후에 URL·메모·태그·저장 시각이 같다(자동 테스트로 검증).
- **SC-004**: 이 기능 전에 저장된 링크가 있는 로컬 데이터에서 앱을 다시 띄우면 정상으로 뜨고, 기존 링크가 모두 안 읽음으로 보인다(실제 기동으로 검증. 자동 테스트는 매번 빈 데이터에서 시작해 이 경우를 잡지 못한다).
- **SC-005**: 1인 사용 규모(수천 건)에서 목록 한 부분을 받는 데 사용자가 기다린다고 느끼지 않는다(1초 이내).
- **SC-006**: R-LINK-09~18이 각각 최소 하나의 테스트 이름과 연결되고, 기존 R-LINK-01~08 테스트가 모두 통과한다.

## Assumptions

- 사용자는 1명이며 로그인·권한 확인은 없다. 외부로 요청을 보내지 않는다(PRD §6).
- 화면 없이 백엔드 API만 제공한다(프론트엔드는 MVP-3).
- 범위 밖(Clarifications에서 확정): 안 읽은 것만 보기 필터, 여러 태그로 거르기, 검색, 링크 수정·삭제, 정렬 기준 바꾸기.
- 구현 순서와 단위: US1 → US2 → US3, 스토리마다 Issue·PR 하나. 세 스토리가 같은 코드(목록 요청 처리, 응답 모양)를 고치므로 병렬로 하지 않는다(advisor 판단).
- 응답 형식·상태 코드·읽음 여부의 DB 표현 같은 구체 계약은 plan 단계에서 정하고 승인받는다. 기존 로컬 데이터에 읽음 여부를 더하는 방식도 plan에서 정한다.

## 추적 (구현 완료, 2026-10-15)

`main` `4c016ee`에서 `cd backend && ./gradlew test` → 120 tests, 0 failed (SC-006).

| R-ID | PR | 테스트 |
|---|---|---|
| R-LINK-09 | #16 | `ListLinksIntegrationTest.R_LINK_09_empty_store_returns_empty_items`, `ListLinksIntegrationTest.R_LINK_09_lists_saved_links_newest_first_with_korean_intact`, `ListLinksRepositoryTest.R_LINK_09_orders_by_created_at_desc_then_id_desc` |
| R-LINK-10 | #16 | `ListLinksControllerTest.R_LINK_10_defaults_page_0_size_20`, `ListLinksControllerTest.R_LINK_10_returns_page_object_shape`, `ListLinksIntegrationTest.R_LINK_10_all_pages_together_return_every_link`, `ListLinksIntegrationTest.R_LINK_10_huge_page_is_empty_not_server_error`, `ListLinksIntegrationTest.R_LINK_10_page_after_last_is_empty`, `ListLinksRepositoryTest.R_LINK_10_counts_total`, `ListLinksRepositoryTest.R_LINK_10_pages_without_overlap_or_gap` |
| R-LINK-11 | #16 | `ListLinksControllerTest.R_LINK_11_error_messages_match_contract`, `ListLinksControllerTest.R_LINK_11_negative_page_400`, `ListLinksControllerTest.R_LINK_11_non_numeric_page_reports_only_that_field`, `ListLinksControllerTest.R_LINK_11_non_numeric_size_400`, `ListLinksControllerTest.R_LINK_11_reports_page_and_size_together`, `ListLinksControllerTest.R_LINK_11_size_bounds_accepted`, `ListLinksControllerTest.R_LINK_11_size_out_of_bounds_400` |
| R-LINK-12 | #17 | `TagFilterControllerTest.R_LINK_12_passes_tag_to_service`, `TagFilterIntegrationTest.R_LINK_12_filtered_pages_keep_newest_first`, `TagFilterIntegrationTest.R_LINK_12_huge_page_with_tag_is_empty`, `TagFilterIntegrationTest.R_LINK_12_no_tag_lists_everything`, `TagFilterRepositoryTest.R_LINK_12_link_matching_twice_appears_once`, `TagFilterRepositoryTest.R_LINK_12_link_with_many_tags_appears_once_and_total_counts_filtered_only`, `TagFilterRepositoryTest.R_LINK_12_small_filtered_pages_without_overlap_or_gap` |
| R-LINK-13 | #17 | `TagFilterIntegrationTest.R_LINK_13_padded_uppercase_tag_filters_end_to_end`, `TagFilterIntegrationTest.R_LINK_13_unknown_tag_returns_empty_items`, `TagFilterRepositoryTest.R_LINK_13_korean_tag`, `TagFilterRepositoryTest.R_LINK_13_matches_case_insensitively`, `TagFilterRepositoryTest.R_LINK_13_non_ascii_case_insensitive`, `TagFilterRepositoryTest.R_LINK_13_requires_whole_tag` |
| R-LINK-14 | #17 | `TagFilterControllerTest.R_LINK_14_blank_tag_400`, `TagFilterControllerTest.R_LINK_14_padded_30_code_points_ok`, `TagFilterControllerTest.R_LINK_14_reports_all_three_parameters_together`, `TagFilterControllerTest.R_LINK_14_same_blank_rule_as_save`, `TagFilterControllerTest.R_LINK_14_tag_of_30_code_points_ok`, `TagFilterControllerTest.R_LINK_14_tag_over_30_code_points_400` |
| R-LINK-15 | #18 | `MarkReadControllerTest.R_LINK_15_returns_changed_link`, `MarkReadIntegrationTest.R_LINK_15_can_mark_unread_again`, `MarkReadIntegrationTest.R_LINK_15_marking_twice_gives_same_result_and_keeps_other_fields`, `MarkReadRepositoryTest.R_LINK_15_read_state_persists` |
| R-LINK-16 | #18 | `MarkReadIntegrationTest.R_LINK_16_list_items_have_read`, `MarkReadIntegrationTest.R_LINK_16_save_response_has_read_false`, `MarkReadRepositoryTest.R_LINK_16_new_link_is_unread` |
| R-LINK-17 | #18 | `MarkReadControllerTest.R_LINK_17_bad_body_before_unknown_id`, `MarkReadControllerTest.R_LINK_17_malformed_json_400_without_errors`, `MarkReadControllerTest.R_LINK_17_missing_or_null_read_400`, `MarkReadControllerTest.R_LINK_17_non_boolean_read_400`, `MarkReadControllerTest.R_LINK_17_non_json_content_type_415`, `MarkReadControllerTest.R_LINK_17_non_numeric_id_400`, `MarkReadControllerTest.R_LINK_17_unknown_id_404_shape`, `MarkReadIntegrationTest.R_LINK_17_unknown_id_changes_nothing` |
| R-LINK-18 | #18 | 기존 R-LINK-01~08 테스트 전부(단언 수정 없음, 생성자 인자 `read`만 추가) |

| 성공 기준 | 근거 |
|---|---|
| SC-001 | `ListLinksIntegrationTest.R_LINK_10_all_pages_together_return_every_link` |
| SC-002 | `TagFilterIntegrationTest.R_LINK_13_padded_uppercase_tag_filters_end_to_end` |
| SC-003 | `MarkReadIntegrationTest.R_LINK_15_marking_twice_gives_same_result_and_keeps_other_fields` |
| SC-004 | 실제 기동(PR #18 검증 표, `docs/ai/handoff.md`): DB 직접 조회·API PASS, 기동 로그의 ALTER 문은 확인 불가(층 2·3으로 대체) |
| SC-005 | NOT_RUN(별도 성능 측정 없음. 쪽 크기 상한 100, 태그 배치 로딩) |
| SC-006 | 위 표 |
